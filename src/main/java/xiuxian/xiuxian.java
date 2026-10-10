package xiuxian;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xiuxian.block.XiuxianBlocks;
import xiuxian.block.OrientalBlocks;
import xiuxian.block.SectBlocks;
import xiuxian.block.VeinBlocks;
import xiuxian.vein.VeinWorldgen;
import xiuxian.block.XiuxianBlockEntities;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationEvents;
import xiuxian.cultivation.TaixuFeatures;
import xiuxian.item.XiuxianItems;
import xiuxian.network.XiuxianNetwork;
import xiuxian.menu.XiuxianMenus;
import xiuxian.recipe.XiuxianRecipes;

@Mod("xiuxian")
public class xiuxian {
    public xiuxian() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(CultivationCapability::register);
        TaixuFeatures.register(modEventBus);
        XiuxianNetwork.register();
        XiuxianBlocks.register(modEventBus);
        OrientalBlocks.register(modEventBus);
        SectBlocks.register(modEventBus);
        VeinBlocks.register(modEventBus);
        VeinWorldgen.register(modEventBus);
        XiuxianBlockEntities.register(modEventBus);
        XiuxianMenus.register(modEventBus);
        XiuxianRecipes.register(modEventBus);
        XiuxianItems.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new CultivationEvents());
    }
}
