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
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;
import xiuxian.cultivation.TaixuDimension;
import xiuxian.network.XiuxianNetwork;

public class TechniqueManualItem extends Item {
    private final String techniqueId;

    public TechniqueManualItem(String techniqueId) {
        super(new Properties().stacksTo(1));
        this.techniqueId = techniqueId;
    }

    public String techniqueId() {
        return techniqueId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        CultivationData data = player instanceof ServerPlayer serverPlayer
                ? TaixuDimension.recoverTripData(serverPlayer) : null;
        if (data == null || !data.isInitialized()) {
            player.sendSystemMessage(Component.literal("\u5148\u786e\u7acb\u4fee\u884c\u8eab\u4efd\uff0c\u624d\u80fd\u7814\u8bfb\u529f\u6cd5\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (technique == null) return InteractionResultHolder.fail(stack);
        if (data.hasLearnedTechnique(techniqueId)) {
            if (player instanceof ServerPlayer serverPlayer) {
                data.activateTechnique(techniqueId);
                XiuxianNetwork.syncCultivation(serverPlayer, data);
            }
            player.sendSystemMessage(Component.literal("\u4f60\u5df2\u5c06\u300a" + technique.displayName()
                    + "\u300b\u7acb\u4e3a\u5f53\u524d\u529f\u6cd5\u3002"));
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        if (!technique.canBeLearnedAt(data.realm())) {
            player.sendSystemMessage(Component.literal("\u300a" + technique.displayName() + "\u300b\u9002\u4fee\u5883\u754c\u4e3a"
                    + technique.realmRangeLabel() + "\uff0c\u5f53\u524d\u5883\u754c\u65e0\u6cd5\u53c2\u609f\u6b64\u6cd5\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (data.isMeditating()) {
            player.sendSystemMessage(Component.literal("\u8bf7\u5148\u7ed3\u675f\u5410\u7eb3\uff0c\u518d\u53c2\u609f\u65b0\u7684\u529f\u6cd5\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (data.isStudyingTechnique()) {
            player.sendSystemMessage(Component.literal("\u4f60\u6b63\u5728\u53c2\u609f\u53e6\u4e00\u672c\u529f\u6cd5\uff0c\u65e0\u6cd5\u5206\u5fc3\u7ffb\u9605\u3002"));
            return InteractionResultHolder.fail(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !player.onGround() || player.isPassenger()) {
            player.sendSystemMessage(Component.literal("\u9700\u5b89\u7a33\u843d\u5730\u540e\uff0c\u624d\u80fd\u9759\u5fc3\u53c2\u609f\u529f\u6cd5\u3002"));
            return InteractionResultHolder.fail(stack);
        }

        int chance = data.learningChance(technique);
        int roll = player.getRandom().nextInt(100) + 1;
        if (!data.beginTechniqueStudy(techniqueId, technique.learningDurationTicks(), chance, roll,
                player.getX(), player.getY(), player.getZ(), false)) {
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.sendSystemMessage(Component.literal("\u4f60\u4ee5\u6307\u629a\u5377\uff0c\u6c89\u5fc3\u53c2\u609f\u300a"
                + technique.displayName() + "\u300b\u3002"));
        serverPlayer.sendSystemMessage(Component.literal("\u9053\u8bba\uff1a" + technique.doctrine()));
        serverPlayer.sendSystemMessage(Component.literal("\u884c\u529f\uff1a" + technique.method()));
        serverPlayer.sendSystemMessage(Component.literal("\u53c2\u609f\u7ea6\u9700 "
                + technique.learningDurationTicks() / 20 + " \u79d2\uff0c\u6210\u529f\u7387 " + chance
                + "%\uff1b\u53c2\u609f\u6709\u6210\u540e\u5178\u7c4d\u65b9\u4f1a\u6d88\u8017\u3002"));
        serverPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.45F, 0.82F);
        XiuxianNetwork.syncCultivation(serverPlayer, data);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (technique != null) {
            lines.add(Component.literal("\u9053\u8bba\uff1a" + technique.doctrine()));
            lines.add(Component.literal("\u9002\u4fee\uff1a" + technique.realmRangeLabel()
                    + " \u00b7 \u96be\u5ea6 " + technique.learningDifficultyLabel()));
            lines.add(Component.literal("\u529f\u6cd5\u8054\u7cfb\uff1a" + technique.relationSummary()
                    + "\uff1b\u5171\u9e23\u5410\u7eb3 +" + technique.resonanceMeditationBonusPercent() + "%"
                    + "\uff1b\u8fd0\u8f6c\u51cf\u76ca " + technique.drawbackMeditationPercent() + "%"));
            lines.add(Component.literal(technique.effectSummary()));
            lines.add(Component.literal("\u53f3\u952e\u5c55\u5377\u9605\u8bfb\uff0c\u8e72\u4e0b\u52a0\u53f3\u952e\u9759\u5fc3\u53c2\u609f\u3002"));
        }
    }
}
