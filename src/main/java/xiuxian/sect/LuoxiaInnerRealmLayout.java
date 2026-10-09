package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;

/** Separate arrival migration preserves the town, terrain seed and underground landmarks. */
public final class LuoxiaInnerRealmLayout {
    public static final BlockPos CITY_ORIGIN = new BlockPos(390,64,210);
    public static final BlockPos ENTRY = CITY_ORIGIN.offset(0,2,90);
    static final BlockPos LEGACY_ENTRY = new BlockPos(0,72,0);
    public static final BlockPos SECT_ORIGIN = new BlockPos(390,68,-1800);
    public static final BlockPos DAOTAI_ORIGIN = new BlockPos(-1900,80,-700);
    public static final BlockPos JINDAN_ORIGIN = new BlockPos(1900,112,210);
    static final int ARRIVAL_VERSION = 1;

    private LuoxiaInnerRealmLayout() {}

    static boolean canBuild(ServerLevel level) {
        var sect=SectComplexConstruction.data(level);var dao=DaotaiResidenceConstruction.data(level);var jin=JindanResidenceConstruction.data(level);
        return (sect.contains(SECT_ORIGIN)||SECT_ORIGIN.equals(sect.origin)&&sect.phase==SectComplexConstruction.Phase.BUILD)
                &&(dao.contains(DAOTAI_ORIGIN)||DAOTAI_ORIGIN.equals(dao.origin)&&dao.phase==DaotaiResidenceConstruction.Phase.BUILD)
                &&(jin.contains(JINDAN_ORIGIN)||JINDAN_ORIGIN.equals(jin.origin)&&jin.phase==JindanResidenceConstruction.Phase.BUILD);
    }

    static void finishMigration(ServerLevel level) {
        var data=LuoxiaInnerRealmData.get(level);
        if(data.routesVersion<1&&LuoxiaSectComplexResidence.isReady(level)&&LuoxiaDaotaiResidence.isReady(level)&&LuoxiaJindanResidence.isReady(level)) {
            LuoxiaInnerRealmGenerator.restoreSurfaceHub(level);
            data.routesVersion=1;data.setDirty();
        }
    }

    static void ensureArrival(ServerLevel level) {
        var data=LuoxiaInnerRealmData.get(level);
        if(data.arrivalVersion<ARRIVAL_VERSION) {
            SiteClearance.clearBox(level,-8,70,-8,8,78,8);
            int x=ENTRY.getX(),y=ENTRY.getY(),z=ENTRY.getZ();
            SiteClearance.clearBox(level,x-6,y,z-4,x+6,y+9,z+4);
            fill(level,x-6,y-1,z-4,x+6,y-1,z+4,OrientalBlocks.state("town_white_jade"));
            for(int side:new int[]{-1,1}) {
                fill(level,x+side*5,y,z,x+side*5,y+7,z+1,OrientalBlocks.state("town_vermilion_pillar"));
                fill(level,x+side*6,y,z,x+side*6,y,z,OrientalBlocks.state("town_stone_lamp"));
                fill(level,x+side*4,y+6,z,x+side*4,y+6,z,OrientalBlocks.state("town_red_lantern"));
            }
            fill(level,x-5,y+8,z,x+5,y+8,z+1,OrientalBlocks.state("town_roof_ridge"));
            data.arrivalVersion=ARRIVAL_VERSION;data.setDirty();
        }
        if(!ENTRY.equals(data.markers.get("entry_gate"))) {
            data.marker("entry_gate",ENTRY);data.marker("city_arrival",ENTRY);data.setDirty();
        }
    }

    private static void fill(ServerLevel level,int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)
            level.setBlock(new BlockPos(x,y,z),state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
}
