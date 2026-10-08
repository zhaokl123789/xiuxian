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
import xiuxian.sect.SectComplexEvents;

public final class SectComplexInspectionItem extends Item {
    public SectComplexInspectionItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
        if(player.getCooldowns().isOnCooldown(this))return InteractionResult.CONSUME;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        boolean queued=player instanceof ServerPlayer server&&SectComplexEvents.generate(server,context.getClickedPos());
        player.getCooldowns().addCooldown(this,20);return queued?InteractionResult.CONSUME:InteractionResult.FAIL;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.consume(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        boolean queued=player instanceof ServerPlayer server&&SectComplexEvents.generate(server,player.blockPosition().below());
        player.getCooldowns().addCooldown(this,20);return queued?InteractionResultHolder.consume(stack):InteractionResultHolder.fail(stack);
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> text,TooltipFlag flag) {
        text.add(Component.translatable("item.xiuxian.sect_complex_inspection_token.site").withStyle(ChatFormatting.GOLD));
        text.add(Component.translatable("item.xiuxian.sect_complex_inspection_token.clear").withStyle(ChatFormatting.RED));
        text.add(Component.literal("/xiuxian sect status").withStyle(ChatFormatting.GRAY));
    }
}
