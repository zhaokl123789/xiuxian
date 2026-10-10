package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import xiuxian.block.*;

public final class VeinSealBlock extends OrientalDecorationBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final String key;
    private final VoxelShape[] opened;
    public VeinSealBlock(Properties p, double[][] boxes, String id) {
        super(p, boxes); key = id.replace("_seal_gate", "_key");
        registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(POWERED, false));
        opened = VeinShapes.rotated(new double[][]{{0,0,5,3,16,11},{13,0,5,16,16,11}});
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(OPEN, POWERED); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? VeinShapes.facing(opened,state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean powered = ctx.getLevel().hasNeighborSignal(ctx.getClickedPos());
        return super.getStateForPlacement(ctx).setValue(POWERED, powered).setValue(OPEN, powered);
    }
    private void change(Level level, BlockPos pos, BlockState state, boolean open) {
        if (!open && !level.getEntities(null, new AABB(pos).deflate(.05)).isEmpty()) { level.scheduleTick(pos, this, 10); return; }
        level.setBlock(pos, state.setValue(OPEN, open), 3);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild() || !player.getItemInHand(hand).is(VeinBlocks.material(key))) return InteractionResult.PASS;
        if (!level.isClientSide && !state.getValue(POWERED)) {
            boolean open=!state.getValue(OPEN);
            if(!(level instanceof ServerLevel server) || !VeinEncounters.toggleEntrance(server,pos,open)) change(level,pos,state,open);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            state = state.setValue(POWERED, powered); level.setBlock(pos, state, 2); change(level, pos, state, powered);
        }
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) change(level, pos, state, false);
    }
}
