package xiuxian.item;

import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationData;

public class CultivationPillItem extends Item {
    private final int healing;
    private final int foodPoints;
    private final boolean clearHarmfulEffects;
    private final MobEffect effect;
    private final int effectDuration;
    private final int effectAmplifier;
    private final String message;

    public CultivationPillItem(Properties properties, int healing, int foodPoints, boolean clearHarmfulEffects,
                               MobEffect effect, int effectDuration, int effectAmplifier, String message) {
        super(properties);
        this.healing = healing;
        this.foodPoints = foodPoints;
        this.clearHarmfulEffects = clearHarmfulEffects;
        this.effect = effect;
        this.effectDuration = effectDuration;
        this.effectAmplifier = effectAmplifier;
        this.message = message;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
            if (data == null || !data.isInitialized()) {
                player.sendSystemMessage(Component.literal("先确立修行身份，才能炼化丹药。"));
                return InteractionResultHolder.fail(stack);
            }
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }

        if (!level.isClientSide) {
            CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
            if (data == null || !data.isInitialized()) {
                return stack;
            }
            if (healing > 0) {
                player.heal(healing);
            }
            if (foodPoints > 0) {
                player.getFoodData().eat(foodPoints, 0.8F);
            }
            if (clearHarmfulEffects) {
                for (MobEffectInstance active : new ArrayList<>(player.getActiveEffects())) {
                    if (!active.getEffect().isBeneficial()) {
                        player.removeEffect(active.getEffect());
                    }
                }
            }
            if (effect != null && effectDuration > 0) {
                player.addEffect(new MobEffectInstance(effect, effectDuration, effectAmplifier));
            }
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.7F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(message));
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}
