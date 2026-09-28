package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.client.screen.CultivationProfileScreen;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationInventoryTab {
    private CultivationInventoryTab() {}

    @SubscribeEvent
    public static void addProfileButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Button profileButton = Button.builder(Component.literal("修行"), button ->
                        Minecraft.getInstance().setScreen(new CultivationProfileScreen(screen)))
                .tooltip(Tooltip.create(Component.literal("修行档案与人物属性")))
                .bounds(screen.width / 2 + 90, screen.height / 2 - 106, 42, 18)
                .build();
        event.addListener(profileButton);
    }
}
