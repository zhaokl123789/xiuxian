package xiuxian.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import xiuxian.cultivation.CultivationData;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.cultivation.TaixuDimension;
import xiuxian.network.XiuxianNetwork;

/** A one-use adventure reward that teaches one of the ten hidden fetal-breath spells. */
public final class SpellManualItem extends Item {
    private final String spellId;

    public SpellManualItem(String spellId) {
        super(new Properties().stacksTo(1));
        this.spellId = spellId;
    }

    @Override
    public Component getName(ItemStack stack) {
        CultivationSpell spell = CultivationSpells.byId(spellId);
        return spell == null ? super.getName(stack)
                : Component.literal("\u300a" + spell.displayName() + "\u300b\u672f\u6cd5\u4e66");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        CultivationData data = TaixuDimension.recoverTripData(serverPlayer);
        CultivationSpell spell = CultivationSpells.byId(spellId);
        if (data == null || !data.isInitialized()) {
            player.sendSystemMessage(Component.literal("\u5148\u786e\u7acb\u4fee\u884c\u8eab\u4efd\uff0c\u624d\u80fd\u7814\u8bfb\u672f\u6cd5\u4e66\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (spell == null || !CultivationSpells.requiresSpellbook(spell.id())) {
            return InteractionResultHolder.fail(stack);
        }
        if (data.hasLearnedSpell(spell.id())) {
            player.sendSystemMessage(Component.literal("\u4f60\u5df2\u9886\u609f\u300a" + spell.displayName() + "\u300b\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (!data.learnSpell(spell.id())) {
            player.sendSystemMessage(Component.literal("\u5f53\u524d\u5883\u754c\u8fd8\u65e0\u6cd5\u9886\u609f\u300a" + spell.displayName() + "\u300b\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.sendSystemMessage(Component.literal("\u4f60\u4ece\u672f\u6cd5\u4e66\u4e2d\u9886\u609f\u4e86\u300a" + spell.displayName() + "\u300b\uff01"));
        XiuxianNetwork.syncCultivation(serverPlayer, data);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        CultivationSpell spell = CultivationSpells.byId(spellId);
        if (spell != null) {
            lines.add(Component.literal("\u672f\u6cd5\uff1a" + spell.displayName()));
            lines.add(Component.literal("\u4f7f\u7528\u65b9\u5f0f\uff1a\u53f3\u952e\u7814\u8bfb"));
        }
    }
}
