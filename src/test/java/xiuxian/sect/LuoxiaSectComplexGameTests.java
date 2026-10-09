package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_sect_inner")
@PrefixGameTestTemplate(false)
public final class LuoxiaSectComplexGameTests {
    @GameTest(templateNamespace="xiuxian_sect_inner",template="empty",timeoutTicks=1000000,batch="sect_inner")
    public static void completeTransplantClearsResumesAndPreservesRealm(GameTestHelper helper) throws Exception {
        var server=helper.getLevel().getServer();var level=server.getLevel(LuoxiaInnerDimension.LEVEL);
        var realm=LuoxiaInnerRealmData.get(level);
        // Keep the accepted residences idle during this independent migration test.
        DaotaiResidenceConstruction.cancel(level);JindanResidenceConstruction.cancel(level);
        var daotai=DaotaiResidenceConstruction.data(level);var jindan=JindanResidenceConstruction.data(level);
        if(!daotai.contains(LuoxiaDaotaiResidence.ORIGIN))daotai.origins.add(LuoxiaDaotaiResidence.ORIGIN.asLong());
        if(!jindan.contains(LuoxiaJindanResidence.ORIGIN))jindan.origins.add(LuoxiaJindanResidence.ORIGIN.asLong());
        daotai.setDirty();jindan.setDirty();
        SectComplexConstruction.cancel(level);var d=SectComplexConstruction.data(level);d.origins.clear();d.setDirty();
        var origin=LuoxiaSectComplexResidence.ORIGIN;
        var preserved=List.of(origin.offset(-225,200,-256),origin.offset(224,-25,256),
                new BlockPos(59,180,105),new BlockPos(390,150,210),new BlockPos(-360,180,220),
                new BlockPos(0,280,-500),new BlockPos(0,-38,300),new BlockPos(0,48,160),new BlockPos(0,64,150));
        for(var pos:preserved)level.setBlock(pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        var removed=List.of(origin.offset(-224,level.getMaxBuildHeight()-1-origin.getY(),-256),
                origin.offset(224,200,256),new BlockPos(-58,level.getMaxBuildHeight()-1,47),
                new BlockPos(58,220,153),new BlockPos(30,180,105));
        for(var pos:removed)level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        var chest=new BlockPos(35,190,105);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
        ((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,64));
        realm.marker("sect_module_0",new BlockPos(30,67,105));
        var custom=new BlockPos(390,150,210);realm.marker("player_custom_sect_marker",custom);
        realm.marker("sect_module_custom",custom);
        long seed=realm.seed;
        var overworld=SectComplexConstruction.data(server.overworld()).save(new CompoundTag());
        LuoxiaInnerRealmGenerator.ensureGenerated(level);LuoxiaInnerRealmGenerator.ensureGenerated(level);
        helper.assertTrue(origin.equals(d.origin)&&d.phase==SectComplexConstruction.Phase.CLEAR,"Transplant not scheduled uniquely");
        helper.assertTrue(!realm.markers.containsKey("sect_module_0"),"Retired sect module still used as destination");
        var markers=java.util.Map.copyOf(realm.markers);
        var visitor=new RecordingVisitor(level);visitor.setGameMode(GameType.CREATIVE);
        var source=visitor.createCommandSourceStack().withPermission(2);
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner status",source)==1,"Status command failed");
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner visit entrance",source)==0,"Unfinished visit allowed");
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner retry",source)==0,"Active job restarted");
        d.problem="Test paused construction";d.setDirty();
        visitor.setGameMode(GameType.SURVIVAL);
        var paused=d.save(new CompoundTag());
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner retry",source)==0
                &&d.save(new CompoundTag()).equals(paused),"Denied retry changed paused job");
        visitor.setGameMode(GameType.CREATIVE);
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner retry",source)==1
                &&origin.equals(d.origin)&&d.problem.isEmpty(),"Paused inner job did not restart");
        helper.runAtTickTime(200,()->resume(helper,level));
        resumeDuringBuild(helper,level);
        helper.startSequence().thenWaitUntil(()-> {
            helper.assertTrue(LuoxiaSectComplexResidence.isReady(level),"Sect incomplete: "+SectComplexConstruction.status(level));
        }).thenExecute(()-> {
            var data=SectComplexConstruction.data(level);
            helper.assertTrue(data.origin==null&&data.origins.size()==1&&data.problem.isEmpty(),"Duplicate or paused sect");
            helper.assertTrue(realm.version==3&&realm.seed==seed&&realm.markers.equals(markers),"Realm data reset by migration");
            helper.assertTrue(realm.markers.get("player_custom_sect_marker").equals(custom),"Custom marker changed");
            helper.assertTrue(realm.markers.get("sect_module_custom").equals(custom),"Custom sect module marker changed");
            helper.assertTrue(SectComplexConstruction.data(server.overworld()).save(new CompoundTag()).equals(overworld),"Overworld data changed");
            for(var pos:preserved)helper.assertTrue(level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK),"Other site/boundary erased: "+pos);
            for(var pos:removed)helper.assertTrue(level.getBlockState(pos).isAir(),"Site not completely cleared: "+pos);
            helper.assertTrue(level.getBlockState(chest).isAir()&&level.getBlockEntity(chest)==null,"Retired chest survived");
            var accepted=SectComplexGenerator.createPlan();var reference=new SectComplexVerification(accepted);
            for(var op:accepted.placements) {
                var relative=new BlockPos(op.minX(),op.minY(),op.minZ());
                helper.assertTrue(level.getBlockState(origin.offset(relative)).equals(reference.state(relative.getX(),relative.getY(),relative.getZ())),
                        "Accepted blueprint changed at "+relative);
            }
            for(var route:accepted.routes)walkway(helper,level,origin.offset(route.x(),route.floor(),route.z()));
            SectComplexGameTests.verifyBuiltInteractions(helper,level,origin,accepted);
            for(var floor:LuoxiaSectComplexResidence.approachPath())walkway(helper,level,floor);
            for(int z=47;z<=150;z++) {
                int y=z<=110?70-Math.min(4,Math.max(0,(z-5)/28)):67+Math.round((z-110)*5.0F/50);
                walkway(helper,level,new BlockPos(0,y,z));
            }
            try {
                for(var visit:accepted.visits.entrySet()) {
                    helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner visit "+visit.getKey(),source)==1,"Visit failed");
                    helper.assertTrue(visitor.destination==level&&visitor.target.equals(origin.offset(visit.getValue())),"Visit target incorrect");
                }
                visitor.setGameMode(GameType.SURVIVAL);
                helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian sect inner visit entrance",source)==0,"Survival inspection allowed");
            }catch(Exception e){throw new RuntimeException(e);}
            helper.assertTrue(SectComplexEvents.protects(level,origin.offset(224,200,256))
                    &&SectComplexEvents.protects(level,new BlockPos(390,74,-1000))
                    &&!SectComplexEvents.protects(level,preserved.get(0)),"Spawn protection boundaries incorrect");
            var edit=origin.offset(210,200,230);level.setBlock(edit,Blocks.EMERALD_BLOCK.defaultBlockState(),2);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);LuoxiaInnerRealmGenerator.ensureGenerated(level);
            helper.assertTrue(data.origin==null&&data.origins.size()==1&&level.getBlockState(edit).is(Blocks.EMERALD_BLOCK),"Re-entry rebuilt completed sect");
            System.out.println("SECT INNER PASS: full two-site clearance, clear/build cursor reload, accepted blueprint, approach, unique migration, retained landmarks and visits");
        }).thenSucceed();
    }

    private static void resumeDuringBuild(GameTestHelper helper,ServerLevel level) {
        helper.runAfterDelay(200,()-> {
            var d=SectComplexConstruction.data(level);
            if(d.phase==SectComplexConstruction.Phase.BUILD&&d.cell>0){resume(helper,level);return;}
            if(d.origin!=null)resumeDuringBuild(helper,level);
        });
    }
    private static void resume(GameTestHelper helper,ServerLevel level) {
        var saved=SectComplexConstruction.data(level).save(new CompoundTag());
        var loaded=SectComplexConstruction.Data.load(saved);
        helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Saved cursor changed on load");
        SectComplexConstruction.release(level);level.getDataStorage().set("xiuxian_sect_complexes",loaded);
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        System.out.println("SECT INNER RESUME: "+loaded.phase+" chunk="+loaded.chunk+" operation="+loaded.operation+" cell="+loaded.cell);
    }
    private static void walkway(GameTestHelper helper,ServerLevel level,BlockPos floor) {
        helper.assertTrue(!level.getBlockState(floor).isAir(),"Missing walkway floor: "+floor);
        int stair=level.getBlockState(floor.above()).getBlock() instanceof StairBlock?1:0;
        helper.assertTrue(level.getBlockState(floor.above(stair+1)).isAir()&&level.getBlockState(floor.above(stair+2)).isAir(),"Blocked walkway: "+floor);
    }
    private static final class RecordingVisitor extends FakePlayer {
        ServerLevel destination;BlockPos target;
        RecordingVisitor(ServerLevel level){super(level,new GameProfile(UUID.randomUUID(),"SectInnerVisitor"));}
        @Override public void teleportTo(ServerLevel level,double x,double y,double z,float yaw,float pitch){destination=level;target=BlockPos.containing(x,y,z);}
    }
}
