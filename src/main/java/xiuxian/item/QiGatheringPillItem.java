package xiuxian.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationData;

public class QiGatheringPillItem extends Item {
    private static final int QI_RESTORED = 20;

    public QiGatheringPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
        if (data == null || !data.isInitialized()) {
            player.sendSystemMessage(Component.literal("先确立修行身份，才能炼化丹药。"));
            return InteractionResultHolder.fail(stack);
        }

        data.addQi(QI_RESTORED);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.15F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("你服下凝气丹，获得 " + QI_RESTORED + " 点修为。当前修为：" + data.qi()));
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
