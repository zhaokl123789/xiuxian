package xiuxian.sect;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_initial")
@PrefixGameTestTemplate(false)
public final class LuoxiaInitialBuildingsGameTests {
    @GameTest(templateNamespace="xiuxian_initial",template="empty",timeoutTicks=200,batch="initial_buildings")
    public static void loadedRealmIsCompleteAndInterruptedInitialPalaceRecovers(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        var level=server.getLevel(LuoxiaInnerDimension.LEVEL);
        var realm=LuoxiaInnerRealmData.get(level);
        helper.assertTrue(realm.initialBuildingsVersion==LuoxiaInnerBuildings.VERSION,
                "Initial buildings were not generated during level load");
        helper.assertTrue(LuoxiaSectComplexResidence.isReady(level)&&LuoxiaDaotaiResidence.isReady(level)
                &&LuoxiaJindanResidence.isReady(level),"First arrival would see unfinished buildings");
        assertNoJobs(helper,level);
        var sectPlan=SectComplexGenerator.createPlan();
        for(var b:sectPlan.buildings)helper.assertTrue(!level.getBlockState(LuoxiaSectComplexResidence.ORIGIN.offset(b.x(),b.floor(),b.z())).isAir(),
                "Initial sect hall missing: "+b.id());
        helper.assertTrue(!level.getBlockState(LuoxiaDaotaiResidence.ORIGIN.offset(54,113,-92)).isAir(),"Initial Dao-Tai palace missing");
        assertJindan(helper,level);
        for(var path:List.of(LuoxiaSectComplexResidence.approachPath(),LuoxiaDaotaiResidence.approachPath(),LuoxiaJindanResidence.approachPath()))
            for(var floor:path) {
                helper.assertTrue(!level.getBlockState(floor).isAir(),"Initial road floor missing: "+floor);
                int step=level.getBlockState(floor.above()).getBlock() instanceof StairBlock?1:0;
                helper.assertTrue(level.getBlockState(floor.above(step+1)).isAir()
                        &&level.getBlockState(floor.above(step+2)).isAir(),"Initial road headroom blocked: "+floor);
            }
        SectComplexGameTests.verifyBuiltInteractions(helper,level,LuoxiaSectComplexResidence.ORIGIN,sectPlan);

        // Simulate a save made by the old initial-generation queue, including a paused cursor.
        var jin=JindanResidenceConstruction.data(level);
        jin.origins.clear();jin.origin=LuoxiaJindanResidence.ORIGIN;
        jin.version=1104;jin.phase=JindanResidenceConstruction.Phase.BUILD;
        jin.problem="construction save is incompatible with the current blueprint";jin.setDirty();
        var missing=LuoxiaJindanResidence.ORIGIN.offset(0,-1,0);
        level.setBlock(missing,Blocks.AIR.defaultBlockState(),2);
        var legacy=new BlockPos(-360,240,220);
        level.setBlock(legacy,Blocks.STONE.defaultBlockState(),2);
        var preserved=List.of(LuoxiaSectComplexResidence.ORIGIN.offset(0,160,0),LuoxiaDaotaiResidence.ORIGIN.offset(0,170,0),
                new BlockPos(390,100,210),new BlockPos(0,48,160),new BlockPos(0,-38,300));
        for(var pos:preserved)level.setBlock(pos,Blocks.EMERALD_BLOCK.defaultBlockState(),2);
        realm.initialBuildingsVersion=0;realm.setDirty();
        var overJin=JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag());
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        helper.assertTrue(LuoxiaJindanResidence.isReady(level)&&!level.getBlockState(missing).isAir(),"Interrupted initial Jin-Dan palace not repaired before entry");
        assertJindan(helper,level);
        helper.assertTrue(level.getBlockState(legacy).isAir(),"Initial repair retained the retired Jin-Dan site");
        assertNoJobs(helper,level);
        for(var pos:preserved)helper.assertTrue(level.getBlockState(pos).is(Blocks.EMERALD_BLOCK),"Initial repair erased another landmark: "+pos);
        helper.assertTrue(JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag()).equals(overJin),"Initial repair changed Overworld state");
        var edit=LuoxiaJindanResidence.ORIGIN.offset(0,160,0);
        level.setBlock(edit,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        var saved=realm.save(new CompoundTag());
        var loaded=LuoxiaInnerRealmData.load(saved);
        helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Initial generation guard lost on reload");
        level.getDataStorage().set(LuoxiaInnerRealmData.DATA_ID,loaded);
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        helper.assertTrue(level.getBlockState(edit).is(Blocks.DIAMOND_BLOCK),"Re-entry rebuilt a completed Jin-Dan palace");
        assertNoJobs(helper,level);
        System.out.println("LUOXIA INITIAL PASS: buildings ready before arrival, approved geometry, roads, interactions, interrupted palace recovery, preserved edits and NBT guard");
        helper.succeed();
    }

    private static void assertNoJobs(GameTestHelper helper, net.minecraft.server.level.ServerLevel level) {
        helper.assertTrue(SectComplexConstruction.data(level).origin==null&&DaotaiResidenceConstruction.data(level).origin==null
                &&JindanResidenceConstruction.data(level).origin==null,"Initial generation left a construction queue running");
    }

    private static void assertJindan(GameTestHelper helper, net.minecraft.server.level.ServerLevel level) {
        var plan=JindanResidenceGenerator.createPlan();
        int solids=0;
        for(int[] sample:new int[][]{{30,20,-103},{104,15,9},{28,32,0},{58,10,0},{-74,10,-76},
                {0,30,-10},{0,17,0},{0,18,0},{-178,-2,-164},{0,-1,0}}) {
            var relative=new BlockPos(sample[0],sample[1],sample[2]);
            var expected=Blocks.AIR.defaultBlockState();
            for(var p:plan.placements)if(relative.getX()>=p.minX()&&relative.getX()<=p.maxX()
                    &&relative.getY()>=p.minY()&&relative.getY()<=p.maxY()
                    &&relative.getZ()>=p.minZ()&&relative.getZ()<=p.maxZ())expected=p.state();
            if(!expected.isAir())solids++;
            helper.assertTrue(level.getBlockState(LuoxiaJindanResidence.ORIGIN.offset(relative)).equals(expected),"Initial Jin-Dan blueprint differs at "+relative);
        }
        helper.assertTrue(solids>=4,"Jin-Dan checks did not inspect enough solid structure");
    }
}
