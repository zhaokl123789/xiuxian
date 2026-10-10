package xiuxian.vein;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import xiuxian.block.*;
import xiuxian.sect.*;

@GameTestHolder("xiuxian_vein_gen")
@PrefixGameTestTemplate(false)
public final class VeinGenerationGameTests {
    private static ServerLevel realm(GameTestHelper h) { return h.getLevel().getServer().getLevel(LuoxiaInnerDimension.LEVEL); }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=100)
    public static void allNineLayersContainDistinctAbundantResources(GameTestHelper h) {
        var terrain=new VeinTerrain(741852);
        Set<Block> materialKinds=new HashSet<>();
        for(int layer=1;layer<=9;layer++) {
            int air=0,ore=0,clusters=0;int fy=VeinTerrain.floorY(layer);
            for(int x=40;x<104;x++) for(int z=205;z<269;z++) for(int y=fy;y<=fy+7;y++) {
                var state=terrain.state(x,y,z);materialKinds.add(state.getBlock());
                if(state.isAir()) air++;
                if(state.is(VeinBlocks.state("vein_"+VeinLayers.id(layer)+"_ore").getBlock())) ore++;
                if(state.getBlock() instanceof VeinClusterBlock) {
                    clusters++;h.assertTrue(terrain.state(x,y-1,z).is(VeinBlocks.state("vein_"+VeinLayers.id(layer)+"_rock").getBlock()),"Cluster lacks host rock");
                }
            }
            h.assertTrue(air>500 && ore>200 && clusters>0,"Insufficient layer resources/caverns at "+layer+" air="+air+" ore="+ore+" clusters="+clusters);
        }
        h.assertTrue(materialKinds.size()>35,"Resource diversity too low: "+materialKinds.size());h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=100)
    public static void corridorsJoinAcrossPositiveAndNegativeChunkBoundaries(GameTestHelper h) {
        var terrain=new VeinTerrain(-134646);
        for(int layer=1;layer<=9;layer++) for(int boundary:new int[]{-4096,-512,-128,-16,0,16,128,512,8192}) {
            int fy=VeinTerrain.floorY(layer);
            for(int x=boundary-2;x<=boundary+2;x++) {
                h.assertTrue(!terrain.state(x,fy,161).isAir(),"Corridor floor has a hole");
                h.assertTrue(terrain.state(x,fy+2,161).isAir() && terrain.state(x,fy+3,161).isAir(),"Corridor blocked at chunk boundary: "+x+" layer "+layer);
            }
        }
        for(int y=-56;y<=60;y++) h.assertTrue(terrain.state(1,y,160).is(Blocks.LADDER),"Layer ladder gap at "+y);
        for(int x=-2;x<=2;x++) h.assertTrue(terrain.state(x,-64,160).is(Blocks.BEDROCK),"Bedrock altered");
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=400)
    public static void coldChunksGenerateBeforeArrivalAndPreserveEdits(GameTestHelper h) {
        ServerLevel level=realm(h);h.assertTrue(level.getChunkSource().getGenerator() instanceof VeinChunkGenerator,"New realm did not use custom chunk generator");
        var terrain=new VeinTerrain(VeinChunkGenerator.terrainSeed(level.getChunkSource().randomState()));
        ChunkPos a=new ChunkPos(-780,813),b=new ChunkPos(820,-799);
        long before=System.nanoTime();
        for(ChunkPos cp:List.of(a,b)) {
            var chunk=level.getChunk(cp.x,cp.z);
            for(int layer=1;layer<=9;layer++) {
                int fy=VeinTerrain.floorY(layer);
                for(int dx:new int[]{0,15}) for(int dz:new int[]{0,15}) {
                    var pos=new BlockPos(cp.getMinBlockX()+dx,fy+2,cp.getMinBlockZ()+dz);
                    h.assertTrue(chunk.getBlockState(pos).equals(terrain.state(pos.getX(),pos.getY(),pos.getZ())),"Cold chunk inconsistent with deterministic terrain");
                }
            }
            var top=new BlockPos(cp.getMinBlockX(),63,cp.getMinBlockZ());h.assertTrue(chunk.getBlockState(top).is(Blocks.GRASS_BLOCK),"Underground pass changed surface");
            h.assertTrue(chunk.getHeight(Heightmap.Types.WORLD_SURFACE,0,0)==63,"Underground pass broke surface heightmap");
            var edit=top.below(45);level.setBlock(edit,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);level.getChunk(cp.x,cp.z);
            h.assertTrue(level.getBlockState(edit).is(Blocks.DIAMOND_BLOCK),"Repeated chunk access overwrote player edits");
        }
        System.out.println("VEIN_COLD_CHUNK_MS="+(System.nanoTime()-before)/1_000_000);h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=400)
    public static void mainAccessAndGeneratedWorkstationsRemainUsable(GameTestHelper h) {
        var level=realm(h);
        for(int y=56;y<=73;y++) h.assertTrue(level.getBlockState(new BlockPos(1,y,160)).is(Blocks.LADDER),"Initial landmark pass sealed vein ladder at "+y);
        for(int layer=1;layer<=9;layer++) {
            BlockPos marker=LuoxiaInnerRealmGenerator.marker(level,"vein_l"+layer);
            h.assertTrue(marker.getY()==VeinTerrain.floorY(layer)+1 && level.getBlockState(marker).isAir()
                    && level.getBlockState(marker.above()).isAir() && !level.getBlockState(marker.below()).isAir(),"Layer arrival not walkable "+layer);
        }
        var pos=new BlockPos(256,VeinTerrain.floorY(4)+1,165);
        level.getChunkAt(pos);
        h.assertTrue(level.getBlockState(pos).getBlock() instanceof VeinStationBlock,"Missing generated processing station");
        h.assertTrue(level.getBlockEntity(pos) instanceof VeinStationBlockEntity,"Generated station missing block entity");
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=100)
    public static void legacyEligibilityNeverSelectsExistingChunksOrOldLandmarks(GameTestHelper h) {
        h.assertTrue(!VeinLegacyChunks.eligible(false,false,1000,-1000),"Existing chunk selected for regeneration");
        h.assertTrue(!VeinLegacyChunks.eligible(true,true,1000,-1000),"Modern generator runs twice");
        h.assertTrue(!VeinLegacyChunks.eligible(true,false,0,10) && !VeinLegacyChunks.eligible(true,false,-3,20),"Legacy mine/arena not protected");
        h.assertTrue(VeinLegacyChunks.eligible(true,false,1000,-1000),"Legacy new chunks failed to opt in");h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=100)
    public static void deferredLegacyPassPreservesNonNaturalBlocks(GameTestHelper h) {
        var level=realm(h);var pos=new BlockPos(6404,13,6436);var chunk=level.getChunkAt(pos);
        level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(pos.south(),Blocks.STONE.defaultBlockState(),3);
        new VeinTerrain(17).fill(chunk,true);
        h.assertTrue(level.getBlockState(pos).is(Blocks.CHEST) && level.getBlockState(pos.east()).isAir(),"Legacy first-load pass overwrote storage or cleared space");
        h.assertTrue(level.getBlockState(pos.south()).equals(new VeinTerrain(17).state(pos.getX(),pos.getY(),pos.getZ()+1)),"Legacy first-load pass did not replace natural substrate");
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=400)
    public static void bossRoomsHaveLoadedGatesControlsAndOneTimeRewards(GameTestHelper h) {
        var level=realm(h);var data=VeinEncounterData.get(level);
        for(int layer:new int[]{3,6,9}) {
            var center=VeinTerrain.room(1024,1184,layer);int radius=VeinTerrain.roomRadius(layer);
            for(int cx=(center.getX()-radius-2)>>4;cx<=(center.getX()+radius+2)>>4;cx++)
                for(int cz=(center.getZ()-radius-2)>>4;cz<=(center.getZ()+radius+2)>>4;cz++)level.getChunk(cx,cz);
            var control=VeinTerrain.control(center);h.assertTrue(level.getBlockState(control).getBlock() instanceof VeinSwitchBlock,"Boss controller missing");
            h.assertTrue(level.getBlockState(center.offset(0,0,-radius)).getBlock() instanceof VeinSealBlock,"Boss seal missing");
            h.assertTrue(VeinEncounters.toggleEntrance(level,center.offset(0,0,-radius),true),"Generated entrance was not grouped");
            for(int dx=-2;dx<=2;dx++)for(int y=0;y<3;y++) h.assertTrue(level.getBlockState(center.offset(dx,y,-radius)).getValue(VeinSealBlock.OPEN),"Key did not open entire doorway");
            var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"vein_fight"));player.setPos(control.getX()+.5,control.getY(),control.getZ()+1.5);
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            h.assertTrue(VeinEncounters.activate(level,control,player),"Boss activation not handled");
            var room=data.room(center);h.assertTrue(room.boss!=null,"Boss not spawned");
            Mob boss=(Mob)level.getEntity(room.boss);h.assertTrue(boss!=null && boss.getMaxHealth()>=160,"Boss attributes missing");
            UUID first=room.boss;VeinEncounters.activate(level,control,player);h.assertTrue(first.equals(room.boss),"Duplicate boss spawned");
            boss.hurt(level.damageSources().playerAttack(player),10000);
            h.assertTrue(room.completed && room.rewarded,"Boss death did not finish room");
            var cache=(VeinStationBlockEntity)level.getBlockEntity(VeinTerrain.cache(center));
            h.assertTrue(!cache.isEmpty(),"Boss reward cache empty");int count=0;for(int i=0;i<27;i++)count+=cache.getItem(i).getCount();
            VeinEncounters.activate(level,control,player);VeinEncounters.deliver(level,data,room);
            int again=0;for(int i=0;i<27;i++)again+=cache.getItem(i).getCount();h.assertTrue(count==again,"Repeat activation duplicated rewards");
            var reloaded=VeinEncounterData.load(data.save(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(reloaded.room(center).completed && reloaded.room(center).rewarded && reloaded.room(center).boss.equals(first),"Room ownership lost on reload");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein_gen",template="empty",timeoutTicks=400)
    public static void discardedBossCanRetryWithoutReward(GameTestHelper h) {
        var level=realm(h);var center=VeinTerrain.room(-1536,1696,6);int r=VeinTerrain.roomRadius(6)+2;
        for(int x=(center.getX()-r)>>4;x<=(center.getX()+r)>>4;x++)for(int z=(center.getZ()-r)>>4;z<=(center.getZ()+r)>>4;z++)level.getChunk(x,z);
        var control=VeinTerrain.control(center);var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"vein_retry"));player.setPos(control.getX()+.5,control.getY(),control.getZ()+1.5);
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        VeinEncounters.activate(level,control,player);var room=VeinEncounterData.get(level).room(center);UUID original=room.boss;
        h.assertTrue(original!=null,"Initial trial did not activate");level.getEntity(original).discard();
        h.assertTrue(room.boss==null && !room.completed && !room.rewarded,"Discarded boss granted reward or kept room locked");
        VeinEncounters.activate(level,control,player);h.assertTrue(room.boss!=null && !original.equals(room.boss),"Trial did not recover after discarded boss");
        level.getEntity(room.boss).discard();h.succeed();
    }
}
