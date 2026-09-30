package xiuxian.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationData;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.network.XiuxianNetwork;

/** A creative test item used to jump directly to one major realm. */
public final class RealmAscensionPillItem extends Item {
    private final CultivationRealm target;

    public RealmAscensionPillItem(CultivationRealm target) {
        super(new Properties().stacksTo(1));
        this.target = target;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                player.sendSystemMessage(Component.literal("此丹仅供创造模式测试使用。"));
                return InteractionResultHolder.fail(stack);
            }
            CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
            if (data == null || !data.isInitialized()) {
                player.sendSystemMessage(Component.literal("先确立修行身份，才能服用升境丹。"));
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
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide && player.getAbilities().instabuild) {
            CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
            if (data != null && data.grantDirectRealm(target, player.getRandom())) {
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 0.65F, 1.15F);
                if (player instanceof ServerPlayer serverPlayer) {
                    CultivationAttributeSync.sync(serverPlayer, data);
                    serverPlayer.sendSystemMessage(Component.literal(
                            "测试丹药生效：境界已直升至 " + target.displayName() + "一层，功法已随机配置。"));
                }
            }
        }
        return stack;
    }

    private static final class CultivationAttributeSync {
        private static void sync(ServerPlayer player, CultivationData data) {
            XiuxianNetwork.syncCultivation(player, data);
            xiuxian.cultivation.CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        }
    }
}
