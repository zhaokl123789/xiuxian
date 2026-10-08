package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xiuxian.block.SectInstrumentBlock;
import xiuxian.block.SectInstrumentBlockEntity;
import xiuxian.block.SectSeatBlock;
import xiuxian.entity.SectSeatEntity;
import xiuxian.cultivation.CultivationEvents;
import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.FamilyOrigin;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_sect_complex")
@PrefixGameTestTemplate(false)
public final class SectComplexGameTests {
    @GameTest(templateNamespace="xiuxian_sect_geometry",template="empty",timeoutTicks=400)
    public static void completeBlueprintRetainsRoutesFloorsAndInteractions(GameTestHelper helper) throws Exception {
        new SectComplexVerification(SectComplexGenerator.createPlan()).verify();helper.succeed();
    }
    @GameTest(templateNamespace="xiuxian_sect_geometry",template="empty",timeoutTicks=40)
    public static void rejectedSiteAndRebuildRetainHistory(GameTestHelper helper) throws Exception {
        var level=helper.getLevel().getServer().overworld();var d=SectComplexConstruction.data(level);
        var before=d.save(new CompoundTag());
        helper.assertTrue(!SectComplexGenerator.generate(level,new BlockPos(12000,200,12000)),"Height violation accepted");
        helper.assertTrue(d.save(new CompoundTag()).equals(before),"Rejected site created a construction job");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"sect_denied"));player.setGameMode(GameType.SURVIVAL);
        long old=new BlockPos(12000,40,12000).asLong();d.origins.add(old);
        try {
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute("xiuxian sect rebuild",player.createCommandSourceStack().withPermission(2))==0,"Survival rebuild accepted");
            helper.assertTrue(d.contains(BlockPos.of(old))&&d.origin==null,"Failed rebuild erased completed history");
            var tag=new CompoundTag();tag.putLong("ActiveOrigin",old);tag.putString("Phase","INVALID");tag.putInt("Version",1);
            helper.assertTrue(!SectComplexConstruction.Data.load(tag).problem.isEmpty(),"Unknown phase silently resumed");
        }finally{d.origins.remove(old);d.setDirty();}
        helper.succeed();
    }
    @GameTest(templateNamespace="xiuxian_sect_complex",template="empty",timeoutTicks=120000,batch="sect_full_build")
    public static void fullSectClearsColdChunksResumesAndCompletes(GameTestHelper helper) {
        var level=helper.getLevel().getServer().overworld();var origin=new BlockPos(16000,40,16000);
        var top=origin.offset(-224,level.getMaxBuildHeight()-1-origin.getY(),-256);
        var edge=origin.offset(224,190,256);var outside=edge.east();var below=origin.offset(-224,-25,-256);
        var chest=origin.offset(218,90,250);
        for(var pos:new BlockPos[]{top,edge,outside,below})level.setBlock(pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
        ((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,64));
        helper.assertTrue(SectComplexGenerator.generate(level,origin),"Sect failed to queue");
        helper.assertTrue(!SectComplexGenerator.generate(level,origin.offset(1000,0,0)),"Concurrent job accepted");
        helper.runAtTickTime(200,()-> {
            var d=SectComplexConstruction.data(level);var saved=d.save(new CompoundTag());
            var loaded=SectComplexConstruction.Data.load(saved);
            helper.assertTrue(loaded.save(new CompoundTag()).equals(saved),"Construction cursor lost on save/load");
            SectComplexConstruction.release(level);level.getDataStorage().set("xiuxian_sect_complexes",loaded);
            reportProgress(helper);
        });
        helper.startSequence().thenWaitUntil(()-> {
            var d=SectComplexConstruction.data(level);
            helper.assertTrue(d.origin==null&&d.contains(origin),"Sect still building: "+SectComplexConstruction.status(level));
        }).thenExecute(()-> {
            var d=SectComplexConstruction.data(level);
            helper.assertTrue(d.problem.isEmpty(),"Sect paused: "+d.problem);
            helper.assertTrue(d.origin==null&&d.contains(origin),"Sect still building: "+SectComplexConstruction.status(level));
            helper.assertTrue(level.getBlockState(top).isAir()&&level.getBlockState(edge).isAir(),"Full height/boundary clearing missed blocks");
            helper.assertTrue(level.getBlockState(chest).isAir()&&level.getBlockEntity(chest)==null,"Unoccupied site retained chest");
            helper.assertTrue(level.getBlockState(outside).is(Blocks.DIAMOND_BLOCK)&&level.getBlockState(below).is(Blocks.DIAMOND_BLOCK),"Clearing exceeded site");
            var plan=SectComplexGenerator.createPlan();
            for(var route:plan.routes) {
                var pos=origin.offset(route.x(),route.floor(),route.z());
                helper.assertTrue(!level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()
                        &&level.getBlockState(pos.above(2)).isAir(),"Built route blocked: "+route);
            }
            helper.assertTrue(!SectComplexGenerator.generate(level,origin),"Completed site regenerated");
            helper.assertTrue(SectComplexEvents.protects(level,origin.offset(224,100,256))
                    &&!SectComplexEvents.protects(level,outside),"Natural spawn protection boundary incorrect");
            for(var building:plan.buildings) {
                var pos=origin.offset(building.x(),building.floor(),building.z());
                helper.assertTrue(!level.getBlockState(pos).isAir(),"Cold chunk omitted building: "+building.id());
            }
            verifyBuiltInteractions(helper,level,origin,plan);
            try {
                var visitor=new RecordingVisitor(level);visitor.setGameMode(GameType.CREATIVE);
                var source=visitor.createCommandSourceStack().withPermission(2);
                for(var entry:plan.visits.entrySet()) {
                    helper.assertTrue(level.getServer().getCommands().getDispatcher().execute("xiuxian sect visit "+entry.getKey(),source)==1,"Visit command failed");
                    helper.assertTrue(visitor.destination==level&&visitor.target.equals(origin.offset(entry.getValue())),"Visit requested wrong landing");
                }
                helper.assertTrue(visitor.getAbilities().flying,"Overview did not enable flight");
            }catch(Exception e){throw new RuntimeException(e);}
            var zombie=new Zombie(level);zombie.setPos(origin.getX(),origin.getY()+50,origin.getZ());
            var natural=new MobSpawnEvent.PositionCheck(zombie,level,MobSpawnType.NATURAL,null);
            SectComplexEvents.naturalMonsterSpawn(natural);
            helper.assertTrue(natural.getResult()==Event.Result.DENY,"Natural monster allowed in sect");
            var summoned=new MobSpawnEvent.PositionCheck(zombie,level,MobSpawnType.COMMAND,null);
            SectComplexEvents.naturalMonsterSpawn(summoned);
            helper.assertTrue(summoned.getResult()==Event.Result.DEFAULT,"Command summon denied");
            System.out.println("SECT FULL BUILD PASS: complete clearing, cold chunks, saved cursor resume, 31 buildings and connected routes");
        }).thenSucceed();
    }

    private static void reportProgress(GameTestHelper helper) {
        var level=helper.getLevel().getServer().overworld();var d=SectComplexConstruction.data(level);
        if(!d.problem.isEmpty()){helper.fail("Sect construction paused: "+d.problem);return;}
        if(d.origin==null)return;
        System.out.println("SECT BUILD PROGRESS: "+d.phase+" chunk="+d.chunk+" changed="+d.writes);
        helper.runAfterDelay(200,()->reportProgress(helper));
    }

    static void verifyBuiltInteractions(GameTestHelper helper,ServerLevel level,BlockPos origin,SectComplexGenerator.Plan plan) {
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"sect_interact"));player.setGameMode(GameType.CREATIVE);
        CultivationEvents.selectIdentity(player,FamilyOrigin.MORTAL.id(),CultivationPath.WANDERER.id());
        boolean sat=false,played=false;
        for(var op:plan.placements) {
            var pos=origin.offset(op.minX(),op.minY(),op.minZ());var state=level.getBlockState(pos);
            if(!sat&&state.getBlock() instanceof SectSeatBlock) {
                player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
                state.getBlock().use(state,level,pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
                if(player.getVehicle() instanceof SectSeatEntity seat){player.stopRiding();seat.tick();sat=true;}
            }
            if(!played&&state.getBlock() instanceof SectInstrumentBlock&&level.getBlockEntity(pos) instanceof SectInstrumentBlockEntity instrument) {
                instrument.toggle();instrument.playNextNote();played=instrument.notesPlayed()>0;instrument.toggle();
            }
            if(sat&&played)break;
        }
        helper.assertTrue(sat&&played,"Assembled sect lost usable seating or musical block entities");
    }
    private static final class RecordingVisitor extends FakePlayer {
        ServerLevel destination;BlockPos target;
        RecordingVisitor(ServerLevel level){super(level,new GameProfile(UUID.randomUUID(),"SectVisitor"));}
        @Override public void teleportTo(ServerLevel level,double x,double y,double z,float yaw,float pitch){destination=level;target=BlockPos.containing(x,y,z);}
    }
}
