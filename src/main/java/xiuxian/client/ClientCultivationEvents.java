package xiuxian.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.network.XiuxianNetwork;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCultivationEvents {
    private ClientCultivationEvents() {}

    @SubscribeEvent
    public static void onCultivatorJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer)
                || !CultivationClientState.isInitialized()
                || CultivationClientState.realm().ordinal() < CultivationRealm.QI_REFINING.ordinal()) {
            return;
        }

        int cost = 4 + CultivationClientState.realm().ordinal() * 2;
        if (CultivationClientState.trueQi() >= cost) {
            XiuxianNetwork.requestJumpEnhancement();
        }
    }
}
