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
import xiuxian.sect.JindanResidenceEvents;

/** Creative-only reusable token for queuing the standalone Jin-Dan residence. */
public final class JindanResidenceInspectionItem extends Item {
    public JindanResidenceInspectionItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public InteractionResult useOn(UseOnContext c){Player p=c.getPlayer();if(p==null)return InteractionResult.FAIL;if(p.getCooldowns().isOnCooldown(this))return InteractionResult.CONSUME;if(c.getLevel().isClientSide()){p.getCooldowns().addCooldown(this,20);return InteractionResult.SUCCESS;}if(!(p instanceof ServerPlayer sp)){p.sendSystemMessage(Component.literal("该验收道具只能由服务端玩家使用。"));return InteractionResult.FAIL;}boolean ok=JindanResidenceEvents.generate(sp,c.getClickedPos());sp.getCooldowns().addCooldown(this,20);return ok?InteractionResult.CONSUME:InteractionResult.FAIL;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){ItemStack s=p.getItemInHand(h);if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.consume(s);if(l.isClientSide()){p.getCooldowns().addCooldown(this,20);return InteractionResultHolder.success(s);}if(!(p instanceof ServerPlayer sp)){p.sendSystemMessage(Component.literal("该验收道具只能由服务端玩家使用。"));return InteractionResultHolder.fail(s);}boolean ok=JindanResidenceEvents.generate(sp,p.blockPosition().below());sp.getCooldowns().addCooldown(this,20);return ok?InteractionResultHolder.consume(s):InteractionResultHolder.fail(s);}
    @Override public void appendHoverText(ItemStack s,@Nullable Level l,List<Component> t,TooltipFlag f){t.add(Component.literal("Right-click ground to queue the Jin-Dan furnace palace.").withStyle(ChatFormatting.GOLD));t.add(Component.literal("Creative operator only; construction is chunked and restart-safe.").withStyle(ChatFormatting.GRAY));t.add(Component.literal("Progress: /xiuxian jindan status").withStyle(ChatFormatting.GRAY));}
}
