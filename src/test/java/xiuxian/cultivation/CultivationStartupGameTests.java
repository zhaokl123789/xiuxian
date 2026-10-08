package xiuxian.cultivation;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_startup")
@PrefixGameTestTemplate(false)
public final class CultivationStartupGameTests {
    private static final UUID LEGACY_HEALTH_MODIFIER =
            UUID.fromString("c0b30001-4dc8-47a4-9281-8575ef9d6001");

    private CultivationStartupGameTests() {}

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40, batch = "cultivation_startup")
    public static void everyIdentityStartsAtTwentyHealth(GameTestHelper helper) {
        for (FamilyOrigin family : FamilyOrigin.values()) {
            for (CultivationPath path : CultivationPath.values()) {
                String caseName = family.id() + "_" + path.id();
                FakePlayer player = freshPlayer(helper, caseName);
                CultivationData data = cultivation(player);
                helper.assertTrue(!data.isInitialized(), caseName + " already had an identity before selection");
                assertHealth(helper, player, 20.0F, 20.0F, caseName + " before selection");

                helper.assertTrue(CultivationEvents.selectIdentity(player, family.id(), path.id()),
                        caseName + " could not select its starting identity");
                helper.assertTrue(data.realm() == CultivationRealm.FETAL_BREATH && data.realmLevel() == 1,
                        caseName + " inherited progression from another character");
                assertHealth(helper, player, 20.0F, 20.0F, caseName + " after selection");
                helper.assertTrue(!CultivationEvents.selectIdentity(player, family.id(), path.id()),
                        caseName + " accepted a second identity selection");
                assertHealth(helper, player, 20.0F, 20.0F, caseName + " after duplicate selection");
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40, batch = "cultivation_startup")
    public static void removingCultivationClampsStaleHealth(GameTestHelper helper) {
        FakePlayer player = freshPlayer(helper, "remove_stale");
        healthAttribute(player).setBaseValue(800.0D);
        player.setHealth(800.0F);
        CultivationAttributeEffects.remove(player);
        assertHealth(helper, player, 20.0F, 20.0F, "Stale health base after identity reset");

        healthAttribute(player).addPermanentModifier(new AttributeModifier(LEGACY_HEALTH_MODIFIER,
                "Legacy cultivation health", 700.0D, AttributeModifier.Operation.ADDITION));
        player.setHealth(400.0F);
        CultivationAttributeEffects.remove(player);
        assertHealth(helper, player, 20.0F, 20.0F, "Legacy health modifier after identity reset");

        healthAttribute(player).setBaseValue(800.0D);
        player.setHealth(9.0F);
        CultivationAttributeEffects.remove(player);
        assertHealth(helper, player, 20.0F, 9.0F, "Identity cleanup must not heal a damaged player");
        helper.assertTrue(!cultivation(player).isInitialized(), "Attribute cleanup created a starting identity");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40, batch = "cultivation_startup")
    public static void directAttributeRefreshClampsHealth(GameTestHelper helper) {
        FakePlayer player = freshPlayer(helper, "refresh_stale");
        CultivationData data = cultivation(player);
        data.begin(FamilyOrigin.CULTIVATOR, CultivationPath.SECT, RandomSource.create(129L));
        healthAttribute(player).setBaseValue(800.0D);
        player.setHealth(600.0F);
        CultivationAttributeEffects.apply(player, data);
        assertHealth(helper, player, 20.0F, 20.0F, "Starting realm refresh must remove illegal health");

        player.setHealth(9.0F);
        for (int tick = 0; tick < 20; tick++) {
            CultivationAttributeEffects.apply(player, data);
            assertHealth(helper, player, 20.0F, 9.0F, "Repeated refresh must not heal damage");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40, batch = "cultivation_startup")
    public static void damagedHealthSurvivesReloadAndAttributeRestoration(GameTestHelper helper) {
        FakePlayer player = freshPlayer(helper, "restore_damage");
        CultivationData data = cultivation(player);
        data.begin(FamilyOrigin.CULTIVATOR, CultivationPath.SECT, RandomSource.create(130L));
        helper.assertTrue(data.grantDirectRealm(CultivationRealm.PURPLE_MANSION, RandomSource.create(131L)),
                "Could not prepare a high-health character");
        CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        float expectedMaximum = player.getMaxHealth();
        float expectedHealth = expectedMaximum * 0.375F;
        player.setHealth(expectedHealth);

        CompoundTag savedCultivation = data.serializeNBT().copy();
        data.deserializeNBT(savedCultivation);
        CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        assertHealth(helper, player, expectedMaximum, expectedHealth, "Reloading persisted cultivation");

        // A replacement entity starts with vanilla attributes; preserve its damage ratio when restoring the realm.
        healthAttribute(player).setBaseValue(20.0D);
        player.setHealth(7.5F);
        CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        assertHealth(helper, player, expectedMaximum, expectedHealth, "Restoring a replacement attribute map");
        for (int tick = 0; tick < 20; tick++) {
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            assertHealth(helper, player, expectedMaximum, expectedHealth, "Repeated restoration must not heal damage");
        }
        helper.succeed();
    }

    private static FakePlayer freshPlayer(GameTestHelper helper, String caseName) {
        UUID id = UUID.nameUUIDFromBytes(("cultivation_startup_" + caseName).getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "StartupTest"));
    }

    private static CultivationData cultivation(FakePlayer player) {
        return player.getCapability(CultivationCapability.CULTIVATION).orElseThrow(() ->
                new IllegalStateException("Cultivation capability was not attached in the real Forge runtime"));
    }

    private static AttributeInstance healthAttribute(FakePlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) throw new IllegalStateException("Player has no maximum health attribute");
        return attribute;
    }

    private static void assertHealth(GameTestHelper helper, FakePlayer player, float maximum, float health,
                                     String context) {
        helper.assertTrue(Math.abs(player.getMaxHealth() - maximum) < 0.001F,
                context + ": maximum health " + player.getMaxHealth() + " != " + maximum);
        helper.assertTrue(Math.abs(player.getHealth() - health) < 0.001F,
                context + ": health " + player.getHealth() + " != " + health);
    }
}
