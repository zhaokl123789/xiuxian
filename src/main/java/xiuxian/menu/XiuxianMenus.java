package xiuxian.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class XiuxianMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "xiuxian");

    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> MORTAL_FURNACE = register("alchemy_furnace", 1);
    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> SPIRIT_FURNACE = register("alchemy_furnace_spirit", 2);
    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> EARTH_FURNACE = register("alchemy_furnace_earth", 3);
    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> HEAVEN_FURNACE = register("alchemy_furnace_heaven", 4);

    private XiuxianMenus() {}

    public static RegistryObject<MenuType<AlchemyFurnaceMenu>> forTier(int tier) {
        return switch (tier) {
            case 2 -> SPIRIT_FURNACE;
            case 3 -> EARTH_FURNACE;
            case 4 -> HEAVEN_FURNACE;
            default -> MORTAL_FURNACE;
        };
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private static RegistryObject<MenuType<AlchemyFurnaceMenu>> register(String id, int tier) {
        return MENUS.register(id, () -> IForgeMenuType.create(
                (containerId, inventory, buffer) -> new AlchemyFurnaceMenu(containerId, inventory, tier, buffer)));
    }
}
