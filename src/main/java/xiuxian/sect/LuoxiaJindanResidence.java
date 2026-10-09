package xiuxian.sect;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;
import xiuxian.sect.JindanResidenceGenerator.Plan;

/** Unique eastern furnace palace, approached from the town's southern gate. */
public final class LuoxiaJindanResidence {
    static final int VERSION = 2;
    public static final BlockPos ORIGIN = LuoxiaInnerRealmLayout.JINDAN_ORIGIN;
    static final BlockPos LEGACY_ORIGIN = new BlockPos(-360,112,220);
    private LuoxiaJindanResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if(level.dimension()!=LuoxiaInnerDimension.LEVEL)return;
        var d=JindanResidenceConstruction.data(level);
        if(LEGACY_ORIGIN.equals(d.origin))JindanResidenceConstruction.cancel(level);
        if(!d.contains(ORIGIN)&&d.origin==null)JindanResidenceConstruction.start(level,ORIGIN,createPlan());
        var realm=LuoxiaInnerRealmData.get(level);
        var markers=Map.of("jindan_residence",ORIGIN.offset(92,10,0),"jindan_core_furnace",ORIGIN.offset(4,18,4),
                "jindan_future_gate",ORIGIN.offset(121,10,0),"jindan_entrance",ORIGIN.offset(177,8,0),
                "jindan_approach",new BlockPos(390,65,319));
        markers.forEach((id,pos)->{if(!pos.equals(realm.markers.get(id))){realm.marker(id,pos);realm.setDirty();}});
    }
    public static boolean isReady(ServerLevel level) {
        return level!=null&&level.dimension()==LuoxiaInnerDimension.LEVEL&&JindanResidenceConstruction.data(level).contains(ORIGIN);
    }
    static java.util.List<BlockPos> approachPath() {
        return LuoxiaInnerRoads.path(new BlockPos(390,64,319),new BlockPos(390,70,430),
                new BlockPos(2100,119,430),new BlockPos(2100,119,210),ORIGIN.offset(179,7,0));
    }
    static Plan createPlan() {
        var plan=JindanResidenceGenerator.createPlan();
        plan.clearances.set(0,new SiteClearance.Region(-178,-2,-164,178,164));
        clear(plan,-538,110,56,-182,Integer.MAX_VALUE,384);
        clear(plan,-4,73,160,-4,76,160);
        for(int step=1;step<=100;step++) {
            int x=Math.round(-360.0F*step/100),y=72+Math.round(40.0F*step/100),z=160+Math.round(60.0F*step/100);
            clear(plan,x-2,y,z,x+2,y+2,z);
            if(step%10==0)clear(plan,x-5,y+1,z-1,x-3,y+6,z+1);
        }
        for(int distance=0;distance<=181;distance++) {
            int x=-distance,z=160+Math.min(60,distance),y=72+distance*47/181;
            clear(plan,x-2,y-1,z-2,x+2,y+6,z+2);
        }
        addApproach(plan);
        return plan;
    }

    static Plan createInitialPlan() {
        var plan=JindanResidenceGenerator.createPlan();
        plan.clearances.set(0,new SiteClearance.Region(-178,-2,-164,178,164));
        addApproach(plan);
        return plan;
    }

    private static void addApproach(Plan plan) {
        plan.beginSection("approach","Inner realm approach");
        var jade=OrientalBlocks.state("jindan_white_jade");
        for(var op:LuoxiaInnerRoads.build(approachPath(),jade,OrientalBlocks.state("town_jade_railing"),OrientalBlocks.state("jindan_copper_lantern")))
            append(plan,op.minX(),op.minY(),op.minZ(),op.maxX(),op.maxY(),op.maxZ(),op.state());
        for(int rx=178;rx>=122;rx--) {
            int y=ORIGIN.getY()+7+Math.min(2,(178-rx)/20),x=ORIGIN.getX()+rx,z=ORIGIN.getZ();
            append(plan,x,y-1,z-3,x,y,z+3,jade);
            append(plan,x,y+1,z-2,x,y+4,z+2,Blocks.AIR.defaultBlockState());
            if(rx==159||rx==139)append(plan,x,y+1,z-1,x,y+1,z+1,
                    Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));
        }
        plan.finishSection();
    }
    private static void clear(Plan plan,int x1,int y1,int z1,int x2,int y2,int z2) {
        plan.clearances.add(new SiteClearance.Region(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2==Integer.MAX_VALUE?y2:y2-ORIGIN.getY(),z2-ORIGIN.getZ()));
    }
    private static void append(Plan plan,int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        plan.placements.add(new JindanResidenceGenerator.Placement(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2-ORIGIN.getY(),z2-ORIGIN.getZ(),state));
    }
}
