package xiuxian.sect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

import xiuxian.sect.DaotaiResidenceGenerator.Placement;
import xiuxian.sect.DaotaiResidenceGenerator.Plan;

/** Bounded, restart-safe chunk construction for the large Dao-Tai landmark. */
final class DaotaiResidenceConstruction {
    private static final int CELLS_PER_TICK = 32768;
    private static final int WRITES_PER_TICK = 3072;
    private static final long BUDGET_NS = 8_000_000L;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("xiuxian_daotai_residence",
            (a, b) -> Long.compare(a.toLong(), b.toLong()));
    private static final WeakHashMap<ServerLevel, Job> JOBS = new WeakHashMap<>();

    private DaotaiResidenceConstruction() {}

    static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data::load, Data::new, "xiuxian_daotai_residences");
    }

    static boolean start(ServerLevel level, BlockPos origin, Plan plan) {
        Data data = data(level);
        if (data.origin != null || data.contains(origin)) return false;
        data.origin = origin.immutable();
        data.phase = Phase.CLEAR;
        data.version = blueprintVersion(level);
        data.chunk = data.operation = 0;
        data.cell = data.writes = 0;
        data.problem = "";
        data.setDirty();
        JOBS.put(level, new Job(origin, plan));
        return true;
    }

    static String status(ServerLevel level) {
        Data data = data(level);
        if (data.origin == null) return data.origins.isEmpty() ? "No active Dao-Tai residence construction."
                : "Dao-Tai residence complete at " + BlockPos.of(data.origins.get(data.origins.size() - 1)).toShortString()
                        + ". Use /xiuxian daotai " + (level.dimension() == LuoxiaInnerDimension.LEVEL ? "inner " : "")
                        + "visit entrance or visit view.";
        Job job = JOBS.get(level);
        String total = job == null ? "" : "/" + job.chunks.size();
        return "Dao-Tai residence " + data.phase + " chunk " + data.chunk + total
                + ", changed " + data.writes + " blocks."
                + (data.problem.isEmpty() ? "" : " PAUSED: " + data.problem);
    }

    static void tick(ServerLevel level) {
        Data data = data(level);
        if (data.origin == null || !data.problem.isEmpty()) { release(level); return; }
        // Older cursors used different clearing coverage and must restart.
        boolean legacyPlan = level.dimension() == LuoxiaInnerDimension.LEVEL
                ? data.version == 1102 || data.version == 1202 || data.version == 1203
                : data.version == 2 || data.version == 3;
        if (legacyPlan) {
            data.phase = Phase.CLEAR;
            data.version = blueprintVersion(level);
            data.chunk = data.operation = 0;
            data.cell = 0;
            data.setDirty();
            release(level);
        }
        if (data.version != blueprintVersion(level) || !data.validCursor()) {
            pause(level, data, "construction save is incompatible with the current blueprint");
            return;
        }
        Job job = JOBS.computeIfAbsent(level, ignored -> new Job(data.origin,
                level.dimension() == LuoxiaInnerDimension.LEVEL
                        ? LuoxiaDaotaiResidence.createPlan() : DaotaiResidenceGenerator.createPlan()));
        if (!job.origin.equals(data.origin) || data.chunk > job.chunks.size()) {
            pause(level, data, "construction cursor exceeds the reserved site");
            return;
        }
        if (data.chunk == job.chunks.size()) {
            job.release(level);
            if (data.phase != Phase.BUILD) {
                data.phase = data.phase == Phase.CLEAR ? Phase.CLEAR_VERIFY : Phase.BUILD;
                data.chunk = data.operation = 0; data.cell = 0;
                data.setDirty();
                return;
            }
            data.origins.add(data.origin.asLong());
            data.origin = null;
            JOBS.remove(level);
            data.setDirty();
            announce(level, "Dao-Tai residence construction complete. Use /xiuxian daotai "
                    + (level.dimension() == LuoxiaInnerDimension.LEVEL ? "inner " : "") + "status to inspect it.");
            return;
        }
        ChunkPos chunk = job.chunks.get(data.chunk);
        LevelChunk loaded = job.load(level, chunk);
        if (loaded == null) return;
        if (data.phase != Phase.BUILD) {
            var result = SiteClearance.clear(level, loaded,
                    data.origin.getX() + DaotaiResidenceGenerator.MIN_X, data.origin.getY() + 1,
                    data.origin.getZ() + DaotaiResidenceGenerator.MIN_Z,
                    data.origin.getX() + DaotaiResidenceGenerator.MAX_X, level.getMaxBuildHeight() - 1,
                    data.origin.getZ() + DaotaiResidenceGenerator.MAX_Z,
                    data.cell, CELLS_PER_TICK, WRITES_PER_TICK, BUDGET_NS);
            data.cell = result.cell(); data.writes += result.writes();
            if (!result.problem().isEmpty()) { pause(level, data, result.problem()); return; }
            if (result.done()) nextChunk(data);
            data.setDirty();
            return;
        }
        int writes = 0;
        long started = System.nanoTime();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cells = 0; cells < CELLS_PER_TICK && writes < WRITES_PER_TICK; cells++) {
            if (cells > 0 && System.nanoTime() - started >= BUDGET_NS) break;
            if (data.operation > job.operations.size()) {
                pause(level, data, "construction operation cursor is invalid");
                return;
            }
            if (data.operation == job.operations.size()) { nextChunk(data); break; }
            Placement op = job.operations.get(data.operation);
            int minX = Math.max(chunk.getMinBlockX(), data.origin.getX() + op.minX());
            int maxX = Math.min(chunk.getMaxBlockX(), data.origin.getX() + op.maxX());
            int minZ = Math.max(chunk.getMinBlockZ(), data.origin.getZ() + op.minZ());
            int maxZ = Math.min(chunk.getMaxBlockZ(), data.origin.getZ() + op.maxZ());
            int minY = data.origin.getY() + op.minY(), maxY = data.origin.getY() + op.maxY();
            if (minX > maxX || minZ > maxZ || minY > maxY) {
                data.cell = 0;
                data.operation++;
                continue;
            }
            int xs = maxX - minX + 1, zs = maxZ - minZ + 1;
            long layer = (long) xs * zs, total = layer * (maxY - minY + 1);
            if (data.cell > total) {
                pause(level, data, "construction cell cursor is invalid");
                return;
            }
            if (data.cell == total) {
                data.cell = 0;
                data.operation++;
                continue;
            }
            int y = minY + (int) (data.cell / layer);
            pos.set(minX + (int) (data.cell % xs), y,
                    minZ + (int) (data.cell / xs % zs));
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                pause(level, data, "a placement crossed the world border or build height");
                return;
            }
            if (!level.getBlockState(pos).equals(op.state())) {
                var entity = level.getBlockEntity(pos);
                if (entity != null) {
                    Clearable.tryClear(entity);
                    level.removeBlockEntity(pos);
                }
                if (!level.setBlock(pos, op.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                    pause(level, data, "a block write failed");
                    return;
                }
                data.writes++;
                writes++;
            }
            data.cell++;
        }
        data.setDirty();
        if (level.getGameTime() % 100 == 0) level.players().forEach(player ->
                player.displayClientMessage(Component.literal(status(level)), true));
    }

    static void cancel(ServerLevel level) {
        Data data = data(level);
        data.origin = null;
        data.problem = "";
        data.setDirty();
        release(level);
    }

    static void release(ServerLevel level) {
        Job job = JOBS.remove(level);
        if (job != null) job.release(level);
    }

    private static int blueprintVersion(ServerLevel level) {
        return level.dimension() == LuoxiaInnerDimension.LEVEL
                ? 1000 + LuoxiaDaotaiResidence.VERSION * 100 + DaotaiResidenceGenerator.VERSION
                : DaotaiResidenceGenerator.VERSION;
    }

    private static void nextChunk(Data data) { data.chunk++; data.operation = 0; data.cell = 0; }

    private static void pause(ServerLevel level, Data data, String problem) {
        data.problem = problem;
        data.setDirty();
        release(level);
        announce(level, "Dao-Tai residence paused: " + problem);
    }

    private static void announce(ServerLevel level, String message) {
        level.players().forEach(player -> player.sendSystemMessage(Component.literal(message)));
    }

    enum Phase { CLEAR, CLEAR_VERIFY, BUILD }

    static final class Data extends SavedData {
        final List<Long> origins = new ArrayList<>();
        BlockPos origin;
        Phase phase = Phase.CLEAR;
        int version = DaotaiResidenceGenerator.VERSION, chunk, operation;
        long cell, writes;
        String problem = "";

        boolean contains(BlockPos pos) { return origins.contains(pos.asLong()); }

        boolean validCursor() {
            return origin == null || chunk >= 0 && operation >= 0 && cell >= 0 && writes >= 0;
        }

        static Data load(CompoundTag tag) {
            Data data = new Data();
            for (long old : tag.getLongArray("Origins")) data.origins.add(old);
            if (tag.contains("ActiveOrigin")) {
                data.origin = BlockPos.of(tag.getLong("ActiveOrigin"));
                data.version = tag.getInt("Version");
                data.chunk = tag.getInt("Chunk");
                data.operation = tag.getInt("Operation");
                data.cell = tag.getLong("Cell");
                data.writes = tag.getLong("Writes");
                data.problem = tag.getString("Problem");
                try { data.phase = Phase.valueOf(tag.getString("Phase")); }
                catch (IllegalArgumentException invalid) { data.problem = "invalid saved construction phase"; }
            }
            return data;
        }

        @Override public CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Origins", origins);
            if (origin != null) {
                tag.putLong("ActiveOrigin", origin.asLong());
                tag.putString("Phase", phase.name());
                tag.putInt("Version", version);
                tag.putInt("Chunk", chunk);
                tag.putInt("Operation", operation);
                tag.putLong("Cell", cell);
                tag.putLong("Writes", writes);
                tag.putString("Problem", problem);
            } else tag.remove("ActiveOrigin");
            return tag;
        }
    }

    static final class Job {
        final BlockPos origin;
        final Plan plan;
        final List<ChunkPos> chunks = new ArrayList<>();
        List<Placement> operations = List.of();
        ChunkPos workChunk;

        Job(BlockPos origin, Plan plan) {
            this.origin = origin;
            this.plan = plan;
            var touched = new HashSet<ChunkPos>();
            addChunks(touched, new Placement(DaotaiResidenceGenerator.MIN_X, 1, DaotaiResidenceGenerator.MIN_Z,
                    DaotaiResidenceGenerator.MAX_X, DaotaiResidenceGenerator.MAX_Y,
                    DaotaiResidenceGenerator.MAX_Z, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
            for (Placement op : plan.placements) addChunks(touched, op);
            chunks.addAll(touched);
            chunks.sort(Comparator.comparingInt((ChunkPos pos) -> pos.z).thenComparingInt(pos -> pos.x));
        }

        private void addChunks(HashSet<ChunkPos> touched, Placement op) {
            for (int z = (origin.getZ() + op.minZ() >> 4); z <= (origin.getZ() + op.maxZ() >> 4); z++)
                for (int x = (origin.getX() + op.minX() >> 4); x <= (origin.getX() + op.maxX() >> 4); x++)
                    touched.add(new ChunkPos(x, z));
        }

        LevelChunk load(ServerLevel level, ChunkPos chunk) {
            if (!chunk.equals(workChunk)) {
                release(level);
                workChunk = chunk;
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
}
