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

/** Restart-safe, chunk-bounded construction for Overworld and inner-realm Jin-Dan palaces. */
final class JindanResidenceConstruction {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("xiuxian_jindan_residence",
            (a, b) -> Long.compare(a.toLong(), b.toLong()));
    private static final WeakHashMap<ServerLevel, Job> JOBS = new WeakHashMap<>();
    private static final int CELLS_PER_TICK = 32768;
    private static final int WRITES_PER_TICK = 4096;
    private static final long BUDGET_NS = 8_000_000L;

    private JindanResidenceConstruction() {}

    static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data::load, Data::new, "xiuxian_jindan_residences");
    }

    static boolean start(ServerLevel level, BlockPos origin, JindanResidenceGenerator.Plan plan) {
        Data d = data(level);
        if (d.origin != null || d.contains(origin)) return false;
        d.origin = origin.immutable();
        d.phase = Phase.CLEAR;
        d.section = 0;
        d.version = blueprintVersion(level);
        d.chunk = d.operation = 0;
        d.cell = d.writes = 0;
        d.problem = "";
        d.setDirty();
        JOBS.put(level, new Job(origin, plan));
        return true;
    }

    static Data active(ServerLevel level) {
        return data(level);
    }

    static String status(ServerLevel level) {
        Data d = data(level);
        if (d.origin == null) {
            return d.origins.isEmpty() ? "没有正在进行的金丹居所施工。"
                    : "金丹居所已完成，位置：" + BlockPos.of(d.origins.get(d.origins.size() - 1)).toShortString() + "。";
        }
        Job job = JOBS.get(level);
        String total = job == null ? "" : "/" + job.chunks.size();
        String label = d.phase == Phase.CLEAR ? "场地清理"
                : d.phase == Phase.CLEAR_VERIFY ? "清理复扫"
                : job == null || d.section >= job.plan.sections.size() ? "施工"
                : job.plan.sections.get(d.section).label();
        return "金丹居所【" + label + "】 " + d.phase + " 区块 " + d.chunk + total
                + "，已写入 " + d.writes + " 方块。" + (d.problem.isEmpty() ? "" : " 已暂停：" + d.problem);
    }

    static void tick(ServerLevel level) {
        Data d = data(level);
        if (d.origin == null || !d.problem.isEmpty()) {
            release(level);
            return;
        }
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD && (d.version == 4 || d.version == 5)) {
            d.version = blueprintVersion(level);
            d.phase = Phase.CLEAR;
            d.section = d.chunk = d.operation = 0;
            d.cell = 0;
            d.setDirty();
            release(level);
        }
        if (d.version != blueprintVersion(level) || d.chunk < 0 || d.section < 0
                || d.operation < 0 || d.cell < 0 || d.writes < 0) {
            pause(level, d, "施工存档与当前金丹蓝图不兼容，请取消后重新选址");
            return;
        }
        Job job = JOBS.computeIfAbsent(level, ignored -> new Job(d.origin,
                level.dimension() == LuoxiaInnerDimension.LEVEL
                        ? LuoxiaJindanResidence.createPlan() : JindanResidenceGenerator.createPlan()));
        if(d.phase==Phase.BUILD&&level.dimension()==LuoxiaInnerDimension.LEVEL&&!LuoxiaInnerRealmLayout.canBuild(level))return;
        if (!job.origin.equals(d.origin) || (d.phase == Phase.BUILD && d.section >= job.plan.sections.size())) {
            pause(level, d, "施工游标超出当前金丹蓝图");
            return;
        }
        if(d.phase==Phase.BUILD) {
            int before=d.chunk;
            var touched=job.sectionChunks.get(d.section);
            while(d.chunk<job.chunks.size()&&!touched.contains(job.chunks.get(d.chunk)))nextChunk(d);
            if(d.chunk!=before)d.setDirty();
        }
        // A section may contain no placements in several outer chunks. Those
        // chunks still advance the shared site cursor; normalize both the
        // exact boundary and a stale +1 cursor from an older tick/save.
        if (d.chunk >= job.chunks.size()) {
            job.release(level);
            if (d.phase != Phase.BUILD) {
                d.phase = d.phase == Phase.CLEAR ? Phase.CLEAR_VERIFY : Phase.BUILD;
                d.section = d.chunk = d.operation = 0; d.cell = 0;
            } else if (d.section + 1 < job.plan.sections.size()) {
                announce(level, "金丹居所：【" + job.plan.sections.get(d.section).label() + "】完成。");
                d.section++;
                d.chunk = d.operation = 0;
                d.cell = 0;
                announce(level, "金丹居所：开始施工【" + job.plan.sections.get(d.section).label() + "】。");
            } else {
                announce(level, "金丹居所：【" + job.plan.sections.get(d.section).label() + "】完成。");
                d.origins.add(d.origin.asLong());
                if(level.dimension()==LuoxiaInnerDimension.LEVEL)
                    d.origins.remove(Long.valueOf(LuoxiaJindanResidence.LEGACY_ORIGIN.asLong()));
                d.origin = null;
                if(level.dimension()==LuoxiaInnerDimension.LEVEL)LuoxiaInnerRealmLayout.finishMigration(level);
                JOBS.remove(level);
                announce(level, "金丹居所施工完成。使用 /xiuxian jindan "
                        + (level.dimension() == LuoxiaInnerDimension.LEVEL ? "inner " : "") + "status 查看。");
            }
            d.setDirty();
            return;
        }

        ChunkPos chunk = job.chunks.get(d.chunk);
        LevelChunk loaded = job.load(level, chunk, d.phase, d.section);
        if (loaded == null) return;
        if (d.phase != Phase.BUILD) {
            if(d.operation>job.clearances.size()){pause(level,d,"Invalid clearance region cursor");return;}
            if(d.operation==job.clearances.size()){nextChunk(d);d.setDirty();return;}
            var region=job.clearances.get(d.operation);
            var result = SiteClearance.clear(level, loaded,
                    d.origin.getX() + region.minX(), d.origin.getY() + region.minY(),
                    d.origin.getZ() + region.minZ(),
                    d.origin.getX() + region.maxX(), region.top(level,d.origin),
                    d.origin.getZ() + region.maxZ(), d.cell, CELLS_PER_TICK, WRITES_PER_TICK, BUDGET_NS);
            d.cell = result.cell(); d.writes += result.writes();
            if (!result.problem().isEmpty()) { pause(level, d, result.problem()); return; }
            if(result.done()){d.operation++;d.cell=0;}
            d.setDirty();
            return;
        }
        int writes = 0;
        long started = System.nanoTime();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cells = 0; cells < CELLS_PER_TICK && writes < WRITES_PER_TICK; cells++) {
            if (cells > 0 && System.nanoTime() - started >= BUDGET_NS) break;
            if (d.operation >= job.operations.size()) {
                nextChunk(d);
                break;
            }
            JindanResidenceGenerator.Placement op = job.operations.get(d.operation);
            int minX = Math.max(chunk.getMinBlockX(), d.origin.getX() + op.minX());
            int maxX = Math.min(chunk.getMaxBlockX(), d.origin.getX() + op.maxX());
            int minZ = Math.max(chunk.getMinBlockZ(), d.origin.getZ() + op.minZ());
            int maxZ = Math.min(chunk.getMaxBlockZ(), d.origin.getZ() + op.maxZ());
            int minY = d.origin.getY() + op.minY();
            int maxY = d.origin.getY() + op.maxY();
            if (minX > maxX || minZ > maxZ || minY > maxY) {
                advance(d);
                continue;
            }
            long xs = maxX - minX + 1L;
            long zs = maxZ - minZ + 1L;
            long layer = xs * zs;
            long total = layer * (maxY - minY + 1L);
            // A cursor can point past a clipped operation after a restart or a
            // blueprint revision. Treat it as completed instead of deadlocking
            // the entire site behind a generic failure message.
            if (d.cell > total) {
                advance(d);
                continue;
            }
            if (d.cell == total) {
                advance(d);
                continue;
            }
            int y = minY + (int) (d.cell / layer);
            pos.set(minX + (int) (d.cell % xs), y, minZ + (int) (d.cell / xs % zs));
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                pause(level, d, "Placement exceeds world border or build height");
                return;
            }
            if (!level.getBlockState(pos).equals(op.state())) {
                var entity = level.getBlockEntity(pos);
                if (entity != null) {
                    Clearable.tryClear(entity);
                    level.removeBlockEntity(pos);
                }
                if (!level.setBlock(pos, op.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                    pause(level, d, "方块写入失败，施工已暂停");
                    return;
                }
                d.writes++;
                writes++;
            }
            d.cell++;
        }
        d.setDirty();
        if (level.getGameTime() % 100 == 0) {
            level.players().forEach(player -> player.displayClientMessage(Component.literal(status(level)), true));
        }
    }

    private static void advance(Data d) {
        d.cell = 0;
        d.operation++;
    }

    private static void nextChunk(Data d) {
        d.chunk++;
        d.operation = 0;
        d.cell = 0;
    }

    private static void pause(ServerLevel level, Data d, String problem) {
        d.problem = problem;
        d.setDirty();
        release(level);
        announce(level, "金丹居所施工暂停：" + problem);
    }

    private static void announce(ServerLevel level, String message) {
        level.players().forEach(player -> player.sendSystemMessage(Component.literal(message)));
    }

    static void cancel(ServerLevel level) {
        Data d = data(level);
        d.origin = null;
        d.problem = "";
        d.setDirty();
        release(level);
    }

    static void release(ServerLevel level) {
        Job job = JOBS.remove(level);
        if (job != null) job.release(level);
    }

    enum Phase { CLEAR, CLEAR_VERIFY, BUILD }

    private static int blueprintVersion(ServerLevel level) {
        return level.dimension() == LuoxiaInnerDimension.LEVEL
                ? 1000 + LuoxiaJindanResidence.VERSION * 100 + JindanResidenceGenerator.VERSION
                : JindanResidenceGenerator.VERSION;
    }

    static final class Data extends SavedData {
        final List<Long> origins = new ArrayList<>();
        BlockPos origin;
        Phase phase = Phase.CLEAR;
        int version = JindanResidenceGenerator.VERSION;
        int section, chunk, operation;
        long cell, writes;
        String problem = "";

        boolean contains(BlockPos pos) { return origins.contains(pos.asLong()); }

        static Data load(CompoundTag tag) {
            Data d = new Data();
            for (long p : tag.getLongArray("Origins")) d.origins.add(p);
            if (tag.contains("ActiveOrigin")) {
                d.origin = BlockPos.of(tag.getLong("ActiveOrigin"));
                String phase = tag.getString("Phase");
                d.phase = "BUILD".equals(phase) ? Phase.BUILD
                        : "CLEAR_VERIFY".equals(phase) ? Phase.CLEAR_VERIFY : Phase.CLEAR;
                d.version = tag.getInt("Version");
                d.section = tag.getInt("Section");
                d.chunk = tag.getInt("Chunk");
                d.operation = tag.getInt("Operation");
                d.cell = tag.getLong("Cell");
                d.writes = tag.getLong("Writes");
                d.problem = tag.getString("Problem");
            }
            return d;
        }

        @Override public CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Origins", origins);
            if (origin != null) {
                tag.putLong("ActiveOrigin", origin.asLong());
                tag.putString("Phase", phase.name());
                tag.putInt("Version", version);
                tag.putInt("Section", section);
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
        final JindanResidenceGenerator.Plan plan;
        final List<ChunkPos> chunks = new ArrayList<>();
        final List<HashSet<ChunkPos>> sectionChunks = new ArrayList<>();
        List<JindanResidenceGenerator.Placement> operations = List.of();
        List<SiteClearance.Region> clearances = List.of();
        ChunkPos workChunk;
        int workSection=-1;

        Job(BlockPos origin, JindanResidenceGenerator.Plan plan) {
            this.origin = origin;
            this.plan = plan;
            var touched = new HashSet<ChunkPos>();
            for(var region:plan.clearances)add(touched,new JindanResidenceGenerator.Placement(region.minX(),region.minY(),region.minZ(),
                    region.maxX(),region.minY(),region.maxZ(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
            for (var op : plan.placements) add(touched, op);
            for(var section:plan.sections) {
                var part=new HashSet<ChunkPos>();
                for(var op:plan.placements.subList(section.firstPlacement(),section.lastPlacement()))add(part,op);
                sectionChunks.add(part);
            }
            chunks.addAll(touched);
            chunks.sort(Comparator.comparingInt((ChunkPos c) -> c.z).thenComparingInt(c -> c.x));
        }

        private void add(HashSet<ChunkPos> set, JindanResidenceGenerator.Placement op) {
            for (int z = (origin.getZ() + op.minZ()) >> 4; z <= (origin.getZ() + op.maxZ()) >> 4; z++) {
                for (int x = (origin.getX() + op.minX()) >> 4; x <= (origin.getX() + op.maxX()) >> 4; x++) {
                    set.add(new ChunkPos(x, z));
                }
            }
        }

        LevelChunk load(ServerLevel level, ChunkPos chunk, Phase phase, int section) {
            boolean moved=!chunk.equals(workChunk);
            if (moved) {
                release(level);
                workChunk = chunk;
                level.getChunkSource().addRegionTicket(TICKET, chunk, 0, chunk);
                clearances=plan.clearances.stream().filter(r->r.minX()+origin.getX()<=chunk.getMaxBlockX()
                        &&r.maxX()+origin.getX()>=chunk.getMinBlockX()&&r.minZ()+origin.getZ()<=chunk.getMaxBlockZ()
                        &&r.maxZ()+origin.getZ()>=chunk.getMinBlockZ()).toList();
            }
            if (phase == Phase.BUILD&&(moved||workSection!=section)) {
                workSection=section;
                var part = plan.sections.get(section);
                operations = plan.placements.subList(part.firstPlacement(), part.lastPlacement()).stream()
                        .filter(op -> intersects(op, chunk)).toList();
            } else if(phase!=Phase.BUILD) {
                operations = List.of();
            }
            return level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        }

        private boolean intersects(JindanResidenceGenerator.Placement op, ChunkPos chunk) {
            return op.minX() + origin.getX() <= chunk.getMaxBlockX()
                    && op.maxX() + origin.getX() >= chunk.getMinBlockX()
                    && op.minZ() + origin.getZ() <= chunk.getMaxBlockZ()
                    && op.maxZ() + origin.getZ() >= chunk.getMinBlockZ();
        }

        void release(ServerLevel level) {
            if (workChunk != null) {
                level.getChunkSource().removeRegionTicket(TICKET, workChunk, 0, workChunk);
                workChunk = null;
            }
        }
    }
}
