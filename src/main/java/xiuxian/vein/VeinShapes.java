package xiuxian.vein;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;

final class VeinShapes {
    private VeinShapes() {}
    static VoxelShape[] rotated(double[][] boxes) {
        VoxelShape[] shapes=new VoxelShape[4];
        for(int turn=0;turn<4;turn++) {
            VoxelShape shape=Shapes.empty();
            for(double[] box:boxes) {
                double x1=box[0],x2=box[3],z1=box[2],z2=box[5];
                for(int i=0;i<turn;i++) { double a=x1,b=x2;x1=16-z2;x2=16-z1;z1=a;z2=b; }
                shape=Shapes.or(shape,Block.box(x1,box[1],z1,x2,box[4],z2));
            }
            shapes[turn]=shape.optimize();
        }
        return shapes;
    }
    static VoxelShape facing(VoxelShape[] shapes,Direction direction) {
        return shapes[switch(direction) { case EAST -> 1;case SOUTH -> 2;case WEST -> 3;default -> 0; }];
    }
}
