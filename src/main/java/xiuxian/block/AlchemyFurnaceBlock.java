package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.FriendlyByteBuf;

public class AlchemyFurnaceBlock extends FurnaceBlock {
    private final int tier;

    public AlchemyFurnaceBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public static int tier(BlockState state) {
        return state.getBlock() instanceof AlchemyFurnaceBlock furnace ? furnace.tier() : 1;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemyFurnaceBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof Container container) {
            Containers.dropContents(level, pos, container);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, XiuxianBlockEntities.ALCHEMY_FURNACE.get(),
                        AlchemyFurnaceBlockEntity::serverTick);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int requiredLevel = AlchemyFurnaceTier.byLevel(tier).requiredAlchemyLevel();
            xiuxian.cultivation.CultivationData data =
                    xiuxian.cultivation.TaixuDimension.recoverTripData(serverPlayer);
            if (data == null || !data.isInitialized()) {
                serverPlayer.sendSystemMessage(Component.literal("先选择修行身份，才能踏入丹道。"));
                return;
            }
            int alchemyLevel = data.alchemyLevel();
            if (alchemyLevel < requiredLevel) {
                serverPlayer.sendSystemMessage(Component.literal("此炉需炼丹师等级 " + requiredLevel
                        + "，你当前为 " + alchemyLevel + " 级。"));
                return;
            }
            NetworkHooks.openScreen(serverPlayer, furnace, (FriendlyByteBuf buffer) -> buffer.writeVarInt(tier));
        }
    }
}
