package xiuxian.block;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class XiuxianBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "xiuxian");

    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE =
            BLOCK_ENTITIES.register("alchemy_furnace", () -> BlockEntityType.Builder
                    .of(AlchemyFurnaceBlockEntity::new, XiuxianBlocks.ALCHEMY_FURNACE.get(),
                            XiuxianBlocks.ALCHEMY_FURNACE_SPIRIT.get(), XiuxianBlocks.ALCHEMY_FURNACE_EARTH.get(),
                            XiuxianBlocks.ALCHEMY_FURNACE_HEAVEN.get()).build(null));

    private XiuxianBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
