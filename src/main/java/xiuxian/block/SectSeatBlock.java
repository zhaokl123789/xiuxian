package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import xiuxian.entity.SectSeatEntity;

public final class SectSeatBlock extends OrientalDecorationBlock {
    private final double seatHeight;

    public SectSeatBlock(Properties properties, double[][] boxes, double seatHeight) {
        super(properties, boxes);
        this.seatHeight = seatHeight;
    }

    public double seatHeight() { return seatHeight; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger() || !player.isAlive()
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + seatHeight, pos.getZ() + 0.5) > 16)
            return InteractionResult.PASS;
        if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                || !level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty())
            return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var seats = level.getEntitiesOfClass(SectSeatEntity.class, new AABB(pos).inflate(0.25),
                seat -> seat.seatPos().equals(pos));
        for (var seat : seats) {
            if (seat.isVehicle()) return InteractionResult.CONSUME;
            seat.discard();
        }
        var seat = new SectSeatEntity(level, pos, state, seatHeight);
        if (!level.addFreshEntity(seat)) return InteractionResult.FAIL;
        if (!player.startRiding(seat)) { seat.discard(); return InteractionResult.FAIL; }
        player.setYRot(state.getValue(FACING).toYRot());
        return InteractionResult.CONSUME;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && state.getBlock() != next.getBlock())
            level.getEntitiesOfClass(SectSeatEntity.class, new AABB(pos).inflate(0.25),
                    seat -> seat.seatPos().equals(pos)).forEach(SectSeatEntity::discard);
        super.onRemove(state, level, pos, next, moving);
    }
}
