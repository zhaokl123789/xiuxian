package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static multipart decoration: no renderer, ticking entity, or external library. */
public class OrientalDecorationBlock extends HorizontalDirectionalBlock {
    private final VoxelShape[] shapes = new VoxelShape[4];

    public OrientalDecorationBlock(Properties properties, double[][] boxes) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (int turns = 0; turns < 4; turns++) {
            VoxelShape shape = Shapes.empty();
            for (double[] source : boxes) {
                double x1 = source[0], z1 = source[2], x2 = source[3], z2 = source[5];
                for (int step = 0; step < turns; step++) {
                    double oldX1 = x1, oldX2 = x2;
                    x1 = 16 - z2; x2 = 16 - z1; z1 = oldX1; z2 = oldX2;
                }
                shape = Shapes.or(shape, Block.box(x1, source[1], z1, x2, source[4], z2));
            }
            shapes[turns] = shape.optimize();
        }
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapes[switch (state.getValue(FACING)) {
            case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0;
        }];
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
