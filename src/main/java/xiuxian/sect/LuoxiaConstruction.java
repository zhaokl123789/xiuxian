package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** One loaded work chunk and a persisted cursor; no world-sized block queue. */
final class LuoxiaConstruction {
    private static final int MAX_CELLS_PER_TICK = 16_384;
    private static final int MAX_WRITES_PER_TICK = 3_072;
    private static final long TICK_BUDGET_NS = 8_000_000L;
    private static final WeakHashMap<ServerLevel, Job> JOBS = new WeakHashMap<>();

    private LuoxiaConstruction() {}

    static void tick(ServerLevel level) {
        LuoxiaSiteData data = LuoxiaSiteData.get(level);
        if (!data.active()) {
            release(level);
            return;
        }
        if (data.version != LuoxiaBlueprint.VERSION) {
            data.paused = true;
            data.problem = "建筑版本已变化，已暂停旧施工记录，避免错位续建。";
            data.setDirty();
            release(level);
            return;
        }
        if (!validCursorNumbers(data)) {
            pause(level, data, "施工游标无效，不能继续写入，请检查存档备份。");
            return;
        }
        Job job = JOBS.computeIfAbsent(level, ignored -> new Job(data.origin));
        if (!job.origin.equals(data.origin) || !job.validCursor(data)) {
            pause(level, data, "施工游标超出当前区块或建筑体积，已暂停，请检查存档数据。");
            return;
        }
        long started = System.nanoTime();
        int writes = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cells = 0; cells < MAX_CELLS_PER_TICK && writes < MAX_WRITES_PER_TICK; cells++) {
            if (cells > 0 && System.nanoTime() - started >= TICK_BUDGET_NS) break;
            if (data.chunkIndex >= job.chunks.size()) {
                job.release(level);
                data.nextPhase();
                if (data.phase == LuoxiaSiteData.Phase.COMPLETE) {
                    level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                            "落霞宗外部建造完成。可用 /xiuxian luoxia locate 定位山门。"), false);
                    JOBS.remove(level);
                    return;
                }
                continue;
            }
            ChunkPos chunk = job.chunks.get(data.chunkIndex);
            LevelChunk loaded = job.load(level, chunk);
            if (loaded == null) break;
            if (data.phase == LuoxiaSiteData.Phase.SURVEY) {
                if (!data.forceClearing) {
                    for (BlockPos protectedPos : loaded.getBlockEntitiesPos()) {
                        if (job.contains(protectedPos)) {
                            pause(level, data, "建造范围内有方块实体，已保护并暂停：" + coordinates(protectedPos)
                                    + "。潜行使用营建令可强制清场续建，或移走设施后 resume。");
                            return;
                        }
                    }
                }
                data.chunkIndex++;
                data.setDirty();
                // Only request one new work chunk per tick, even during the site survey.
                break;
            }
            BlockState desired;
            if (data.phase == LuoxiaSiteData.Phase.TERRAIN) {
                int minX = Math.max(job.minX, chunk.getMinBlockX());
                int maxX = Math.min(job.maxX, chunk.getMaxBlockX());
                int minZ = Math.max(job.minZ, chunk.getMinBlockZ());
                int maxZ = Math.min(job.maxZ, chunk.getMaxBlockZ());
                int ys = LuoxiaBlueprint.MAX_Y - LuoxiaBlueprint.MIN_Y + 1;
                long total = (long) (maxX - minX + 1) * (maxZ - minZ + 1) * ys;
                if (data.cellIndex >= total) {
                    data.chunkIndex++;
                    data.cellIndex = 0;
                    break;
                }
                long column = data.cellIndex / ys;
                int x = minX + (int) (column % (maxX - minX + 1));
                int z = minZ + (int) (column / (maxX - minX + 1));
                int y = data.origin.getY() + LuoxiaBlueprint.MIN_Y + (int) (data.cellIndex % ys);
                pos.set(x, y, z);
                if (job.lastX != x || job.lastZ != z) {
                    job.lastX = x;
                    job.lastZ = z;
                    job.lastHeight = LuoxiaTerrain.heightAt(x - data.origin.getX(), z - data.origin.getZ());
                }
                desired = LuoxiaTerrain.blockAt(x - data.origin.getX(), y - data.origin.getY(),
                        z - data.origin.getZ(), job.lastHeight);
            } else {
                List<LuoxiaBlueprint.Placement> ops = job.operations(chunk,
                        data.phase == LuoxiaSiteData.Phase.WATER);
                if (data.operationIndex >= ops.size()) {
                    data.chunkIndex++;
                    data.operationIndex = 0;
                    data.cellIndex = 0;
                    break;
                }
                LuoxiaBlueprint.Placement op = ops.get(data.operationIndex);
                int minX = Math.max(chunk.getMinBlockX(), op.minX() + data.origin.getX());
                int maxX = Math.min(chunk.getMaxBlockX(), op.maxX() + data.origin.getX());
                int minZ = Math.max(chunk.getMinBlockZ(), op.minZ() + data.origin.getZ());
                int maxZ = Math.min(chunk.getMaxBlockZ(), op.maxZ() + data.origin.getZ());
                int ys = op.maxY() - op.minY() + 1, xs = maxX - minX + 1;
                long total = (long) xs * ys * (maxZ - minZ + 1);
                if (data.cellIndex >= total) {
                    data.operationIndex++;
                    data.cellIndex = 0;
                    continue;
                }
                pos.set(minX + (int) (data.cellIndex % xs),
                        data.origin.getY() + op.minY() + (int) (data.cellIndex / xs % ys),
                        minZ + (int) (data.cellIndex / ((long) xs * ys)));
                desired = op.state();
            }
            if (!level.getBlockState(pos).equals(desired)) {
                if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                    pause(level, data, "施工位置超出建造高度或世界边界，已保留进度并暂停：" + coordinates(pos));
                    return;
                }
                BlockEntity obstacle = level.getBlockEntity(pos);
                if (obstacle != null) {
                    if (!data.forceClearing) {
                        pause(level, data, "施工位置出现方块实体，已暂停并保留：" + coordinates(pos)
                                + "。潜行使用营建令可强制清场续建，或移走设施后 resume。");
                        return;
                    }
                    // Remove inventories before block callbacks run, avoiding a flood of dropped items.
                    Clearable.tryClear(obstacle);
                    level.removeBlockEntity(pos);
                }
                if (!level.setBlock(pos, desired, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                    pause(level, data, "方块写入失败，已保留施工位置并暂停：" + coordinates(pos) + "。请检查场地后再继续。");
                    return;
                }
                writes++;
                data.changedBlocks++;
            }
            data.cellIndex++;
        }
        data.setDirty();
    }

    static void release(ServerLevel level) {
        Job job = JOBS.remove(level);
        if (job != null) job.release(level);
    }

    private static void pause(ServerLevel level, LuoxiaSiteData data, String problem) {
        data.paused = true;
        data.problem = problem;
        data.setDirty();
        release(level);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("落霞宗：" + problem), false);
    }

    static String coordinates(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    static int chunkCount(BlockPos origin) {
        return ((origin.getX() + LuoxiaBlueprint.MAX_X >> 4) - (origin.getX() + LuoxiaBlueprint.MIN_X >> 4) + 1)
                * ((origin.getZ() + LuoxiaBlueprint.MAX_Z >> 4) - (origin.getZ() + LuoxiaBlueprint.MIN_Z >> 4) + 1);
    }

    private static boolean validCursorNumbers(LuoxiaSiteData data) {
        return data.phase != null && data.origin != null && data.chunkIndex >= 0 && data.operationIndex >= 0
                && data.cellIndex >= 0 && data.changedBlocks >= 0 && data.chunkIndex <= chunkCount(data.origin);
    }

    static boolean validCursor(LuoxiaSiteData data) {
        if (!validCursorNumbers(data)) return false;
        if (data.phase == LuoxiaSiteData.Phase.BUILDINGS || data.phase == LuoxiaSiteData.Phase.WATER) {
            return new Job(data.origin).validCursor(data);
        }
        return validNonBuildingCursor(data);
    }

    private static boolean validNonBuildingCursor(LuoxiaSiteData data) {
        if (data.phase == LuoxiaSiteData.Phase.PLANNED || data.phase == LuoxiaSiteData.Phase.COMPLETE) {
            return data.chunkIndex == 0 && data.operationIndex == 0 && data.cellIndex == 0;
        }
        if (data.operationIndex != 0) return false;
        if (data.phase == LuoxiaSiteData.Phase.SURVEY || data.chunkIndex == chunkCount(data.origin)) {
            return data.cellIndex == 0;
        }
        int minChunkX = data.origin.getX() + LuoxiaBlueprint.MIN_X >> 4;
        int minChunkZ = data.origin.getZ() + LuoxiaBlueprint.MIN_Z >> 4;
        int chunkWidth = (data.origin.getX() + LuoxiaBlueprint.MAX_X >> 4) - minChunkX + 1;
        ChunkPos chunk = new ChunkPos(minChunkX + data.chunkIndex % chunkWidth, minChunkZ + data.chunkIndex / chunkWidth);
        int xs = Math.min(data.origin.getX() + LuoxiaBlueprint.MAX_X, chunk.getMaxBlockX())
                - Math.max(data.origin.getX() + LuoxiaBlueprint.MIN_X, chunk.getMinBlockX()) + 1;
        int zs = Math.min(data.origin.getZ() + LuoxiaBlueprint.MAX_Z, chunk.getMaxBlockZ())
                - Math.max(data.origin.getZ() + LuoxiaBlueprint.MIN_Z, chunk.getMinBlockZ()) + 1;
        return data.cellIndex <= (long) xs * zs * (LuoxiaBlueprint.MAX_Y - LuoxiaBlueprint.MIN_Y + 1);
    }

    private static final class Job {
        final BlockPos origin;
        final List<ChunkPos> chunks = new ArrayList<>();
        final List<LuoxiaBlueprint.Placement> allOperations;
        final int minX, maxX, minZ, maxZ;
        ChunkPos workChunk;
        ChunkPos operationChunk;
        LevelChunk loadedChunk;
        List<LuoxiaBlueprint.Placement> solids = List.of(), water = List.of();
        int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE, lastHeight;

        Job(BlockPos origin) {
            this.origin = origin;
            minX = origin.getX() + LuoxiaBlueprint.MIN_X;
            maxX = origin.getX() + LuoxiaBlueprint.MAX_X;
            minZ = origin.getZ() + LuoxiaBlueprint.MIN_Z;
            maxZ = origin.getZ() + LuoxiaBlueprint.MAX_Z;
            allOperations = LuoxiaBlueprint.create().placements();
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                for (int x = minX >> 4; x <= maxX >> 4; x++) chunks.add(new ChunkPos(x, z));
            }
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ
                    && pos.getY() >= origin.getY() + LuoxiaBlueprint.MIN_Y
                    && pos.getY() <= origin.getY() + LuoxiaBlueprint.MAX_Y;
        }

        LevelChunk load(ServerLevel level, ChunkPos chunk) {
            if (!chunk.equals(workChunk)) {
                release(level);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 0, chunk);
                workChunk = chunk;
            }
            prepareOperations(chunk);
            // Ticket-driven generation stays asynchronous; a cold FULL chunk never blocks a player tick.
            if (loadedChunk == null) loadedChunk = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
            return loadedChunk;
        }

        private void prepareOperations(ChunkPos chunk) {
            if (chunk.equals(operationChunk)) return;
            solids = allOperations.stream().filter(p -> intersects(p, chunk) && !p.state().is(Blocks.WATER)).toList();
            water = allOperations.stream().filter(p -> intersects(p, chunk) && p.state().is(Blocks.WATER)).toList();
            operationChunk = chunk;
        }

        boolean validCursor(LuoxiaSiteData data) {
            if (!validCursorNumbers(data) || !origin.equals(data.origin)) return false;
            if (data.phase != LuoxiaSiteData.Phase.BUILDINGS && data.phase != LuoxiaSiteData.Phase.WATER) {
                return validNonBuildingCursor(data);
            }
            if (data.chunkIndex == chunks.size()) return data.operationIndex == 0 && data.cellIndex == 0;
            ChunkPos chunk = chunks.get(data.chunkIndex);
            prepareOperations(chunk);
            List<LuoxiaBlueprint.Placement> ops = operations(chunk, data.phase == LuoxiaSiteData.Phase.WATER);
            if (data.operationIndex > ops.size()) return false;
            if (data.operationIndex == ops.size()) return data.cellIndex == 0;
            LuoxiaBlueprint.Placement op = ops.get(data.operationIndex);
            int xs = Math.min(chunk.getMaxBlockX(), op.maxX() + origin.getX())
                    - Math.max(chunk.getMinBlockX(), op.minX() + origin.getX()) + 1;
            int zs = Math.min(chunk.getMaxBlockZ(), op.maxZ() + origin.getZ())
                    - Math.max(chunk.getMinBlockZ(), op.minZ() + origin.getZ()) + 1;
            return data.cellIndex <= (long) xs * zs * (op.maxY() - op.minY() + 1);
        }

        private boolean intersects(LuoxiaBlueprint.Placement p, ChunkPos chunk) {
            return p.minX() + origin.getX() <= chunk.getMaxBlockX() && p.maxX() + origin.getX() >= chunk.getMinBlockX()
                    && p.minZ() + origin.getZ() <= chunk.getMaxBlockZ() && p.maxZ() + origin.getZ() >= chunk.getMinBlockZ();
        }

        List<LuoxiaBlueprint.Placement> operations(ChunkPos chunk, boolean fluidPhase) {
            return fluidPhase ? water : solids;
        }

        void release(ServerLevel level) {
            if (workChunk != null) level.getChunkSource().removeRegionTicket(TicketType.FORCED, workChunk, 0, workChunk);
            workChunk = null;
            loadedChunk = null;
        }
    }
}
