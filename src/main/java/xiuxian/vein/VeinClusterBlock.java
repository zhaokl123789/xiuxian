package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import xiuxian.block.*;

public final class VeinClusterBlock extends OrientalDecorationBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    private final int layer;
    private final VoxelShape[][] stages = new VoxelShape[4][];
    public VeinClusterBlock(Properties properties, double[][] boxes, int layer) {
        super(properties, boxes);
        this.layer = layer;
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
        int[] heights = {4, 7, 11, 16};
        for (int i = 0; i < 4; i++) {
            int h = heights[i];
            stages[i] = VeinShapes.rotated(new double[][]{{2,0,2,14,2,14},
                    {6,2,6,10,h,10},{2,2,3,5,Math.max(3,h-3),6},{11,2,9,14,Math.max(3,h-5),12}});
        }
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(AGE);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var stage = stages[state.getValue(AGE)];
        return VeinShapes.facing(stage, state.getValue(FACING));
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < 3 && random.nextInt(8) == 0
                && level.getBlockState(pos.below()).is(VeinBlocks.state("vein_" + VeinLayers.id(layer) + "_rock").getBlock()))
            level.setBlock(pos, state.cycle(AGE), 3);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild() || state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(AGE, 0), 3);
            popResource(level, pos, new ItemStack(VeinBlocks.material("vein_" + VeinLayers.id(layer) + "_shard")));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, .7F, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
