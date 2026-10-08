package xiuxian.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import xiuxian.block.SectBlocks;
import xiuxian.block.SectSeatBlock;

/** A transient mount: the seat block owns its lifetime and only one rider may use it. */
public final class SectSeatEntity extends Entity {
    private BlockPos seatPos = BlockPos.ZERO;
    private BlockState seatState;

    public SectSeatEntity(EntityType<? extends SectSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public SectSeatEntity(Level level, BlockPos pos, BlockState state, double height) {
        this(SectBlocks.SEAT_ENTITY.get(), level);
        seatPos = pos.immutable();
        seatState = state;
        setPos(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5);
        setYRot(state.getValue(SectSeatBlock.FACING).toYRot());
    }

    public BlockPos seatPos() { return seatPos; }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }
    @Override public double getPassengersRidingOffset() { return 0.35; }

    @Override public void remove(RemovalReason reason) {
        // Dismount while the mount is still valid so LivingEntity uses the safe landing search.
        if (!level().isClientSide) ejectPassengers();
        super.remove(reason);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (seatState == null || !level().getBlockState(seatPos).equals(seatState)
                || !isVehicle() || getPassengers().stream().anyMatch(rider -> !rider.isAlive()))) discard();
    }

    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = seatState == null ? Direction.NORTH : seatState.getValue(SectSeatBlock.FACING);
        for (Direction direction : new Direction[]{facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()}) {
            for (int dy : new int[]{0, 1, -1}) {
                Vec3 location = DismountHelper.findSafeDismountLocation(passenger.getType(), level(),
                        seatPos.relative(direction).offset(0, dy, 0), true);
                if (location != null) return location;
            }
        }
        return new Vec3(getX(), seatPos.getY() + 1, getZ());
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
