package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/** A clipped, top-down clearing cursor. The caller must refresh the chunk after done. */
final class SiteClearance {
    record Region(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        Region(int minX,int minY,int minZ,int maxX,int maxZ) {
            this(minX,minY,minZ,maxX,Integer.MAX_VALUE,maxZ);
        }
        int top(ServerLevel level,BlockPos origin) {
            return maxY==Integer.MAX_VALUE?level.getMaxBuildHeight()-1:origin.getY()+maxY;
        }
    }
    record Result(long cell, int writes, boolean done, String problem) {}

    static Result clear(ServerLevel level, LevelChunk chunk, int minX, int minY, int minZ,
                        int maxX, int maxY, int maxZ, long cell, int cellsLimit, int writesLimit, long budgetNs) {
        minX = Math.max(minX, chunk.getPos().getMinBlockX());
        maxX = Math.min(maxX, chunk.getPos().getMaxBlockX());
        minZ = Math.max(minZ, chunk.getPos().getMinBlockZ());
        maxZ = Math.min(maxZ, chunk.getPos().getMaxBlockZ());
        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);
        if (minX > maxX || minZ > maxZ || minY > maxY) return new Result(0, 0, true, "");
        int xs = maxX - minX + 1, zs = maxZ - minZ + 1;
        long layer = (long) xs * zs, total = layer * (maxY - minY + 1);
        if (cell < 0 || cell > total) return new Result(cell, 0, false, "Invalid clearing cursor");
        int writes = 0;
        long started = System.nanoTime();
        var pos = new BlockPos.MutableBlockPos();
        for (int scanned = 0; cell < total && scanned < cellsLimit && writes < writesLimit; scanned++) {
            if (scanned > 0 && System.nanoTime() - started >= budgetNs) break;
            int y = maxY - (int) (cell / layer);
            if (chunk.getSection(level.getSectionIndex(y)).hasOnlyAir()) {
                int nextY = Math.max(minY, (y >> 4) << 4) - 1;
                cell = Math.min(total, (long) (maxY - nextY) * layer);
                continue;
            }
            pos.set(minX + (int) (cell % xs), y, minZ + (int) (cell / xs % zs));
            if (!level.getWorldBorder().isWithinBounds(pos))
                return new Result(cell, writes, false, "Clearing crossed the world border");
            if (!chunk.getBlockState(pos).isAir()) {
                var entity = level.getBlockEntity(pos);
                if (entity != null) { Clearable.tryClear(entity); level.removeBlockEntity(pos); }
                if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE))
                    return new Result(cell, writes, false, "Clearing block write failed");
                writes++;
            }
            cell++;
        }
        return new Result(cell, writes, cell == total, "");
    }

    static void clearBox(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int z = minZ >> 4; z <= maxZ >> 4; z++) for (int x = minX >> 4; x <= maxX >> 4; x++) {
            var chunk = level.getChunk(x, z);
            long cursor = 0;
            while (true) {
                Result result = clear(level, chunk, minX, minY, minZ, maxX, maxY, maxZ,
                        cursor, 32768, 4096, 8_000_000L);
                if (!result.problem().isEmpty()) throw new IllegalStateException(result.problem());
                if (result.done()) break;
                cursor = result.cell();
            }
        }
    }
}
