package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused regression tests for the no-natural-monster area around the completed sect. */
@GameTestHolder("xiuxian_startup")
@PrefixGameTestTemplate(false)
public final class LuoxiaSpawnGameTests {
    private LuoxiaSpawnGameTests() {}

    @GameTest(templateNamespace = "xiuxian_startup", template = "empty", batch = "luoxia_spawn")
    public static void naturalMonsterSpawnsAreDeniedOnlyInsideSite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        LuoxiaSiteData site = LuoxiaSiteData.get(level);
        site.origin = new BlockPos(0, 64, 0);
        site.phase = LuoxiaSiteData.Phase.COMPLETE;

        Zombie inside = new Zombie(level);
        inside.setPos(0.5, 64, 0.5);
        MobSpawnEvent.PositionCheck insideEvent = new MobSpawnEvent.PositionCheck(
                inside, level, MobSpawnType.NATURAL, null);
        LuoxiaSectEvents.naturalMonsterSpawn(insideEvent);
        helper.assertTrue(insideEvent.getResult() == Event.Result.DENY,
                "Natural hostile spawn inside the sect bounds was not denied");

        Zombie outside = new Zombie(level);
        outside.setPos(LuoxiaBlueprint.MAX_X + 2.5, 64, 0.5);
        MobSpawnEvent.PositionCheck outsideEvent = new MobSpawnEvent.PositionCheck(
                outside, level, MobSpawnType.NATURAL, null);
        LuoxiaSectEvents.naturalMonsterSpawn(outsideEvent);
        helper.assertTrue(outsideEvent.getResult() == Event.Result.DEFAULT,
                "Natural hostile spawn outside the sect bounds was incorrectly denied");

        MobSpawnEvent.PositionCheck spawnerEvent = new MobSpawnEvent.PositionCheck(
                inside, level, MobSpawnType.SPAWNER, null);
        LuoxiaSectEvents.naturalMonsterSpawn(spawnerEvent);
        helper.assertTrue(spawnerEvent.getResult() == Event.Result.DEFAULT,
                "Spawner hostile spawn was incorrectly denied");

        Sheep sheep = new Sheep(EntityType.SHEEP, level);
        sheep.setPos(0.5, 64, 0.5);
        MobSpawnEvent.PositionCheck animalEvent = new MobSpawnEvent.PositionCheck(
                sheep, level, MobSpawnType.NATURAL, null);
        LuoxiaSectEvents.naturalMonsterSpawn(animalEvent);
        helper.assertTrue(animalEvent.getResult() == Event.Result.DEFAULT,
                "Natural peaceful spawn was incorrectly denied");
        // This test shares the GameTest server with the long construction
        // test. Restore the saved-data singleton immediately so the spawn
        // fixture cannot make the next test look like an existing sect.
        site.origin = null;
        site.phase = LuoxiaSiteData.Phase.PLANNED;
        site.paused = false;
        site.forceClearing = false;
        site.chunkIndex = 0;
        site.operationIndex = 0;
        site.cellIndex = 0;
        site.changedBlocks = 0;
        site.problem = "";
        site.setDirty();
        helper.succeed();
    }
}
