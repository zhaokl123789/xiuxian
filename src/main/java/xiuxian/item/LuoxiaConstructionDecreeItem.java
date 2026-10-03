package xiuxian.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
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
import xiuxian.sect.LuoxiaSectEvents;

/** A reusable creative inspection tool for the existing resumable construction job. */
public final class LuoxiaConstructionDecreeItem extends Item {
    public LuoxiaConstructionDecreeItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return onItemUseFirst(context.getItemInHand(), context);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.CONSUME;
        if (context.getLevel().isClientSide) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        int result = LuoxiaSectEvents.useDecree(serverPlayer,
                context.getClickedFace() == Direction.UP ? context.getClickedPos() : null);
        if (result != 1) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.consume(stack);
        if (level.isClientSide) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        if (LuoxiaSectEvents.useDecree(serverPlayer, null) != 1) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.xiuxian.luoxia_construction_decree.place")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.xiuxian.luoxia_construction_decree.control")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.xiuxian.luoxia_construction_decree.bounds")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
