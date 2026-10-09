package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_layout")
@PrefixGameTestTemplate(false)
public final class LuoxiaLayoutGameTests {
    @GameTest(templateNamespace="xiuxian_layout",template="empty",timeoutTicks=3000000,batch="layout_migration")
    public static void townArrivalAndDistantSitesMigrateTogether(GameTestHelper helper) throws Exception {
        var server=helper.getLevel().getServer();var level=server.getLevel(LuoxiaInnerDimension.LEVEL);
        var realm=LuoxiaInnerRealmData.get(level);
        SectComplexConstruction.cancel(level);DaotaiResidenceConstruction.cancel(level);JindanResidenceConstruction.cancel(level);
        var sect=SectComplexConstruction.data(level);var dao=DaotaiResidenceConstruction.data(level);var jin=JindanResidenceConstruction.data(level);
        sect.origins.clear();dao.origins.clear();jin.origins.clear();
        sect.origins.add(LuoxiaSectComplexResidence.LEGACY_ORIGIN.asLong());
        jin.origins.add(LuoxiaJindanResidence.LEGACY_ORIGIN.asLong());
        dao.origin=LuoxiaDaotaiResidence.LEGACY_ORIGIN;dao.version=1204;dao.phase=DaotaiResidenceConstruction.Phase.BUILD;
        sect.setDirty();dao.setDirty();jin.setDirty();
        realm.arrivalVersion=realm.routesVersion=0;realm.marker("entry_gate",LuoxiaInnerRealmLayout.LEGACY_ENTRY);
        realm.marker("player_custom_layout_marker",new BlockPos(390,90,210));realm.setDirty();
        var outside=List.of(new BlockPos(705,280,-264),new BlockPos(221,280,-290),new BlockPos(-181,280,384),
                new BlockPos(59,180,153),new BlockPos(0,-38,300),new BlockPos(0,48,160),new BlockPos(390,150,210));
        for(var pos:outside)level.setBlock(pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        var old=List.of(new BlockPos(480,230,-520),new BlockPos(0,280,-500),new BlockPos(-360,240,220),
                new BlockPos(58,220,153),new BlockPos(480,73,-30),new BlockPos(0,81,-200),new BlockPos(-100,102,220));
        for(var pos:old)level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        var oldChest=new BlockPos(500,180,-510);var cityChest=new BlockPos(350,90,210);
        for(var pos:List.of(oldChest,cityChest)) {
            level.setBlock(pos,Blocks.CHEST.defaultBlockState(),2);
            ((ChestBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,64));
        }
        var oldReceiver=new BlockPos(0,74,0);level.setBlock(oldReceiver,Blocks.STONE.defaultBlockState(),2);
        long seed=realm.seed;
        var overSect=SectComplexConstruction.data(server.overworld()).save(new CompoundTag());
        var overDao=DaotaiResidenceConstruction.data(server.overworld()).save(new CompoundTag());
        var overJin=JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag());
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        helper.assertTrue(sect.origin.equals(LuoxiaSectComplexResidence.ORIGIN)&&dao.origin.equals(LuoxiaDaotaiResidence.ORIGIN)
                &&jin.origin.equals(LuoxiaJindanResidence.ORIGIN),"Completed/interrupted sites were not relocated");
        helper.assertTrue(level.getBlockState(oldReceiver).isAir(),"Old receiver retained");
        helper.assertTrue(realm.markers.get("entry_gate").equals(LuoxiaInnerDimension.ENTRY),"Entry marker not in town");
        verifySpacing(helper);
        walkway(helper,level,LuoxiaInnerDimension.ENTRY.below());
        var traveler=new RecordingTraveler(server.overworld());traveler.setPos(123.5,80,456.5);traveler.setYRot(35);traveler.setXRot(10);
        helper.assertTrue(LuoxiaInnerDimension.enter(traveler)&&traveler.destination==level
                &&traveler.target.equals(LuoxiaInnerDimension.ENTRY),"Transport did not enter the town reception gate");
        var anchor=traveler.getPersistentData().getCompound("xiuxian_luoxia_inner_return");
        helper.assertTrue(anchor.getDouble("x")==123.5&&anchor.getDouble("z")==456.5&&anchor.getFloat("yaw")==35,
                "Town arrival lost return anchor");
        var reloaded=new boolean[6];monitor(helper,level,reloaded,old);
        helper.startSequence().thenWaitUntil(()-> {
            helper.assertTrue(LuoxiaSectComplexResidence.isReady(level)&&LuoxiaDaotaiResidence.isReady(level)&&LuoxiaJindanResidence.isReady(level),
                    "Distant sites still migrating");
        }).thenExecute(()-> {
            helper.assertTrue(realm.generated&&realm.version==3&&realm.seed==seed&&realm.routesVersion==1,"Layout rebuilt the base realm");
            for(var pos:outside)helper.assertTrue(level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK),"Migration exceeded a reserved site: "+pos);
            for(var pos:old)helper.assertTrue(level.getBlockState(pos).isAir(),"Retired site/road not cleared: "+pos);
            helper.assertTrue(level.getBlockState(oldChest).isAir()&&level.getBlockEntity(oldChest)==null,"Retired inventory survived");
            helper.assertTrue(level.getBlockEntity(cityChest) instanceof ChestBlockEntity chest&&chest.getItem(0).getCount()==64,"Town inventory changed");
            helper.assertTrue(SectComplexConstruction.data(server.overworld()).save(new CompoundTag()).equals(overSect)
                    &&DaotaiResidenceConstruction.data(server.overworld()).save(new CompoundTag()).equals(overDao)
                    &&JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag()).equals(overJin),"Layout altered Overworld saves");
            helper.assertTrue(!SectComplexConstruction.data(level).contains(LuoxiaSectComplexResidence.LEGACY_ORIGIN)
                    &&!DaotaiResidenceConstruction.data(level).contains(LuoxiaDaotaiResidence.LEGACY_ORIGIN)
                    &&!JindanResidenceConstruction.data(level).contains(LuoxiaJindanResidence.LEGACY_ORIGIN),"Retired origins still marked complete");
            for(int i=0;i<reloaded.length;i++)helper.assertTrue(reloaded[i],"Clear/build cursor did not reload: "+i);
            for(var route:List.of(LuoxiaSectComplexResidence.approachPath(),LuoxiaDaotaiResidence.approachPath(),LuoxiaJindanResidence.approachPath()))
                for(var floor:route)walkway(helper,level,floor);
            for(int z=151;z<=160;z++)walkway(helper,level,new BlockPos(0,72,z));
            for(int step=0;step<=10;step++)walkway(helper,level,new BlockPos(0,72-step,160+step));
            var accepted=SectComplexGenerator.createPlan();
            for(var b:accepted.buildings)helper.assertTrue(!level.getBlockState(LuoxiaSectComplexResidence.ORIGIN.offset(b.x(),b.floor(),b.z())).isAir(),"Sect building omitted: "+b.id());
            for(var route:accepted.routes)walkway(helper,level,LuoxiaSectComplexResidence.ORIGIN.offset(route.x(),route.floor(),route.z()));
            helper.assertTrue(!level.getBlockState(LuoxiaDaotaiResidence.ORIGIN.offset(54,113,-92)).isAir(),"Approved Dao-Tai palace missing");
            var jindan=JindanResidenceGenerator.createPlan();
            for(int[] sample:new int[][]{{30,20,-103},{104,15,9},{28,32,0},{58,10,0},{-74,10,-76},
                    {0,30,-10},{0,17,0},{0,18,0},{-178,-2,-164}}) {
                var relative=new BlockPos(sample[0],sample[1],sample[2]);
                var expected=Blocks.AIR.defaultBlockState();
                for(var op:jindan.placements)
                    if(relative.getX()>=op.minX()&&relative.getX()<=op.maxX()
                            &&relative.getY()>=op.minY()&&relative.getY()<=op.maxY()
                            &&relative.getZ()>=op.minZ()&&relative.getZ()<=op.maxZ())expected=op.state();
                helper.assertTrue(level.getBlockState(LuoxiaJindanResidence.ORIGIN.offset(relative)).equals(expected),
                        "Approved Jin-Dan blueprint differs at "+relative);
            }
            SectComplexGameTests.verifyBuiltInteractions(helper,level,LuoxiaSectComplexResidence.ORIGIN,accepted);
            var edit=LuoxiaInnerDimension.ENTRY.offset(5,1,0);level.setBlock(edit,Blocks.EMERALD_BLOCK.defaultBlockState(),2);
            var before=realm.save(new CompoundTag());
            LuoxiaInnerRealmGenerator.ensureGenerated(level);LuoxiaInnerRealmGenerator.ensureGenerated(level);
            helper.assertTrue(realm.save(new CompoundTag()).equals(before)&&level.getBlockState(edit).is(Blocks.EMERALD_BLOCK),"Re-entry rebuilt reception or reset layout");
            helper.assertTrue(SectComplexConstruction.data(level).origin==null&&DaotaiResidenceConstruction.data(level).origin==null
                    &&JindanResidenceConstruction.data(level).origin==null,"Re-entry queued duplicate buildings");
            System.out.println("LUOXIA LAYOUT PASS: town arrival, distant sites, concurrent migration barrier, six cursor reloads, retired sites, retained town/mine/boss, roads and interactions");
        }).thenSucceed();
    }

    private static void monitor(GameTestHelper helper,ServerLevel level,boolean[] restored,List<BlockPos> old) {
        helper.runAfterDelay(200,()-> {
            var sect=SectComplexConstruction.data(level);var dao=DaotaiResidenceConstruction.data(level);var jin=JindanResidenceConstruction.data(level);
            if(!sect.problem.isEmpty()||!dao.problem.isEmpty()||!jin.problem.isEmpty()) {
                helper.fail("Migration paused: "+sect.problem+" / "+dao.problem+" / "+jin.problem);return;
            }
            if(!restored[0]&&sect.phase!=SectComplexConstruction.Phase.BUILD||!restored[3]&&sect.phase==SectComplexConstruction.Phase.BUILD&&sect.chunk>0&&sect.origin!=null) {
                int slot=sect.phase==SectComplexConstruction.Phase.BUILD?3:0;
                var saved=sect.save(new CompoundTag());var loaded=SectComplexConstruction.Data.load(saved);
                helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Sect cursor lost");
                SectComplexConstruction.release(level);level.getDataStorage().set("xiuxian_sect_complexes",loaded);restored[slot]=true;
                System.out.println("LAYOUT RESUME SECT: "+loaded.phase+" chunk="+loaded.chunk+" cell="+loaded.cell);
            }
            if(!restored[1]&&dao.phase!=DaotaiResidenceConstruction.Phase.BUILD||!restored[4]&&dao.phase==DaotaiResidenceConstruction.Phase.BUILD&&dao.chunk>0&&dao.origin!=null) {
                int slot=dao.phase==DaotaiResidenceConstruction.Phase.BUILD?4:1;
                var saved=dao.save(new CompoundTag());var loaded=DaotaiResidenceConstruction.Data.load(saved);
                helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Dao-Tai cursor lost");
                DaotaiResidenceConstruction.release(level);level.getDataStorage().set("xiuxian_daotai_residences",loaded);restored[slot]=true;
                System.out.println("LAYOUT RESUME DAO: "+loaded.phase+" chunk="+loaded.chunk+" cell="+loaded.cell);
            }
            if(!restored[2]&&jin.phase!=JindanResidenceConstruction.Phase.BUILD||!restored[5]&&jin.phase==JindanResidenceConstruction.Phase.BUILD&&jin.chunk>0&&jin.origin!=null) {
                int slot=jin.phase==JindanResidenceConstruction.Phase.BUILD?5:2;
                var saved=jin.save(new CompoundTag());var loaded=JindanResidenceConstruction.Data.load(saved);
                helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Jin-Dan cursor lost");
                JindanResidenceConstruction.release(level);level.getDataStorage().set("xiuxian_jindan_residences",loaded);restored[slot]=true;
                System.out.println("LAYOUT RESUME JIN: "+loaded.phase+" chunk="+loaded.chunk+" cell="+loaded.cell);
            }
            if(LuoxiaInnerRealmLayout.canBuild(level))for(var pos:old)helper.assertTrue(level.getBlockState(pos).isAir(),"Build began before old sites cleared");
            if(!LuoxiaSectComplexResidence.isReady(level)||!LuoxiaDaotaiResidence.isReady(level)||!LuoxiaJindanResidence.isReady(level))monitor(helper,level,restored,old);
        });
    }
    private static void walkway(GameTestHelper helper,ServerLevel level,BlockPos floor) {
        helper.assertTrue(!level.getBlockState(floor).isAir(),"Missing road floor: "+floor);
        int step=level.getBlockState(floor.above()).getBlock() instanceof StairBlock?1:0;
        helper.assertTrue(level.getBlockState(floor.above(step+1)).isAir()&&level.getBlockState(floor.above(step+2)).isAir(),"Blocked road: "+floor);
    }
    private static void verifySpacing(GameTestHelper helper) {
        int[][] boxes={{218,82,562,338},{166,-2056,614,-1544},{-2120,-910,-1680,-490},{1722,46,2078,374}};
        for(int i=0;i<boxes.length;i++)for(int j=i+1;j<boxes.length;j++) {
            var a=boxes[i];var b=boxes[j];int dx=Math.max(0,Math.max(a[0]-b[2],b[0]-a[2])),dz=Math.max(0,Math.max(a[1]-b[3],b[1]-a[3]));
            helper.assertTrue(Math.hypot(dx,dz)>1000,"Landmarks are crowded together");
        }
        helper.assertTrue(LuoxiaInnerDimension.ENTRY.equals(new BlockPos(390,66,300)),"Arrival is outside the town");
    }
    private static final class RecordingTraveler extends FakePlayer {
        ServerLevel destination;BlockPos target;
        RecordingTraveler(ServerLevel level){super(level,new GameProfile(UUID.randomUUID(),"LayoutTraveler"));setGameMode(GameType.CREATIVE);}
        @Override public Entity changeDimension(ServerLevel destination,ITeleporter teleporter) {
            var info=teleporter.getPortalInfo(this,destination,ignored->null);
            this.destination=destination;target=BlockPos.containing(info.pos);return this;
        }
    }
}
