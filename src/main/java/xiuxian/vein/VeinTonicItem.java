package xiuxian.vein;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import xiuxian.item.CultivationPillItem;

public final class VeinTonicItem extends CultivationPillItem {
    public VeinTonicItem(String id) {
        super(new Item.Properties().stacksTo(16), id.contains("spring") ? 8 : 0, 0, false,
                id.contains("ember") ? MobEffects.FIRE_RESISTANCE : id.contains("azure") ? MobEffects.NIGHT_VISION : MobEffects.REGENERATION,
                id.contains("spring") ? 200 : 2400, 0, "灵液已吸收。");
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int before = stack.getCount();
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player && result.getCount() < before) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (result.isEmpty()) return bottle;
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        return result;
    }
}
