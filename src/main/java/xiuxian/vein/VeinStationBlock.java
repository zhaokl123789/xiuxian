package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import xiuxian.block.*;

public final class VeinStationBlock extends OrientalDecorationBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final String station;
    private final boolean cache;
    public VeinStationBlock(Properties p, double[][] boxes, String id, boolean cache) {
        super(p, boxes); station = id.substring(5); this.cache = cache;
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }
    public boolean cache() { return cache; }
    public String station() { return station; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LIT);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VeinStationBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || cache || type != VeinBlocks.STATION_ENTITY.get() ? null : (l,p,s,e) -> VeinStationBlockEntity.tick(l,p,s,(VeinStationBlockEntity)e);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof VeinStationBlockEntity be) player.openMenu(be);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof VeinStationBlockEntity be) {
            Containers.dropContents(level, pos, be); level.updateNeighbourForOutputSignal(pos, this); level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(container) : 0;
    }
}
