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
    static final int VERSION = 1;
    public static final BlockPos ORIGIN = new BlockPos(480,68,-520);
    private LuoxiaSectComplexResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if(level.dimension()!=LuoxiaInnerDimension.LEVEL)return;
        var d=SectComplexConstruction.data(level);
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
        // The old 116m court and new mountain are independent sites; never clear their bounding rectangle.
        plan.clearances.add(new SectComplexGenerator.Clearance(-58-ORIGIN.getX(),65-ORIGIN.getY(),
                47-ORIGIN.getZ(),58-ORIGIN.getX(),153-ORIGIN.getZ()));
        var paving=SectBlocks.state("sect_cloud_paving");
        // Restore the established avenue after retiring the old halls and gate.
        for(int z=47;z<=153;z++) {
            int y=z<=110?70-Math.min(4,Math.max(0,(z-5)/28)):67+Math.round((z-110)*5.0F/50);
            append(plan,-3,y,z,3,y,z,paving);
            append(plan,-3,y+1,z,3,y+3,z,Blocks.AIR.defaultBlockState());
            if(z%12==0)for(int x:new int[]{-6,6}) {
                append(plan,x,y,z,x,y,z,paving);
                append(plan,x,y+1,z,x,y+1,z,SectBlocks.state("sect_bridge_lamp"));
            }
        }
        // Branch east from the existing avenue, then north into the accepted mountain entrance.
        for(int x=0;x<=480;x++) road(plan,x,70+x*2/480,32,false,x<480&&70+(x+1)*2/480>70+x*2/480);
        for(int z=31;z>=-277;z--)road(plan,480,72,z,true,false);
        return plan;
    }

    private static void road(SectComplexGenerator.Plan plan,int x,int y,int z,boolean north,boolean stair) {
        int dx=north?3:0,dz=north?0:3;
        append(plan,x-dx,y-1,z-dz,x+dx,y,z+dz,SectBlocks.state("sect_cloud_paving"));
        append(plan,x-dx,y+1,z-dz,x+dx,y+5,z+dz,Blocks.AIR.defaultBlockState());
        if(stair)append(plan,x-dx,y+1,z-dz,x+dx,y+1,z+dz,
                Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
        if(north?z<20:x>12&&x<470) {
            int lx=north?4:0,lz=north?0:4;
            for(int side:new int[]{-1,1}) {
                append(plan,x+side*lx,y,z+side*lz,x+side*lx,y,z+side*lz,SectBlocks.state("sect_cloud_paving"));
                append(plan,x+side*lx,y+1,z+side*lz,x+side*lx,y+1,z+side*lz,SectBlocks.state("sect_jade_railing"));
                if((north?z:x)%12==0)append(plan,x+side*lx,y+2,z+side*lz,x+side*lx,y+2,z+side*lz,SectBlocks.state("sect_bridge_lamp"));
            }
        }
    }

    static boolean onApproach(BlockPos pos) {
        return pos.getY()>=70&&pos.getY()<=78&&((pos.getX()>=0&&pos.getX()<=484&&pos.getZ()>=28&&pos.getZ()<=36)
                ||(pos.getX()>=476&&pos.getX()<=484&&pos.getZ()>=-277&&pos.getZ()<=32));
    }

    private static void append(SectComplexGenerator.Plan plan,int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        plan.placements.add(new SectComplexGenerator.Placement(x1-ORIGIN.getX(),y1-ORIGIN.getY(),z1-ORIGIN.getZ(),
                x2-ORIGIN.getX(),y2-ORIGIN.getY(),z2-ORIGIN.getZ(),state));
    }
}
