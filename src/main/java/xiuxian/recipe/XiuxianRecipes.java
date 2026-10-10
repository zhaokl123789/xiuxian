package xiuxian.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class XiuxianRecipes {
    public static final RecipeType<VeinProcessingRecipe> VEIN_TYPE = RecipeType.simple(new ResourceLocation("xiuxian", "vein_processing"));
    public static final RecipeType<AlchemyRecipe> ALCHEMY_TYPE =
            RecipeType.simple(new ResourceLocation("xiuxian", "alchemy_mortal"));
    public static final RecipeType<AlchemyRecipe> SPIRIT_ALCHEMY_TYPE =
            RecipeType.simple(new ResourceLocation("xiuxian", "alchemy_spirit"));
    public static final RecipeType<AlchemyRecipe> EARTH_ALCHEMY_TYPE =
            RecipeType.simple(new ResourceLocation("xiuxian", "alchemy_earth"));
    public static final RecipeType<AlchemyRecipe> HEAVEN_ALCHEMY_TYPE =
            RecipeType.simple(new ResourceLocation("xiuxian", "alchemy_heaven"));

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "xiuxian");
    public static final RegistryObject<RecipeSerializer<?>> VEIN_SERIALIZER = SERIALIZERS.register("vein_processing", VeinProcessingRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<?>> ALCHEMY_SERIALIZER = SERIALIZERS.register("alchemy",
            () -> new AlchemyRecipe.Serializer(ALCHEMY_TYPE));
    public static final RegistryObject<RecipeSerializer<?>> SPIRIT_ALCHEMY_SERIALIZER = SERIALIZERS.register("alchemy_spirit",
            () -> new AlchemyRecipe.Serializer(SPIRIT_ALCHEMY_TYPE));
    public static final RegistryObject<RecipeSerializer<?>> EARTH_ALCHEMY_SERIALIZER = SERIALIZERS.register("alchemy_earth",
            () -> new AlchemyRecipe.Serializer(EARTH_ALCHEMY_TYPE));
    public static final RegistryObject<RecipeSerializer<?>> HEAVEN_ALCHEMY_SERIALIZER = SERIALIZERS.register("alchemy_heaven",
            () -> new AlchemyRecipe.Serializer(HEAVEN_ALCHEMY_TYPE));

    public static RecipeType<AlchemyRecipe> typeForTier(int tier) {
        return switch (tier) {
            case 2 -> SPIRIT_ALCHEMY_TYPE;
            case 3 -> EARTH_ALCHEMY_TYPE;
            case 4 -> HEAVEN_ALCHEMY_TYPE;
            default -> ALCHEMY_TYPE;
        };
    }

    private XiuxianRecipes() {}

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
