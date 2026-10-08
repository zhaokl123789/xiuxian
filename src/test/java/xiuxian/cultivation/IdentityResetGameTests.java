package xiuxian.cultivation;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_startup")
@PrefixGameTestTemplate(false)
public final class IdentityResetGameTests {
    private IdentityResetGameTests() {}

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void highRealmDeathRequiresNewIdentity(GameTestHelper helper) {
        for (CultivationRealm realm : new CultivationRealm[]{CultivationRealm.PURPLE_MANSION,
                CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI}) {
            FakePlayer original = preparedPlayer(helper, "death_" + realm.id(), realm);
            try {
                FakePlayer respawned = replacement(helper, original);
                respawned.getAttributes().assignValues(original.getAttributes());
                respawned.setHealth(original.getMaxHealth());
                original.setHealth(0.0F);
                respawned.restoreFrom(original, false);

                helper.assertTrue(!cultivation(respawned).isInitialized(),
                        realm.id() + " death did not reset the cloned identity");
                helper.assertTrue(!original.getPersistentData().contains("xiuxian_cultivation_state")
                                && !respawned.getPersistentData().contains("xiuxian_cultivation_state")
                                && !TaixuDimension.hasValidTripSnapshot(respawned),
                        realm.id() + " death retained a snapshot of the old identity");
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(respawned, false));
                for (int tick = 0; tick < 3; tick++) {
                    MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, respawned));
                    helper.assertTrue(!TaixuDimension.recoverTripData(respawned).isInitialized(),
                            realm.id() + " old identity was resurrected by respawn recovery");
                    assertHealth(helper, respawned, 20.0F, 20.0F, realm.id() + " after reset");
                }
                helper.assertTrue(CultivationEvents.selectIdentity(respawned,
                                FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id()),
                        realm.id() + " death blocked the new identity selection");
                assertHealth(helper, respawned, 20.0F, 20.0F, realm.id() + " after new selection");
            } finally {
                TaixuDimension.discardCultivationSnapshots(original);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void lowerRealmDeathRetainsIdentityAndHealth(GameTestHelper helper) {
        for (CultivationRealm realm : new CultivationRealm[]{CultivationRealm.FETAL_BREATH,
                CultivationRealm.QI_REFINING, CultivationRealm.FOUNDATION_ESTABLISHMENT}) {
            FakePlayer original = preparedPlayer(helper, "retain_" + realm.id(), realm);
            try {
                CompoundTag expectedIdentity = cultivation(original).serializeNBT().copy();
                float expectedMaximum = original.getMaxHealth();
                FakePlayer respawned = replacement(helper, original);
                original.setHealth(0.0F);
                respawned.restoreFrom(original, false);
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(respawned, false));

                CultivationData recovered = TaixuDimension.recoverTripData(respawned);
                helper.assertTrue(recovered.isInitialized() && recovered.serializeNBT().equals(expectedIdentity),
                        realm.id() + " death changed the retained identity");
                assertHealth(helper, respawned, expectedMaximum, expectedMaximum,
                        realm.id() + " retained realm health");
                helper.assertTrue(!CultivationEvents.selectIdentity(respawned,
                                FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id()),
                        realm.id() + " death incorrectly permitted a replacement identity");
            } finally {
                TaixuDimension.discardCultivationSnapshots(original);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void emptyCapabilityLoadDoesNotEraseReplacementIdentity(GameTestHelper helper) {
        CultivationData original = new CultivationData();
        original.begin(FamilyOrigin.FALLEN, CultivationPath.SECT, RandomSource.create(930L));
        helper.assertTrue(original.grantDirectRealm(CultivationRealm.PURPLE_MANSION, RandomSource.create(931L)),
                "Could not prepare a valid identity to clone");
        CultivationData replacement = new CultivationData();
        replacement.copyFrom(original);
        CompoundTag expected = replacement.serializeNBT().copy();
        replacement.deserializeNBT(new CompoundTag());
        replacement.deserializeNBT(new CultivationData().serializeNBT());
        helper.assertTrue(replacement.isInitialized() && replacement.serializeNBT().equals(expected),
                "A default capability load erased a valid cloned identity");

        replacement.resetForDeath();
        replacement.deserializeNBT(new CultivationData().serializeNBT());
        helper.assertTrue(!replacement.isInitialized(), "An explicit death reset was undone by an empty load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void newWorldDoesNotReuseLiveIdentityButSavedWorldStillReloads(GameTestHelper helper) {
        FakePlayer original = preparedPlayer(helper, "world_switch", CultivationRealm.PURPLE_MANSION);
        try {
            CompoundTag savedPersistentData = original.getPersistentData().copy();
            CompoundTag expectedIdentity = cultivation(original).serializeNBT().copy();
            float expectedMaximum = original.getMaxHealth();
            TaixuDimension.clearLiveState();
            helper.assertTrue(original.getPersistentData().equals(savedPersistentData),
                    "Clearing a server session erased the saved character");

            // FakePlayer has no Netty channel for Forge's unrelated login listeners.
            CultivationEvents events = new CultivationEvents();
            FakePlayer freshWorldPlayer = replacement(helper, original);
            events.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(freshWorldPlayer));
            helper.assertTrue(!TaixuDimension.recoverTripData(freshWorldPlayer).isInitialized(),
                    "A fresh world reused the previous world's high-realm identity for the same UUID");
            assertHealth(helper, freshWorldPlayer, 20.0F, 20.0F, "Fresh world before identity selection");

            FakePlayer savedWorldPlayer = replacement(helper, original);
            savedWorldPlayer.getPersistentData().merge(savedPersistentData);
            events.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(savedWorldPlayer));
            CultivationData recovered = TaixuDimension.recoverTripData(savedWorldPlayer);
            helper.assertTrue(recovered.isInitialized() && recovered.serializeNBT().equals(expectedIdentity),
                    "Clearing a server session prevented legitimate persisted identity recovery");
            assertHealth(helper, savedWorldPlayer, expectedMaximum, expectedMaximum,
                    "Reloading the saved high-realm character");
        } finally {
            TaixuDimension.discardCultivationSnapshots(original);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void logoutDiscardsLiveRecordAndReloadsSavedRecord(GameTestHelper helper) {
        FakePlayer original = preparedPlayer(helper, "logout", CultivationRealm.PURPLE_MANSION);
        try {
            CompoundTag savedPersistentData = original.getPersistentData().copy();
            CompoundTag expectedIdentity = cultivation(original).serializeNBT().copy();
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(original));
            helper.assertTrue(original.getPersistentData().equals(savedPersistentData),
                    "Logging out erased persistent cultivation data");

            FakePlayer disconnectedReplacement = replacement(helper, original);
            helper.assertTrue(!TaixuDimension.recoverTripData(disconnectedReplacement).isInitialized(),
                    "Logging out retained the old mutable identity or snapshot in the UUID cache");

            FakePlayer reconnected = replacement(helper, original);
            reconnected.getPersistentData().merge(savedPersistentData);
            CultivationData recovered = TaixuDimension.recoverTripData(reconnected);
            helper.assertTrue(recovered.isInitialized() && recovered != cultivation(original)
                            && recovered.serializeNBT().equals(expectedIdentity),
                    "Reconnect reused the disconnected record instead of loading persisted cultivation");
        } finally {
            TaixuDimension.discardCultivationSnapshots(original);
        }
        helper.succeed();
    }

    private static FakePlayer preparedPlayer(GameTestHelper helper, String caseName, CultivationRealm realm) {
        UUID id = UUID.nameUUIDFromBytes(("identity_reset_" + caseName).getBytes(StandardCharsets.UTF_8));
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(id, "IdentityResetTest"));
        CultivationData data = cultivation(player);
        data.begin(FamilyOrigin.FALLEN, CultivationPath.WANDERER, RandomSource.create(932L));
        helper.assertTrue(data.grantDirectRealm(realm, RandomSource.create(933L)),
                "Could not prepare realm " + realm.id());
        CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        TaixuDimension.persistCultivationData(player, data);
        TaixuDimension.saveTripSnapshot(player, data);
        return player;
    }

    private static FakePlayer replacement(GameTestHelper helper, FakePlayer original) {
        return new FakePlayer(helper.getLevel(), original.getGameProfile());
    }

    private static CultivationData cultivation(FakePlayer player) {
        return player.getCapability(CultivationCapability.CULTIVATION).orElseThrow(() ->
                new IllegalStateException("Cultivation capability was not attached in the real Forge runtime"));
    }

    private static void assertHealth(GameTestHelper helper, FakePlayer player, float maximum, float health,
                                     String context) {
        helper.assertTrue(Math.abs(player.getMaxHealth() - maximum) < 0.001F,
                context + ": maximum health " + player.getMaxHealth() + " != " + maximum);
        helper.assertTrue(Math.abs(player.getHealth() - health) < 0.001F,
                context + ": health " + player.getHealth() + " != " + health);
    }
}
