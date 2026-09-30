package xiuxian.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.cultivation.CultivationRealm;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCultivationEvents {
    private static LocalPlayer lastPlayer;
    private static int lastPredictedJumpTick = -1000;

    private ClientCultivationEvents() {}

    @SubscribeEvent
    public static void onCultivatorJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)
                || !CultivationClientState.isInitialized()
                || CultivationClientState.realm().ordinal() < CultivationRealm.QI_REFINING.ordinal()) {
            return;
        }

        if (lastPlayer != player) {
            lastPlayer = player;
            lastPredictedJumpTick = -1000;
        }

        int cost = 4 + CultivationClientState.realm().ordinal() * 2;
        if (CultivationClientState.trueQi() < cost || player.isPassenger() || player.isFallFlying()
                || player.tickCount - lastPredictedJumpTick < 16) return;

        lastPredictedJumpTick = player.tickCount;
        double lift = 0.18D + CultivationClientState.realm().ordinal() * 0.05D;
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, lift, 0.0D));
        CultivationClientState.spendTrueQiLocally(cost);
    }
}
