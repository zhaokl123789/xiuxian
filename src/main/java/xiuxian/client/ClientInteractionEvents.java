package xiuxian.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.cultivation.CultivationTechniques;
import xiuxian.item.TechniqueManualItem;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientInteractionEvents {
    private ClientInteractionEvents() {}

    @SubscribeEvent
    public static void onRightClickTechniqueManual(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide || event.getEntity().isShiftKeyDown()
                || !(event.getItemStack().getItem() instanceof TechniqueManualItem manual)
                || CultivationTechniques.byId(manual.techniqueId()) == null) {
            return;
        }

        ClientScreens.openTechniqueBookScreen(manual.techniqueId());
    }

    @SubscribeEvent
    public static void onRightClickBlockWithTechniqueManual(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide || event.getEntity().isShiftKeyDown()
                || !(event.getItemStack().getItem() instanceof TechniqueManualItem manual)
                || CultivationTechniques.byId(manual.techniqueId()) == null) {
            return;
        }

        ClientScreens.openTechniqueBookScreen(manual.techniqueId());
        event.setCanceled(true);
    }
}
