package xiuxian.vein;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import xiuxian.block.*;

/** Coordinate-based geology: writes only the supplied chunk, never reads adjacent chunks. */
public final class VeinTerrain {
    public static final int VERSION=1, GRID=128, REGION=512, Z_OFFSET=160;
    public static final BlockPos ENTRANCE=new BlockPos(0,64,Z_OFFSET);
    private static final BlockState AIR=Blocks.AIR.defaultBlockState();
    private final long seed;
    private final SimplexNoise caves, minerals, pockets;
    private final java.util.Map<String,BlockState> states;
    private final BlockState[] rock=new BlockState[9],ore=new BlockState[9],crystal=new BlockState[9];
    private final BlockState[] geology=new BlockState[12];
    private final BlockState floor, lamp, brace, pillar, grate, arena, basalt, reward, controller, seat, workbench;
    public VeinTerrain(long seed) {
        states=VeinBlocks.resources().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(e -> e.getKey().substring(5),e -> e.getValue().get().defaultBlockState()));
        this.seed=seed;var random=RandomSource.create(seed);caves=new SimplexNoise(random);minerals=new SimplexNoise(random);pockets=new SimplexNoise(random);
        for(int i=0;i<9;i++) {
            String prefix="vein_"+VeinLayers.id(i+1);
            rock[i]=VeinBlocks.state(prefix+"_rock");ore[i]=VeinBlocks.state(prefix+"_ore");
            crystal[i]=VeinBlocks.state(prefix+"_cluster").setValue(VeinClusterBlock.AGE,3);
        }
        String[] names={"strata_slate","porous_limestone","mica_schist","magnetite","sulfur_rock","salt_rock","red_ironstone","obsidian_breccia","calcite_ribs","fossil_shale","aquifer_rock","rootstone"};
        for(int i=0;i<names.length;i++) geology[i]=VeinBlocks.state("vein_"+names[i]);
        floor=b("reinforced_floor");lamp=b("mining_lamp");brace=b("support_brace");pillar=b("shield_pillar");grate=b("drain_grate");
        arena=b("arena_floor");basalt=b("arena_basalt");reward=b("reward_cache");controller=b("resonance_pedestal");seat=b("rest_seat");workbench=b("survey_bench");
    }
    private BlockState b(String id) { return states.get(id); }
    public static int floorY(int layer) { return 55-(layer-1)*14; }
    public static BlockPos hub(int x,int z,int layer) {
        return new BlockPos(Math.floorDiv(x+GRID/2,GRID)*GRID,floorY(layer)+1,Math.floorDiv(z-Z_OFFSET+GRID/2,GRID)*GRID+Z_OFFSET);
    }
    public static BlockPos room(int x,int z,int layer) {
        // One room per 512x512 region at three depths; the central ninth-layer core is unique.
        int rx=Math.floorDiv(x,REGION),rz=Math.floorDiv(z-Z_OFFSET,REGION);
        if(layer==9 && rx==0 && rz==0) return new BlockPos(0,floorY(9)+1,Z_OFFSET);
        return new BlockPos(rx*REGION+256,floorY(layer)+1,rz*REGION+256+Z_OFFSET);
    }
    public static int roomRadius(int layer) { return layer==9?26:layer==6?23:20; }
    public static BlockPos control(BlockPos center) { return center.offset(0,0,-roomRadius(VeinLayers.atY(center.getY()))+6); }
    public static BlockPos cache(BlockPos center) { return center.offset(0,0,roomRadius(VeinLayers.atY(center.getY()))-5); }
    private long hash(int x,int y,int z) {
        long n=seed ^ (long)x*0x632BE59BD9B4E019L ^ (long)y*0x9E3779B97F4A7C15L ^ (long)z*0x85157AF5L;
        n=(n^(n>>>30))*0xBF58476D1CE4E5B9L;n=(n^(n>>>27))*0x94D049BB133111EBL;return n^(n>>>31);
    }
    private static int gridDistance(int value) { int n=Math.floorMod(value,GRID);return Math.min(n,GRID-n); }
    private record Column(int gx,int gz,int sx,int sz,double[] noise,double[] detail,BlockPos[] rooms) {}
    private Column column(int x,int z) {
        double[] noise=new double[9],detail=new double[9];BlockPos[] rooms=new BlockPos[9];
        for(int layer=1;layer<=9;layer++) {
            noise[layer-1]=caves.getValue(x*.018+layer*13,z*.018-layer*7);
            detail[layer-1]=caves.getValue(x*.049+layer*29,z*.049);
            if(layer%3==0) rooms[layer-1]=room(x,z,layer);
        }
        return new Column(gridDistance(x),gridDistance(z-Z_OFFSET),x-Math.floorDiv(x+GRID/2,GRID)*GRID,
                z-(Math.floorDiv(z-Z_OFFSET+GRID/2,GRID)*GRID+Z_OFFSET),noise,detail,rooms);
    }
    public void fill(ChunkAccess chunk) {
        fill(chunk,false);
    }
    public void fill(ChunkAccess chunk,boolean legacy) {
        var pos=new BlockPos.MutableBlockPos();ChunkPos cp=chunk.getPos();
        for(int lx=0;lx<16;lx++) for(int lz=0;lz<16;lz++) {
            int x=cp.getMinBlockX()+lx,z=cp.getMinBlockZ()+lz;
            Column column=column(x,z);
            for(int y=-63;y<=60;y++) {
                BlockState state=state(x,y,z,column);pos.set(x,y,z);
                if(legacy) {
                    var existing=chunk.getBlockState(pos);
                    if(!existing.is(Blocks.STONE) && !existing.is(Blocks.DEEPSLATE)) continue;
                }
                chunk.setBlockState(pos,state,false);
                if(chunk instanceof net.minecraft.world.level.chunk.ProtoChunk && state.hasBlockEntity()) {
                    var tag=new net.minecraft.nbt.CompoundTag();tag.putString("id","DUMMY");tag.putInt("x",x);tag.putInt("y",y);tag.putInt("z",z);chunk.setBlockEntityNbt(tag);
                }
                if(state.getBlock() instanceof VeinSensorBlock)
                    chunk.getBlockTicks().schedule(new net.minecraft.world.ticks.ScheduledTick<>(state.getBlock(),pos.immutable(),20,0));
            }
        }
        chunk.setUnsaved(true);
    }
    public BlockState state(int x,int y,int z) {
        return state(x,y,z,null);
    }
    private BlockState state(int x,int y,int z,Column column) {
        if(y<=-64) return Blocks.BEDROCK.defaultBlockState();
        if(y>60) return y==63?Blocks.GRASS_BLOCK.defaultBlockState():y<63?Blocks.STONE.defaultBlockState():AIR;
        int layer=VeinLayers.atY(y),index=layer-1,fy=floorY(layer),dy=y-fy;
        int gx=column==null?gridDistance(x):column.gx,gz=column==null?gridDistance(z-Z_OFFSET):column.gz;
        boolean intersection=gx<=7 && gz<=7,passage=gx<=2 || gz<=2;
        int hx=Math.floorDiv(x+GRID/2,GRID)*GRID,hz=Math.floorDiv(z-Z_OFFSET+GRID/2,GRID)*GRID+Z_OFFSET;
        int sx=column==null?x-hx:column.sx,sz=column==null?z-hz:column.sz;
        if(sx==2 && sz==0) return floor;
        if(sx==1 && sz==0 && y>=-56) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.WEST);
        if(sx==0 && sz==0 && y>=-56) return y==-56?Blocks.WATER.defaultBlockState():AIR;
        BlockPos center=column==null?(layer%3==0?room(x,z,layer):null):column.rooms[index];
        // Core spans the origin's negative region edges, so select it by distance as well.
        if(layer==9 && Math.abs(x)<=27 && Math.abs(z-Z_OFFSET)<=27) center=new BlockPos(0,fy+1,Z_OFFSET);
        if(center!=null) {
            int dx=x-center.getX(),dz=z-center.getZ(),radius=roomRadius(layer);
            if(dx*dx+dz*dz<=(radius+2)*(radius+2) && dy>=0 && dy<=11) return roomState(center,dx,dy,dz,layer,radius);
        }
        // A continuous ladder shaft at each grid intersection, with a solid backing column.
        if(intersection && dy==0) return floor;
        if(intersection && dy>=1 && dy<=7) {
            if(Math.abs(sx)==6 && Math.abs(sz)==6) return dy==2?lamp:dy<=4?b("pressure_pillar"):AIR;
            if(dy==1 && sz==5 && sx>=-4 && sx<=4) return workstation(sx,layer);
            return AIR;
        }
        if(passage && dy==0) return (x+z)%16==0?b("survey_tile"):floor;
        if(passage && dy>=1 && dy<=5) {
            int along=gx<=2?z-Z_OFFSET:x,across=gx<=2?gx:gz;
            if(Math.floorMod(along,16)==8 && across==2) return dy==1?lamp:dy==5?brace:AIR;
            return AIR;
        }
        double noise=column==null?caves.getValue(x*.018+layer*13,z*.018-layer*7):column.noise[index];
        double detail=column==null?caves.getValue(x*.049+layer*29,z*.049):column.detail[index];
        boolean cave=noise+.3*detail>.12 && dy>=1 && dy<=7+(int)(2*Math.max(0,noise));
        if(cave) {
            if(dy==1 && (hash(x,fy,z)&31)==0) return crystal[index];
            if(dy==1 && (hash(x,fy,z)&127)==1) return b("crystal_beacon");
            return AIR;
        }
        // Cavern floors remain the matching host so harvested clusters regrow.
        if(dy==0 && noise+.3*detail>.12) return rock[index];
        double vein=minerals.getValue(x*.087,y*.17,z*.087);
        if(vein>.33 || (vein>.12 && (hash(x,y,z)&31)<3+layer/3)) return ore[index];
        if(vein<-.38) {
            int deposit=(int)Math.floorMod(hash(Math.floorDiv(x,12),layer,Math.floorDiv(z,12)),8);
            return switch(deposit) {
                case 0 -> Blocks.COAL_ORE.defaultBlockState();
                case 1 -> Blocks.IRON_ORE.defaultBlockState();
                case 2 -> Blocks.COPPER_ORE.defaultBlockState();
                case 3 -> (layer>=5?Blocks.DEEPSLATE_GOLD_ORE:Blocks.GRAVEL).defaultBlockState();
                case 4 -> (layer>=4?Blocks.REDSTONE_ORE:Blocks.LAPIS_ORE).defaultBlockState();
                case 5 -> (layer>=7?Blocks.DEEPSLATE_DIAMOND_ORE:Blocks.GRAVEL).defaultBlockState();
                case 6 -> (layer>=8?Blocks.DEEPSLATE_EMERALD_ORE:Blocks.IRON_ORE).defaultBlockState();
                default -> (layer<=2?XiuxianBlocks.SPIRIT_STONE_ORE:layer<=4?XiuxianBlocks.MID_SPIRIT_STONE_ORE:
                        layer<=7?XiuxianBlocks.HIGH_SPIRIT_STONE_ORE:XiuxianBlocks.SUPREME_SPIRIT_STONE_ORE).get().defaultBlockState();
            };
        }
        double seam=pockets.getValue(x*.031+layer*10,y*.08,z*.031);
        if(seam>.38) return geology[(int)Math.floorMod(hash(Math.floorDiv(x,32),layer,Math.floorDiv(z,32)),12)];
        return rock[index];
    }
    private BlockState workstation(int sx,int layer) {
        return switch(sx) { case -4 -> b("ore_cache");case -2 -> workbench;case 0 -> b(layer<=3?"washer":layer<=6?"forge":"condenser");case 2 -> b("reagent_cache");case 4 -> seat;default -> AIR; };
    }
    private BlockState roomState(BlockPos center,int dx,int dy,int dz,int layer,int radius) {
        int r2=dx*dx+dz*dz;
        boolean doorway=Math.abs(dx)<=2 && Math.abs(dz)>=radius-1;
        if(dy==0) return r2<=(radius+1)*(radius+1)?(r2>radius*radius?basalt:arena):rock[layer-1];
        if(r2>=radius*radius && !doorway) return basalt;
        if(dy>=8) return r2<(radius-2)*(radius-2)?b("fracture_tile"):basalt;
        if(doorway) {
            if(dy<=3 && dz==-radius) return b(layer==3?"azure_seal_gate":layer==6?"ember_seal_gate":"origin_seal_gate");
            return dy<=6?AIR:basalt;
        }
        if(Math.abs(dx)==radius-5 && Math.abs(dz)<=1) return dy==4?b("pulse_beacon"):dy<=3?pillar:AIR;
        if(dy==1 && dx==0 && dz==-radius+6) return controller;
        if(dy==1 && dx==0 && dz==radius-5) return reward;
        if(dy==1 && dx==5 && dz==-radius+6) return b("arena_sensor");
        if(dy==1 && Math.abs(dx)==radius-4 && dz%8==0) return lamp;
        if(dy==1 && dx==0 && dz==0) return b("resonance_pedestal");
        if(dy==2 && Math.abs(dx)==10 && Math.abs(dz)==10) return b("seal_cage");
        return AIR;
    }
}
