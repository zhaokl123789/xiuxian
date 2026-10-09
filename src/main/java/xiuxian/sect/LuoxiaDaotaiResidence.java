package xiuxian.sect;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;
import xiuxian.sect.DaotaiResidenceGenerator.Placement;
import xiuxian.sect.DaotaiResidenceGenerator.Plan;

/** Unique western cloud palace; completed and interrupted old sites relocate once. */
public final class LuoxiaDaotaiResidence {
    static final int VERSION = 3;
    public static final BlockPos ORIGIN = LuoxiaInnerRealmLayout.DAOTAI_ORIGIN;
    static final BlockPos LEGACY_ORIGIN = new BlockPos(0,80,-500);
    private LuoxiaDaotaiResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if(level.dimension()!=LuoxiaInnerDimension.LEVEL)return;
        var d=DaotaiResidenceConstruction.data(level);
        if(LEGACY_ORIGIN.equals(d.origin))DaotaiResidenceConstruction.cancel(level);
        if(!d.contains(ORIGIN)&&d.origin==null)DaotaiResidenceConstruction.start(level,ORIGIN,createPlan());
        var realm=LuoxiaInnerRealmData.get(level);
        var markers=Map.of("daotai_residence",ORIGIN.offset(0,75,-75),"daotai_star_pool",ORIGIN.offset(112,75,-128),
                "daotai_approach",new BlockPos(0,73,154),"daotai_entrance",ORIGIN.offset(0,2,209),"daotai_upper",ORIGIN.offset(0,144,-92));
        markers.forEach((id,pos)->{if(!pos.equals(realm.markers.get(id))){realm.marker(id,pos);realm.setDirty();}});
    }

    public static boolean isReady(ServerLevel level) {
        return level!=null&&level.dimension()==LuoxiaInnerDimension.LEVEL&&DaotaiResidenceConstruction.data(level).contains(ORIGIN);
    }

    static java.util.List<BlockPos> approachPath() {
        return LuoxiaInnerRoads.path(new BlockPos(0,72,154),new BlockPos(ORIGIN.getX(),80,154),ORIGIN.offset(0,0,211));
    }

    static Plan createPlan() {
        var plan=DaotaiResidenceGenerator.createPlan();
        plan.clearances.set(0,new SiteClearance.Region(-220,-2,-210,220,210));
        clear(plan,-220,78,-710,220,Integer.MAX_VALUE,-290);
        clear(plan,-70,226,340,70,Integer.MAX_VALUE,520);
        for(int step=3;step<=171;step++) {
            int y=72+step,z=160+Math.round(step*270.0F/171);
            clear(plan,-6,y,z,6,y+6,z);
        }
        for(int distance=0;distance<290;distance++) {
            int y=legacyHeight(distance);
            clear(plan,-4,y-1,-distance,4,y+6,-distance);
        }
        addApproach(plan);
        return plan;
    }

    static Plan createInitialPlan() {
        var plan=DaotaiResidenceGenerator.createPlan();
        plan.clearances.set(0,new SiteClearance.Region(-220,-2,-210,220,210));
        addApproach(plan);
        return plan;
    }

    private static void addApproach(Plan plan) {
        for(var op:LuoxiaInnerRoads.build(approachPath(),OrientalBlocks.state("town_white_jade"),
                OrientalBlocks.state("town_jade_railing"),OrientalBlocks.state("daotai_lamp_01")))
            append(plan,op.minX(),op.minY(),op.minZ(),op.maxX(),op.maxY(),op.maxZ(),op.state());
    }

    static int legacyHeight(int distance){return 71+Math.min(9,distance*9/290);}

    private static void clear(Plan plan,int x1,int y1,int z1,int x2,int y2,int z2) {
        plan.clearances.add(new SiteClearance.Region(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2==Integer.MAX_VALUE?y2:y2-ORIGIN.getY(),z2-ORIGIN.getZ()));
    }
    private static void append(Plan plan,int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        var op=new Placement(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),x2-ORIGIN.getX(),y2-ORIGIN.getY(),z2-ORIGIN.getZ(),state);
        plan.placements.add(op);
        plan.minX=Math.min(plan.minX,op.minX());plan.maxX=Math.max(plan.maxX,op.maxX());
        plan.minY=Math.min(plan.minY,op.minY());plan.maxY=Math.max(plan.maxY,op.maxY());
        plan.minZ=Math.min(plan.minZ,op.minZ());plan.maxZ=Math.max(plan.maxZ,op.maxZ());
    }
}
