package xiuxian.vein;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import xiuxian.block.OrientalDecorationBlock;

public final class VeinSwitchBlock extends OrientalDecorationBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public VeinSwitchBlock(Properties p, double[][] boxes) { super(p, boxes); registerDefaultState(defaultBlockState().setValue(POWERED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(POWERED); }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return state.getValue(POWERED) ? 15 : 0; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel server && VeinEncounters.activate(server,pos,player)) return InteractionResult.CONSUME;
        if (!level.isClientSide) { level.setBlock(pos, state.cycle(POWERED), 3); level.updateNeighborsAt(pos, this); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
