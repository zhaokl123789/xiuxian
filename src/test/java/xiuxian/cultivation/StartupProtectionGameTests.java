package xiuxian.cultivation;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_startup")
@PrefixGameTestTemplate(false)
public final class StartupProtectionGameTests {
    private static final UUID EXTERNAL_MOVEMENT_MODIFIER =
            UUID.fromString("54a3edfe-8e24-4256-9979-1a72cb38b464");
    private static final UUID EXTERNAL_HEALTH_MODIFIER =
            UUID.fromString("54a3edfe-8e24-4256-9979-1a72cb38b465");

    private StartupProtectionGameTests() {}

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void choiceBlocksDamageAndInteractionUntilConfirmed(GameTestHelper helper) {
        FakePlayer player = freshPlayer(helper, "interaction");
        assertActionCancellation(helper, player, true);
        helper.assertTrue(CultivationEvents.selectIdentity(player,
                        FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id()),
                "Could not confirm the protected player's identity");
        assertActionCancellation(helper, player, false);
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void choiceLocksMotionAndReleasesItInEveryGameMode(GameTestHelper helper) {
        for (GameType gameType : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE, GameType.SPECTATOR}) {
            FakePlayer player = freshPlayer(helper, "movement_" + gameType.getName());
            player.setGameMode(gameType);
            AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement == null) throw new IllegalStateException("Player has no movement speed attribute");
            movement.addTransientModifier(new AttributeModifier(EXTERNAL_MOVEMENT_MODIFIER,
                    "External movement bonus", 0.025D, AttributeModifier.Operation.ADDITION));
            double unlockedSpeed = movement.getValue();
            boolean mayfly = player.getAbilities().mayfly;
            boolean flying = player.getAbilities().flying;
            player.setSprinting(true);
            player.setDeltaMovement(0.5D, -0.3D, 0.2D);

            MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(player.isNoGravity() && !player.isSprinting()
                            && player.getDeltaMovement().lengthSqr() == 0.0D && movement.getValue() == 0.0D,
                    gameType + " could move or fall before choosing an identity");
            helper.assertTrue(player.getAbilities().mayfly == mayfly && player.getAbilities().flying == flying,
                    gameType + " lost its own game-mode flight abilities during identity selection");

            helper.assertTrue(CultivationEvents.selectIdentity(player,
                            FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id()),
                    gameType + " could not finish identity selection");
            helper.assertTrue(!player.isNoGravity() && Math.abs(movement.getValue() - unlockedSpeed) < 0.0001D,
                    gameType + " remained locked after the server accepted the identity");
            helper.assertTrue(movement.getModifier(EXTERNAL_MOVEMENT_MODIFIER) != null,
                    gameType + " lost an unrelated movement modifier");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void loginAndRespawnClearHealthBeforeOpeningIdentity(GameTestHelper helper) {
        for (boolean respawn : new boolean[]{false, true}) {
            FakePlayer player = freshPlayer(helper, "uninitialized_" + respawn);
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health == null) throw new IllegalStateException("Player has no maximum health attribute");
            health.setBaseValue(800.0D);
            player.setHealth(800.0F);
            postLifecycle(player, respawn);
            assertHealth(helper, player, 20.0F, 20.0F, "Uninitialized lifecycle " + respawn);
            helper.assertTrue(!cultivation(player).isInitialized(),
                    "Cleaning up old health invented a starting identity");

            health.addTransientModifier(new AttributeModifier(EXTERNAL_HEALTH_MODIFIER,
                    "External health bonus", 10.0D, AttributeModifier.Operation.ADDITION));
            player.setHealth(30.0F);
            helper.assertTrue(CultivationEvents.selectIdentity(player,
                            FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id()),
                    "Could not select an identity after lifecycle cleanup");
            assertHealth(helper, player, 30.0F, 30.0F, "Accepted identity " + respawn);
            helper.assertTrue(health.getModifier(EXTERNAL_HEALTH_MODIFIER) != null,
                    "Confirming the identity removed another source's health modifier");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", timeoutTicks = 40,
            batch = "cultivation_startup")
    public static void returningCharactersRestoreHealthDuringLifecycle(GameTestHelper helper) {
        for (boolean respawn : new boolean[]{false, true}) {
            FakePlayer player = freshPlayer(helper, "initialized_" + respawn);
            CultivationData data = cultivation(player);
            data.begin(FamilyOrigin.CULTIVATOR, CultivationPath.SECT, RandomSource.create(145L));
            helper.assertTrue(data.grantDirectRealm(CultivationRealm.QI_REFINING, RandomSource.create(146L)),
                    "Could not prepare the returning character's realm");
            float expectedMaximum = (float) (20.0D + data.realm().healthBonusAt(data.realmLevel())
                    + data.techniqueHealthBonus());
            player.setHealth(10.0F);
            postLifecycle(player, respawn);
            assertHealth(helper, player, expectedMaximum, expectedMaximum / 2.0F,
                    "Initialized lifecycle " + respawn);
            helper.assertTrue(data.isInitialized(), "A returning character was sent back to identity selection");
        }
        helper.succeed();
    }

    private static void assertActionCancellation(GameTestHelper helper, FakePlayer player, boolean expected) {
        LivingHurtEvent damage = new LivingHurtEvent(player, player.damageSources().generic(), 4.0F);
        MinecraftForge.EVENT_BUS.post(damage);
        helper.assertTrue(damage.isCanceled() == expected, "Damage protection did not follow identity state");

        BlockPos pos = player.blockPosition();
        BlockEvent.BreakEvent breaking = new BlockEvent.BreakEvent(player.level(), pos,
                Blocks.STONE.defaultBlockState(), player);
        MinecraftForge.EVENT_BUS.post(breaking);
        helper.assertTrue(breaking.isCanceled() == expected, "Block breaking did not follow identity state");

        Pig target = EntityType.PIG.create(player.level());
        if (target == null) throw new IllegalStateException("Could not create the interaction target");
        AttackEntityEvent attack = new AttackEntityEvent(player, target);
        MinecraftForge.EVENT_BUS.post(attack);
        helper.assertTrue(attack.isCanceled() == expected, "Attacking did not follow identity state");

        PlayerInteractEvent[] interactions = {
                new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND),
                new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)),
                new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP),
                new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target),
                new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, target, Vec3.ZERO)
        };
        for (PlayerInteractEvent interaction : interactions) {
            MinecraftForge.EVENT_BUS.post(interaction);
            helper.assertTrue(interaction.isCanceled() == expected,
                    interaction.getClass().getSimpleName() + " did not follow identity state");
        }
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, InteractionHand.MAIN_HAND));
    }

    private static void postLifecycle(FakePlayer player, boolean respawn) {
        if (respawn) MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
        // Forge's global login listeners require a real Netty channel, which FakePlayer does not provide.
        else new CultivationEvents().onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(player));
    }

    private static FakePlayer freshPlayer(GameTestHelper helper, String caseName) {
        UUID id = UUID.nameUUIDFromBytes(("startup_protection_" + caseName).getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "ProtectionTest"));
    }

    private static CultivationData cultivation(FakePlayer player) {
        return player.getCapability(CultivationCapability.CULTIVATION).orElseThrow(() ->
                new IllegalStateException("Cultivation capability was not attached in the real Forge runtime"));
    }

    private static void assertHealth(GameTestHelper helper, FakePlayer player, float maximum, float health,
                                     String context) {
        helper.assertTrue(Math.abs(player.getMaxHealth() - maximum) < 0.001F
                        && Math.abs(player.getHealth() - health) < 0.001F,
                context + ": expected " + health + "/" + maximum + ", got "
                        + player.getHealth() + "/" + player.getMaxHealth());
    }
}
