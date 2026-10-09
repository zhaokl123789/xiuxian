package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.SectBlocks;

/** Once-per-realm transplant with separate clearance for the retired central sect. */
public final class LuoxiaSectComplexResidence {
    static final int VERSION = 2;
    public static final BlockPos ORIGIN = LuoxiaInnerRealmLayout.SECT_ORIGIN;
    static final BlockPos LEGACY_ORIGIN = new BlockPos(480,68,-520);
    private LuoxiaSectComplexResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if(level.dimension()!=LuoxiaInnerDimension.LEVEL)return;
        var d=SectComplexConstruction.data(level);
        if(LEGACY_ORIGIN.equals(d.origin))SectComplexConstruction.cancel(level);
        if(!d.contains(ORIGIN)&&d.origin==null)SectComplexConstruction.start(level,ORIGIN,createPlan());
        var realm=LuoxiaInnerRealmData.get(level);
        boolean changed=realm.markers.keySet().removeIf(id->id.matches("sect_module_\\d+"));
        var visits=SectComplexGenerator.createPlan().visits;
        for(var visit:visits.entrySet()) {
            var target=ORIGIN.offset(visit.getValue());
            if(!target.equals(realm.markers.get("sect_"+visit.getKey()))) {
                realm.marker("sect_"+visit.getKey(),target);changed=true;
            }
        }
        for(String id:new String[]{"sect_core","sect_court"}) {
            var target=ORIGIN.offset(visits.get(id.equals("sect_core")?"main":"entrance"));
            if(!target.equals(realm.markers.get(id))){realm.marker(id,target);changed=true;}
        }
        if(changed)realm.setDirty();
    }

    public static boolean isReady(ServerLevel level) {
        return level!=null&&level.dimension()==LuoxiaInnerDimension.LEVEL
                &&SectComplexConstruction.data(level).contains(ORIGIN);
    }

    static SectComplexGenerator.Plan createPlan() {
        var plan=SectComplexGenerator.createPlan();
        clear(plan,-58,65,47,58,Integer.MAX_VALUE,153);
        clear(plan,256,44,-776,704,Integer.MAX_VALUE,-264);
        for(int x=0;x<=480;x++) {
            int y=70+x*2/480;
            clear(plan,x,y-1,28,x,y+8,36);
        }
        for(int z=31;z>=-277;z--)clear(plan,476,71,z,484,80,z);
        // Restore the retired mountain's natural ground; the town and underground mine stay outside this box.
        plan.placements.add(0,new SectComplexGenerator.Placement(256-ORIGIN.getX(),44-ORIGIN.getY(),-776-ORIGIN.getZ(),
                704-ORIGIN.getX(),63-ORIGIN.getY(),-264-ORIGIN.getZ(),Blocks.STONE.defaultBlockState()));
        plan.placements.add(1,new SectComplexGenerator.Placement(256-ORIGIN.getX(),64-ORIGIN.getY(),-776-ORIGIN.getZ(),
                704-ORIGIN.getX(),64-ORIGIN.getY(),-264-ORIGIN.getZ(),Blocks.GRASS_BLOCK.defaultBlockState()));
        var paving=SectBlocks.state("sect_cloud_paving");
        // Restore the established avenue after retiring the old halls and gate.
        for(int z=47;z<=150;z++) {
            int y=z<=110?70-Math.min(4,Math.max(0,(z-5)/28)):67+Math.round((z-110)*5.0F/50);
            append(plan,-3,y,z,3,y,z,paving);
            append(plan,-3,y+1,z,3,y+3,z,Blocks.AIR.defaultBlockState());
            if(z%12==0)for(int x:new int[]{-6,6}) {
                append(plan,x,y,z,x,y,z,paving);
                append(plan,x,y+1,z,x,y+1,z,SectBlocks.state("sect_bridge_lamp"));
            }
        }
        addApproach(plan);
        return plan;
    }

    static SectComplexGenerator.Plan createInitialPlan() {
        var plan=SectComplexGenerator.createPlan();
        addApproach(plan);
        return plan;
    }

    private static void addApproach(SectComplexGenerator.Plan plan) {
        for(var op:LuoxiaInnerRoads.build(approachPath(),SectBlocks.state("sect_cloud_paving"),
                SectBlocks.state("sect_jade_railing"),SectBlocks.state("sect_bridge_lamp")))
            append(plan,op.minX(),op.minY(),op.minZ(),op.maxX(),op.maxY(),op.maxZ(),op.state());
    }

    static java.util.List<BlockPos> approachPath() {
        return LuoxiaInnerRoads.path(new BlockPos(390,64,101),ORIGIN.offset(0,4,243));
    }

    private static void clear(SectComplexGenerator.Plan plan,int x1,int y1,int z1,int x2,int y2,int z2) {
        plan.clearances.add(new SiteClearance.Region(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2==Integer.MAX_VALUE?y2:y2-ORIGIN.getY(),z2-ORIGIN.getZ()));
    }

    static boolean onApproach(BlockPos pos) {
        return pos.getY()>=64&&pos.getY()<=80&&pos.getX()>=386&&pos.getX()<=394
                &&pos.getZ()>=ORIGIN.getZ()+243&&pos.getZ()<=101;
    }

    private static void append(SectComplexGenerator.Plan plan,int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        plan.placements.add(new SectComplexGenerator.Placement(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2-ORIGIN.getY(),z2-ORIGIN.getZ(),state));
    }
}
