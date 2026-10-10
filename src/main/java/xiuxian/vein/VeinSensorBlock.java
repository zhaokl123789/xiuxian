package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import xiuxian.block.*;

public final class VeinSensorBlock extends OrientalDecorationBlock {
    private final boolean arena;
    public VeinSensorBlock(Properties p, double[][] boxes, boolean arena) { super(p, boxes); this.arena = arena; }
    public int nodes(Level level, BlockPos pos) {
        int count = 0;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-3,-3,-3), pos.offset(3,3,3))) {
            if (!level.hasChunkAt(target)) continue;
            var block = level.getBlockState(target).getBlock();
            var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
            if (block instanceof VeinClusterBlock || (id != null && id.getNamespace().equals("xiuxian") && id.getPath().startsWith("vein_") && id.getPath().endsWith("_ore"))) count++;
        }
        return count;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return Math.min(15, arena ? level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(8), e -> e.isAlive() && !(e instanceof Player)).size() : nodes(level, pos));
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(arena ? "vein.sensor.arena" : "vein.sensor.reading", VeinLayers.atY(pos.getY()), nodes(level, pos), getAnalogOutputSignal(state, level, pos)), true);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }
    @Override public void tick(BlockState s, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        level.updateNeighbourForOutputSignal(pos, this); level.scheduleTick(pos, this, 20);
    }
}
