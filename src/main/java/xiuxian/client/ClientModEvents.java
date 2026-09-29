package xiuxian.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xiuxian.client.screen.AlchemyFurnaceScreen;
import xiuxian.menu.XiuxianMenus;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(XiuxianMenus.MORTAL_FURNACE.get(), AlchemyFurnaceScreen::new);
            MenuScreens.register(XiuxianMenus.SPIRIT_FURNACE.get(), AlchemyFurnaceScreen::new);
            MenuScreens.register(XiuxianMenus.EARTH_FURNACE.get(), AlchemyFurnaceScreen::new);
            MenuScreens.register(XiuxianMenus.HEAVEN_FURNACE.get(), AlchemyFurnaceScreen::new);
        });
    }
}
