package xiuxian.cultivation;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TaixuFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, "xiuxian");

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FLOATING_ISLAND =
            FEATURES.register("taixu_island", () -> new TaixuIslandFeature(NoneFeatureConfiguration.CODEC));

    private TaixuFeatures() {}

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
