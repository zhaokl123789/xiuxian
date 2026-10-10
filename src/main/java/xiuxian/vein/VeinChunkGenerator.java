package xiuxian.vein;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.concurrent.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** Flat surface contract plus a chunk-local underground pass, before lighting and publication. */
public final class VeinChunkGenerator extends FlatLevelSource {
    private volatile VeinTerrain terrain;
    private VeinTerrain terrain(RandomState random) {
        if(terrain==null) synchronized(this) { if(terrain==null) terrain=new VeinTerrain(terrainSeed(random)); }
        return terrain;
    }
    public static final Codec<VeinChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(VeinChunkGenerator::settings)).apply(instance, VeinChunkGenerator::new));
    public VeinChunkGenerator(FlatLevelGeneratorSettings settings) { super(settings); }
    @Override protected Codec<? extends ChunkGenerator> codec() { return CODEC; }
    public static long terrainSeed(RandomState random) {
        return random.getOrCreateRandomFactory(new ResourceLocation("xiuxian", "nine_layer_vein")).at(0,0,0).nextLong();
    }
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return super.fillFromNoise(executor,blender,random,structures,chunk).thenApply(result -> {
            terrain(random).fill(result);
            return result;
        });
    }
    @Override public int getMinY() { return -64; }
}
