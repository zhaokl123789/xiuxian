package xiuxian.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class XiuxianBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "xiuxian");

    public static final RegistryObject<Block> SPIRIT_STONE_ORE = BLOCKS.register("spirit_stone_ore",
            () -> new Block(Block.Properties.of().strength(3.0F, 3.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MID_SPIRIT_STONE_ORE = BLOCKS.register("mid_spirit_stone_ore",
            () -> new Block(Block.Properties.of().strength(3.4F, 3.4F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> HIGH_SPIRIT_STONE_ORE = BLOCKS.register("high_spirit_stone_ore",
            () -> new Block(Block.Properties.of().strength(3.8F, 3.8F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SUPREME_SPIRIT_STONE_ORE = BLOCKS.register("supreme_spirit_stone_ore",
            () -> new Block(Block.Properties.of().strength(4.2F, 4.2F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ALCHEMY_FURNACE = BLOCKS.register("alchemy_furnace",
            () -> new AlchemyFurnaceBlock(Block.Properties.of().strength(3.5F, 3.5F)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(FurnaceBlock.LIT) ? 13 : 0), 1));
    public static final RegistryObject<Block> ALCHEMY_FURNACE_SPIRIT = BLOCKS.register("alchemy_furnace_spirit",
            () -> new AlchemyFurnaceBlock(Block.Properties.of().strength(3.7F, 3.7F)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(FurnaceBlock.LIT) ? 13 : 0), 2));
    public static final RegistryObject<Block> ALCHEMY_FURNACE_EARTH = BLOCKS.register("alchemy_furnace_earth",
            () -> new AlchemyFurnaceBlock(Block.Properties.of().strength(4.0F, 4.0F)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(FurnaceBlock.LIT) ? 13 : 0), 3));
    public static final RegistryObject<Block> ALCHEMY_FURNACE_HEAVEN = BLOCKS.register("alchemy_furnace_heaven",
            () -> new AlchemyFurnaceBlock(Block.Properties.of().strength(4.3F, 4.3F)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(FurnaceBlock.LIT) ? 13 : 0), 4));

    private XiuxianBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
