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
import xiuxian.sect.MortalTownEvents;

/**
 * Creative-only, reusable inspection tool for the standalone mortal town.
 *
 * <p>The item deliberately generates in the level where it is used.  This
 * keeps town review independent from the cave-heaven transport and lets a
 * reviewer choose a clean, visible site before the town is made persistent.
 * Right-clicking the top of a block uses the clicked block as the town's
 * ground origin; right-clicking air uses the block below the player.</p>
 */
public final class MortalTownInspectionItem extends Item {
    public MortalTownInspectionItem() {
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
        if (!serverPlayer.isCreative()) {
            serverPlayer.sendSystemMessage(Component.literal("凡人城镇验收道具仅供创造模式使用。"));
            return InteractionResult.FAIL;
        }
        // The clicked block is the ground reference; the whole site above it is cleared.
        var origin = context.getClickedPos();
        if (!MortalTownEvents.generate(serverPlayer, origin)) return InteractionResult.FAIL;
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
        if (!serverPlayer.isCreative()) {
            serverPlayer.sendSystemMessage(Component.literal("凡人城镇验收道具仅供创造模式使用。"));
            return InteractionResultHolder.fail(stack);
        }
        if (!MortalTownEvents.generate(serverPlayer, serverPlayer.blockPosition().below())) {
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("右键选址：清理占地并分批生成凡人城镇（创造模式）")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("会清除城镇占地上方的方块；地下仅替换三层地基")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("退出后继续施工；/xiuxian town status 查看进度")
                .withStyle(ChatFormatting.GRAY));
    }
}
