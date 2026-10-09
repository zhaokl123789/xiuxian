package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared, graded paths use one column per step so corner carving cannot erase adjacent floors. */
final class LuoxiaInnerRoads {
    record Placement(int minX,int minY,int minZ,int maxX,int maxY,int maxZ,BlockState state) {}
    private LuoxiaInnerRoads() {}

    static List<BlockPos> path(BlockPos... corners) {
        var points=new ArrayList<BlockPos>();
        for(int part=1;part<corners.length;part++) {
            var a=corners[part-1];var b=corners[part];
            int dx=Integer.signum(b.getX()-a.getX()),dz=Integer.signum(b.getZ()-a.getZ());
            if(dx!=0&&dz!=0)throw new IllegalArgumentException("Road segments must follow one axis");
            int length=Math.abs(b.getX()-a.getX())+Math.abs(b.getZ()-a.getZ());
            if(length==0||Math.abs(b.getY()-a.getY())>length)throw new IllegalArgumentException("Invalid road grade");
            for(int step=points.isEmpty()?0:1;step<=length;step++) {
                int run=length>16?length-16:length;
                int progress=length>16?Math.max(0,Math.min(run,step-8)):step;
                points.add(new BlockPos(a.getX()+dx*step,a.getY()+(b.getY()-a.getY())*progress/run,a.getZ()+dz*step));
            }
        }
        return points;
    }

    static List<Placement> build(List<BlockPos> path,BlockState floor,BlockState rail,BlockState lamp) {
        var ops=new ArrayList<Placement>();
        for(int i=0;i<path.size();i++) {
            var p=path.get(i);var next=path.get(Math.min(i+1,path.size()-1));
            var previous=path.get(Math.max(0,i-1));
            var facing=direction(i+1<path.size()?p:previous,i+1<path.size()?next:p);
            boolean north=facing.getAxis()==Direction.Axis.Z;
            int dx=north?3:0,dz=north?0:3,x=p.getX(),y=p.getY(),z=p.getZ();
            ops.add(new Placement(x-dx,y-1,z-dz,x+dx,y,z+dz,floor));
            ops.add(new Placement(x-dx,y+1,z-dz,x+dx,y+6,z+dz,Blocks.AIR.defaultBlockState()));
            if(next.getY()>y)ops.add(new Placement(x-dx,y+1,z-dz,x+dx,y+1,z+dz,
                    Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing)));
            else if(previous.getY()>y)ops.add(new Placement(x-dx,y+1,z-dz,x+dx,y+1,z+dz,
                    Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,direction(p,previous))));
            if(i<9||i>=path.size()-9||nearCorner(path,i))continue;
            for(int side:new int[]{-1,1}) {
                int rx=x+(north?4*side:0),rz=z+(north?0:4*side);
                ops.add(new Placement(rx,y,rz,rx,y,rz,floor));
                ops.add(new Placement(rx,y+1,rz,rx,y+1,rz,rail));
                if(i%12==0)ops.add(new Placement(rx,y+2,rz,rx,y+2,rz,lamp));
            }
        }
        return ops;
    }

    private static boolean nearCorner(List<BlockPos> path,int index) {
        for(int i=Math.max(1,index-8);i<=Math.min(path.size()-2,index+8);i++)
            if(direction(path.get(i-1),path.get(i))!=direction(path.get(i),path.get(i+1)))return true;
        return false;
    }
    private static Direction direction(BlockPos a,BlockPos b) {
        if(b.getX()!=a.getX())return b.getX()>a.getX()?Direction.EAST:Direction.WEST;
        return b.getZ()>a.getZ()?Direction.SOUTH:Direction.NORTH;
    }
}
