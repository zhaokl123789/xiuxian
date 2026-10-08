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
import xiuxian.sect.DaotaiResidenceEvents;

/** Creative-only reusable item for placing the standalone Dao-Tai residence. */
public final class DaotaiResidenceInspectionItem extends Item {
    public DaotaiResidenceInspectionItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.CONSUME;
        if (context.getLevel().isClientSide()) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!DaotaiResidenceEvents.generate(serverPlayer, context.getClickedPos())) return InteractionResult.FAIL;
        serverPlayer.getCooldowns().addCooldown(this, 20);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.consume(stack);
        if (level.isClientSide()) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        if (!DaotaiResidenceEvents.generate(serverPlayer, serverPlayer.blockPosition().below())) {
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click ground to queue the large Dao-Tai inspection residence.")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Creative operator only; construction is chunked and restart-safe.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Progress: /xiuxian daotai status")
                .withStyle(ChatFormatting.GRAY));
    }
}
