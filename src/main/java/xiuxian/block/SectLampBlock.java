package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class SectLampBlock extends OrientalDecorationBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SectLampBlock(Properties properties, double[][] boxes) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 15 : 0), boxes);
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(LIT), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.25F,
                    state.getValue(LIT) ? 0.6F : 0.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
