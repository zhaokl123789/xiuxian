package xiuxian;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationEvents;
import xiuxian.item.XiuxianItems;
import xiuxian.network.XiuxianNetwork;

@Mod("xiuxian")
public class xiuxian {
    public xiuxian() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(CultivationCapability::register);
        XiuxianNetwork.register();
        XiuxianItems.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new CultivationEvents());
    }
}
