package xiuxian.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class XiuxianItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "xiuxian");

    public static final RegistryObject<Item> QI_GATHERING_PILL = ITEMS.register("qi_gathering_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16)));

    private XiuxianItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
