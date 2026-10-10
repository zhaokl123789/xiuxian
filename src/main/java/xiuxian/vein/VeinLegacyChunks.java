package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.sect.LuoxiaInnerDimension;

/** Legacy saved flat generators opt in only when a chunk is first created. */
@Mod.EventBusSubscriber(modid="xiuxian")
public final class VeinLegacyChunks {
    private VeinLegacyChunks() {}
    public static boolean eligible(boolean newlyCreated, boolean modern, int chunkX,int chunkZ) {
        if(!newlyCreated || modern) return false;
        int x=chunkX*16,z=chunkZ*16;
        return !(x<=60 && x+15>=-60 && z<=390 && z+15>=120);
    }
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level) || !LuoxiaInnerDimension.isInner(level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        var generator=level.getChunkSource().getGenerator();
        if(!(generator instanceof FlatLevelSource) || !eligible(event.isNewChunk(),generator instanceof VeinChunkGenerator,chunk.getPos().x,chunk.getPos().z)) return;
        // Chunk load fires before promotion to FULL; defer interactions to the server task queue.
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount(), () -> {
                new VeinTerrain(VeinChunkGenerator.terrainSeed(level.getChunkSource().randomState())).fill(chunk,true);
                var cursor=new BlockPos.MutableBlockPos();
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=-63;y<=60;y++) {
                    cursor.set(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
                    level.getChunkSource().getLightEngine().checkBlock(cursor);
                }
        }));
    }
}
