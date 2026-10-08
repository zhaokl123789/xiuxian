package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_rebuild")
@PrefixGameTestTemplate(false)
public final class ConstructionRebuildGameTests {
    @GameTest(templateNamespace = "xiuxian_geometry", template = "empty", timeoutTicks = 400, batch = "rebuild_commands")
    public static void rebuildQueuesTheLastCompletedSiteForFullClearance(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().overworld();
        var server = level.getServer();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ClearanceTester"));
        player.setGameMode(GameType.CREATIVE);
        // GameTestServer's default operator level is below the inspection commands' requirement.
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
        var source = player.createCommandSourceStack().withPermission(2);
        helper.assertTrue(player.isCreative(), "Test player did not enter creative mode");
        helper.assertTrue(player.hasPermissions(2), "Test player has insufficient operator permission");
        var town = MortalTownConstruction.data(level);
        var jindan = JindanResidenceConstruction.data(level);
        var daotai = DaotaiResidenceConstruction.data(level);
        var savedTown = town.save(new CompoundTag());
        var savedJindan = jindan.save(new CompoundTag());
        var savedDaotai = daotai.save(new CompoundTag());
        try {
            var origin = new BlockPos(12000, 40, 12000);
            town.origin = null; town.origins.clear(); town.origins.add(origin.asLong());
            jindan.origin = null; jindan.origins.clear(); jindan.origins.add(origin.asLong());
            daotai.origin = null; daotai.origins.clear(); daotai.origins.add(origin.asLong());
            player.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian town rebuild", source) == 0
                    && town.origin == null && town.contains(origin), "Rejected rebuild lost the completed site");
            player.setGameMode(GameType.CREATIVE);
            for (String type : new String[]{"town", "jindan", "daotai"}) {
                int result = server.getCommands().getDispatcher().execute("xiuxian " + type + " rebuild", source);
                helper.assertTrue(result == 1, type + " rebuild command failed");
            }
            helper.assertTrue(origin.equals(town.origin) && !town.building && town.origins.isEmpty(),
                    "Town rebuild did not queue full clearance at its previous origin");
            helper.assertTrue(origin.equals(jindan.origin) && jindan.phase == JindanResidenceConstruction.Phase.CLEAR
                    && jindan.origins.isEmpty(), "Jindan rebuild did not queue full clearance");
            helper.assertTrue(origin.equals(daotai.origin) && daotai.phase == DaotaiResidenceConstruction.Phase.CLEAR
                    && daotai.origins.isEmpty(), "Daotai rebuild did not queue full clearance");
            helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian town rebuild", source) == 0,
                    "Repeated rebuild replaced an active job");
        } finally {
            MortalTownConstruction.release(level); JindanResidenceConstruction.release(level);
            DaotaiResidenceConstruction.release(level);
            level.getDataStorage().set("xiuxian_mortal_town_sites", MortalTownConstruction.Data.load(savedTown));
            level.getDataStorage().set("xiuxian_jindan_residences", JindanResidenceConstruction.Data.load(savedJindan));
            level.getDataStorage().set("xiuxian_daotai_residences", DaotaiResidenceConstruction.Data.load(savedDaotai));
            server.getPlayerList().getOps().remove(player.getGameProfile());
        }
        helper.succeed();
    }
}
