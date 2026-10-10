package xiuxian.vein;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class VeinWorldgen {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, "xiuxian");
    public static final RegistryObject<Codec<? extends ChunkGenerator>> VEIN = GENERATORS.register("nine_layer_vein", () -> VeinChunkGenerator.CODEC);
    private VeinWorldgen() {}
    public static void register(IEventBus bus) { GENERATORS.register(bus); }
}
