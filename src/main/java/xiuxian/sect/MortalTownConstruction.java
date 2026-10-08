package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import xiuxian.sect.MortalTownGenerator.Placement;
import xiuxian.sect.MortalTownGenerator.Plan;

/** Compact placements, one asynchronous work chunk, and a saveable construction cursor. */
final class MortalTownConstruction {
    private static final int VERSION = 5;
    private static final int CELLS_PER_TICK = 32768, WRITES_PER_TICK = 2048;
    private static final long BUDGET_NS = 6_000_000L;
    static final WeakHashMap<ServerLevel, Job> JOBS = new WeakHashMap<>();
    // Dedicated ticket type prevents town cleanup from removing the sect's ticket.
    private static final TicketType<ChunkPos> TICKET = TicketType.create("xiuxian_town",
            (a, b) -> Long.compare(a.toLong(), b.toLong()));

    static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data::load, Data::new, "xiuxian_mortal_town_sites");
    }

    static boolean start(ServerLevel level, BlockPos origin, Plan plan) {
        Data data = data(level);
        if (data.origin != null || data.contains(origin)) return false;
        data.origin = origin.immutable();
        data.version = VERSION;
        data.building = false;
        data.verifying = false;
        data.chunk = data.operation = 0;
        data.cell = data.writes = 0;
        data.problem = "";
        data.setDirty();
        JOBS.put(level, new Job(origin, plan));
        return true;
    }

    static String status(ServerLevel level) {
        Data data = data(level);
        if (data.origin == null) return "当前没有凡人城镇施工任务。";
        Job job = JOBS.get(level);
        String total = job == null ? "" : "/" + job.chunks.size();
        return (data.building ? "建造" : data.verifying ? "清场复扫" : "清场") + "：区块 " + data.chunk + total
                + "，已修改 " + data.writes + " 个方块。"
                + (data.problem.isEmpty() ? "" : " 已暂停：" + data.problem);
    }

    static void tick(ServerLevel level) {
        Data data = data(level);
        if (data.origin == null || !data.problem.isEmpty()) { release(level); return; }
        // Restart older cursor layouts with full clearance before construction.
        if (data.version == 3 || data.version == 4) {
            data.version = VERSION;
            data.building = false;
            data.verifying = false;
            data.chunk = data.operation = 0;
            data.cell = 0;
            data.setDirty();
            release(level);
        }
        if (data.version != VERSION) { pause(level, data, "施工版本已变化，请取消旧任务后重新选址。"); return; }
        Job job = JOBS.computeIfAbsent(level, ignored -> new Job(data.origin, MortalTownGenerator.createPlan()));
        if (!job.origin.equals(data.origin) || data.chunk < 0 || data.chunk > job.chunks.size()
                || data.operation < 0 || data.cell < 0) {
            pause(level, data, "施工进度无效，已停止写入。"); return;
        }
        if (data.chunk == job.chunks.size()) {
            job.release(level);
            if (!data.building) {
                if (data.verifying) data.building = true;
                else data.verifying = true;
                data.chunk = data.operation = 0; data.cell = 0;
                data.setDirty();
                return;
            }
            data.origins.add(data.origin.asLong()); data.origin = null;
            JOBS.remove(level);
            announce(level, "凡人城镇建造完成，可以验收。自制东方建筑方块已使用。");
            data.setDirty(); return;
        }
        ChunkPos chunk = job.chunks.get(data.chunk);
        LevelChunk loaded = job.load(level, chunk);
        if (loaded == null) return; // ticket drives cold-chunk generation without blocking use/tick
        if (!data.building) {
            var result = SiteClearance.clear(level, loaded,
                    data.origin.getX() + job.plan.minX, data.origin.getY() + 1, data.origin.getZ() + job.plan.minZ,
                    data.origin.getX() + job.plan.maxX, level.getMaxBuildHeight() - 1, data.origin.getZ() + job.plan.maxZ,
                    data.cell, CELLS_PER_TICK, WRITES_PER_TICK, BUDGET_NS);
            data.cell = result.cell(); data.writes += result.writes();
            if (!result.problem().isEmpty()) { pause(level, data, result.problem()); return; }
            if (result.done()) nextChunk(data);
            data.setDirty();
            return; // Never continue with the completed chunk's clipped bounds.
        }
        int writes = 0;
        long started = System.nanoTime();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cells = 0; cells < CELLS_PER_TICK && writes < WRITES_PER_TICK; cells++) {
            if (cells > 0 && System.nanoTime() - started >= BUDGET_NS) break;
            if (data.operation > job.operations.size()) {
                pause(level, data, "建造操作游标越界。"); return;
            }
            if (data.operation == job.operations.size()) { nextChunk(data); break; }
            Placement op = job.operations.get(data.operation);
            int minX = Math.max(chunk.getMinBlockX(), data.origin.getX() + op.minX());
            int maxX = Math.min(chunk.getMaxBlockX(), data.origin.getX() + op.maxX());
            int minZ = Math.max(chunk.getMinBlockZ(), data.origin.getZ() + op.minZ());
            int maxZ = Math.min(chunk.getMaxBlockZ(), data.origin.getZ() + op.maxZ());
            int minY = data.origin.getY() + op.minY(), maxY = data.origin.getY() + op.maxY();
            int xs = maxX - minX + 1, zs = maxZ - minZ + 1;
            long layer = (long) xs * zs, total = layer * (maxY - minY + 1);
            if (data.cell > total) { pause(level, data, "方块游标越界。"); return; }
            if (data.cell == total) {
                data.cell = 0;
                data.operation++;
                continue;
            }
            int y = minY + (int) (data.cell / layer);
            pos.set(minX + (int) (data.cell % xs), y,
                    minZ + (int) (data.cell / xs % zs));
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                pause(level, data, "施工位置超出当前世界边界或高度。"); return;
            }
            BlockState desired = op.state();
            if (!loaded.getBlockState(pos).equals(desired)) {
                var entity = level.getBlockEntity(pos);
                if (entity != null) {
                    Clearable.tryClear(entity); // suppress inventory item floods during deliberate clearance
                    level.removeBlockEntity(pos);
                }
                if (!level.setBlock(pos, desired, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                    pause(level, data, "方块写入失败，已保存进度。"); return;
                }
                data.writes++; writes++;
            }
            data.cell++;
        }
        data.setDirty();
        if (level.getGameTime() % 100 == 0) {
            level.players().forEach(player -> player.displayClientMessage(Component.literal(status(level)), true));
        }
    }

    static void cancel(ServerLevel level) {
        Data data = data(level);
        data.origin = null; data.problem = ""; data.setDirty(); release(level);
    }

    private static void nextChunk(Data data) { data.chunk++; data.operation = 0; data.cell = 0; }
    private static void pause(ServerLevel level, Data data, String problem) {
        data.problem = problem; data.setDirty(); release(level); announce(level, problem);
    }
    private static void announce(ServerLevel level, String message) {
        level.players().forEach(player -> player.sendSystemMessage(Component.literal(message)));
    }
    static void release(ServerLevel level) {
        Job job = JOBS.remove(level);
        if (job != null) job.release(level);
    }

    static final class Job {
        final BlockPos origin;
        final Plan plan;
        final List<ChunkPos> chunks = new ArrayList<>();
        List<Placement> operations = List.of();
        ChunkPos workChunk;

        Job(BlockPos origin, Plan plan) {
            this.origin = origin; this.plan = plan;
            for (int z = (origin.getZ() + plan.minZ) >> 4; z <= (origin.getZ() + plan.maxZ) >> 4; z++)
                for (int x = (origin.getX() + plan.minX) >> 4; x <= (origin.getX() + plan.maxX) >> 4; x++)
                    chunks.add(new ChunkPos(x,z));
        }
        LevelChunk load(ServerLevel level, ChunkPos chunk) {
            if (!chunk.equals(workChunk)) {
                release(level); workChunk = chunk;
                level.getChunkSource().addRegionTicket(TICKET, chunk, 0, chunk);
                operations = plan.placements.stream().filter(op ->
                        op.minX() + origin.getX() <= chunk.getMaxBlockX()
                        && op.maxX() + origin.getX() >= chunk.getMinBlockX()
                        && op.minZ() + origin.getZ() <= chunk.getMaxBlockZ()
                        && op.maxZ() + origin.getZ() >= chunk.getMinBlockZ()).toList();
            }
            return level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        }
        void release(ServerLevel level) {
            if (workChunk != null) level.getChunkSource().removeRegionTicket(TICKET, workChunk, 0, workChunk);
            workChunk = null;
        }
    }

    static final class Data extends SavedData {
        final List<Long> origins = new ArrayList<>();
        BlockPos origin;
        int version = VERSION, chunk, operation;
        long cell, writes;
        boolean building, verifying;
        String problem = "";

        boolean contains(BlockPos pos) { return origins.contains(pos.asLong()); }
        static Data load(CompoundTag tag) {
            Data data = new Data();
            for (long old : tag.getLongArray("Origins")) data.origins.add(old);
            if (tag.contains("ActiveOrigin")) {
                data.origin = BlockPos.of(tag.getLong("ActiveOrigin"));
                data.version = tag.getInt("Version"); data.chunk = tag.getInt("Chunk");
                data.operation = tag.getInt("Operation"); data.cell = tag.getLong("Cell");
                data.writes = tag.getLong("Writes"); data.building = tag.getBoolean("Building");
                data.verifying = tag.getBoolean("Verifying");
                data.problem = tag.getString("Problem");
            }
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Origins", origins);
            if (origin != null) {
                tag.putLong("ActiveOrigin", origin.asLong()); tag.putInt("Version",version);
                tag.putInt("Chunk",chunk); tag.putInt("Operation",operation); tag.putLong("Cell",cell);
                tag.putLong("Writes",writes); tag.putBoolean("Building",building); tag.putString("Problem",problem);
                tag.putBoolean("Verifying", verifying);
            } else tag.remove("ActiveOrigin");
            return tag;
        }
    }
}
