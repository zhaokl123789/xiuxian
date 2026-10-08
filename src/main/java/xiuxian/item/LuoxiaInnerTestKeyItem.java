package xiuxian.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import xiuxian.sect.LuoxiaInnerDimension;

/**
 * Reusable inspection token for entering the Luoxia inner realm without
 * requiring the exterior construction to be complete.
 *
 * <p>This is intentionally a test/creative-tab item. It is never consumed;
 * right-clicking outside the realm enters it, and right-clicking inside uses
 * the same token to return to the saved position.</p>
 */
public final class LuoxiaInnerTestKeyItem extends Item {
    public LuoxiaInnerTestKeyItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return activate(context.getLevel(), context.getPlayer());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = activate(level, player);
        if (result == InteractionResult.SUCCESS) return InteractionResultHolder.success(stack);
        if (result == InteractionResult.CONSUME) return InteractionResultHolder.consume(stack);
        return InteractionResultHolder.fail(stack);
    }

    private InteractionResult activate(Level level, @Nullable Player player) {
        if (player == null) return InteractionResult.FAIL;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.CONSUME;
        if (level.isClientSide()) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        boolean moved = LuoxiaInnerDimension.isInner(level)
                ? LuoxiaInnerDimension.returnToOrigin(serverPlayer)
                : LuoxiaInnerDimension.enter(serverPlayer);
        if (!moved) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.xiuxian.luoxia_inner_test_key.use")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.xiuxian.luoxia_inner_test_key.return")
                .withStyle(ChatFormatting.GRAY));
    }
}
