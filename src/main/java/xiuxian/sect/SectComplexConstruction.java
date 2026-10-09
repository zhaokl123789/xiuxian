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

/** Persistent bounded construction, using the same top-down site clearing as accepted residences. */
final class SectComplexConstruction {
    private static final int CELLS_PER_TICK = 32768, WRITES_PER_TICK = 4096;
    private static final long BUDGET_NS = 8_000_000L;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("xiuxian_sect_complex",
            (a,b) -> Long.compare(a.toLong(),b.toLong()));
    private static final WeakHashMap<ServerLevel,Job> JOBS = new WeakHashMap<>();
    enum Phase { CLEAR, CLEAR_VERIFY, BUILD }

    private SectComplexConstruction() {}
    static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data::load,Data::new,"xiuxian_sect_complexes");
    }
    static boolean start(ServerLevel level,BlockPos origin,SectComplexGenerator.Plan plan) {
        if(!validPlan(level,origin,plan))return false;
        var d=data(level);
        if(d.origin!=null||d.contains(origin))return false;
        d.origin=origin.immutable();d.version=blueprintVersion(level);d.phase=Phase.CLEAR;
        d.chunk=d.operation=0;d.cell=d.writes=0;d.problem="";
        d.setDirty();JOBS.put(level,new Job(origin,plan));return true;
    }
    static String status(ServerLevel level) {
        var d=data(level);
        if(d.origin==null)return d.origins.isEmpty()?"尚未建造完整宗门。":"完整宗门已完成："+BlockPos.of(d.origins.get(d.origins.size()-1)).toShortString();
        var j=JOBS.get(level);
        return "宗门 "+d.phase+" 区块 "+d.chunk+(j==null?"":"/"+j.chunks.size())+"，已修改 "+d.writes+" 方块。"
                +(d.problem.isEmpty()?"":" 已暂停："+d.problem);
    }
    static void tick(ServerLevel level) {
        var d=data(level);
        if(d.origin==null||!d.problem.isEmpty()){release(level);return;}
        if(d.version!=blueprintVersion(level)||d.chunk<0||d.operation<0||d.cell<0||d.writes<0
                ||!SectComplexGenerator.validSite(level,d.origin)) {
            pause(level,d,"施工存档或场地边界不合法，请检查后重试。");return;
        }
        var job=JOBS.computeIfAbsent(level,ignored->new Job(d.origin,plan(level)));
        if(d.phase==Phase.BUILD&&level.dimension()==LuoxiaInnerDimension.LEVEL&&!LuoxiaInnerRealmLayout.canBuild(level))return;
        if(!job.validated) {
            if(!validPlan(level,d.origin,job.plan)){pause(level,d,"Construction extends beyond world bounds");return;}
            job.validated=true;
        }
        if(d.chunk>job.chunks.size()||!job.origin.equals(d.origin)){pause(level,d,"施工区块游标不合法。");return;}
        if(d.phase==Phase.BUILD) {
            int before=d.chunk;
            while(d.chunk<job.chunks.size()&&!job.placementChunks.contains(job.chunks.get(d.chunk)))next(d);
            if(d.chunk!=before)d.setDirty();
        }
        if(d.chunk==job.chunks.size()) {
            job.release(level);
            if(d.phase==Phase.BUILD) {
                d.origins.add(d.origin.asLong());d.origin=null;JOBS.remove(level);
                if(level.dimension()==LuoxiaInnerDimension.LEVEL) {
                    d.origins.remove(Long.valueOf(LuoxiaSectComplexResidence.LEGACY_ORIGIN.asLong()));
                    LuoxiaInnerRealmLayout.finishMigration(level);
                }
                if(level.dimension()==LuoxiaInnerDimension.LEVEL)
                    announce(level,"洞天宗门建筑群已完工，可用 /xiuxian sect inner visit entrance 或 visit view 验收。");
                else
                announce(level,"完整宗门建筑群施工完成，可使用 /xiuxian sect visit entrance 或 visit view 验收。");
            }else {d.phase=d.phase==Phase.CLEAR?Phase.CLEAR_VERIFY:Phase.BUILD;d.chunk=d.operation=0;d.cell=0;}
            d.setDirty();return;
        }
        var chunk=job.chunks.get(d.chunk);
        var loaded=job.load(level,chunk);
        if(loaded==null)return;
        if(d.phase!=Phase.BUILD) {
            if(d.operation>job.clearances.size()){pause(level,d,"Invalid clearing region cursor");return;}
            if(d.operation==job.clearances.size()){next(d);d.setDirty();return;}
            var region=job.clearances.get(d.operation);
            var result=SiteClearance.clear(level,loaded,d.origin.getX()+region.minX(),
                    d.origin.getY()+region.minY(),d.origin.getZ()+region.minZ(),
                    d.origin.getX()+region.maxX(),region.top(level,d.origin),
                    d.origin.getZ()+region.maxZ(),d.cell,CELLS_PER_TICK,WRITES_PER_TICK,BUDGET_NS);
            d.cell=result.cell();d.writes+=result.writes();
            if(!result.problem().isEmpty()){pause(level,d,result.problem());return;}
            if(result.done()){d.operation++;d.cell=0;}
        }else {
            int writes=0;long began=System.nanoTime();var pos=new BlockPos.MutableBlockPos();
            for(int scanned=0;scanned<CELLS_PER_TICK&&writes<WRITES_PER_TICK;scanned++) {
                if(scanned>0&&System.nanoTime()-began>=BUDGET_NS)break;
                if(d.operation>job.operations.size()){pause(level,d,"施工操作游标不合法。");return;}
                if(d.operation==job.operations.size()){next(d);break;}
                var op=job.operations.get(d.operation);
                int x1=Math.max(chunk.getMinBlockX(),d.origin.getX()+op.minX());
                int x2=Math.min(chunk.getMaxBlockX(),d.origin.getX()+op.maxX());
                int z1=Math.max(chunk.getMinBlockZ(),d.origin.getZ()+op.minZ());
                int z2=Math.min(chunk.getMaxBlockZ(),d.origin.getZ()+op.maxZ());
                int y1=d.origin.getY()+op.minY(),y2=d.origin.getY()+op.maxY();
                int xs=x2-x1+1,zs=z2-z1+1;long layer=(long)xs*zs,total=layer*(y2-y1+1);
                if(d.cell>total){pause(level,d,"施工方块游标不合法。");return;}
                if(d.cell==total){d.operation++;d.cell=0;continue;}
                pos.set(x1+(int)(d.cell%xs),y1+(int)(d.cell/layer),z1+(int)(d.cell/xs%zs));
                if(!level.getWorldBorder().isWithinBounds(pos)){pause(level,d,"Placement crossed the world border");return;}
                if(!level.getBlockState(pos).equals(op.state())) {
                    var entity=level.getBlockEntity(pos);
                    if(entity!=null){Clearable.tryClear(entity);level.removeBlockEntity(pos);}
                    if(!level.setBlock(pos,op.state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)) {
                        pause(level,d,"方块写入失败。");return;
                    }
                    writes++;d.writes++;
                }
                d.cell++;
            }
        }
        d.setDirty();
        if(level.getGameTime()%100==0)level.players().forEach(p->p.displayClientMessage(Component.literal(status(level)),true));
    }
    private static void next(Data d){d.chunk++;d.operation=0;d.cell=0;}
    private static void pause(ServerLevel level,Data d,String problem){d.problem=problem;d.setDirty();release(level);announce(level,problem);}
    private static void announce(ServerLevel level,String message){level.players().forEach(p->p.sendSystemMessage(Component.literal(message)));}
    static void release(ServerLevel level){var job=JOBS.remove(level);if(job!=null)job.release(level);}
    static void cancel(ServerLevel level){var d=data(level);d.origin=null;d.problem="";d.setDirty();release(level);}
    static BlockPos latest(ServerLevel level){var d=data(level);return d.origins.isEmpty()?null:BlockPos.of(d.origins.get(d.origins.size()-1));}
    static boolean retryInner(ServerLevel level) {
        if(level.dimension()!=LuoxiaInnerDimension.LEVEL)return false;
        var d=data(level);var origin=d.origin;
        if(origin==null||d.problem.isEmpty()||d.contains(origin))return false;
        var plan=LuoxiaSectComplexResidence.createPlan();
        if(!validPlan(level,origin,plan))return false;
        release(level);d.origin=null;
        return start(level,origin,plan);
    }

    static final class Data extends SavedData {
        final List<Long> origins=new ArrayList<>();
        BlockPos origin;
        Phase phase=Phase.CLEAR;
        int version=SectComplexGenerator.VERSION,chunk,operation;
        long cell,writes;
        String problem="";
        boolean contains(BlockPos pos){return origins.contains(pos.asLong());}
        static Data load(CompoundTag tag) {
            var d=new Data();for(long value:tag.getLongArray("Origins"))d.origins.add(value);
            if(tag.contains("ActiveOrigin")) {
                d.origin=BlockPos.of(tag.getLong("ActiveOrigin"));
                try{d.phase=Phase.valueOf(tag.getString("Phase"));}catch(IllegalArgumentException e){d.problem="Unknown saved construction phase";}
                d.version=tag.getInt("Version");d.chunk=tag.getInt("Chunk");d.operation=tag.getInt("Operation");
                d.cell=tag.getLong("Cell");d.writes=tag.getLong("Writes");
                if(d.problem.isEmpty())d.problem=tag.getString("Problem");
            }
            return d;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Origins",origins);
            if(origin!=null) {
                tag.putLong("ActiveOrigin",origin.asLong());tag.putString("Phase",phase.name());tag.putInt("Version",version);
                tag.putInt("Chunk",chunk);tag.putInt("Operation",operation);tag.putLong("Cell",cell);tag.putLong("Writes",writes);tag.putString("Problem",problem);
            }else tag.remove("ActiveOrigin");
            return tag;
        }
    }
    private static int blueprintVersion(ServerLevel level) {
        return level.dimension()==LuoxiaInnerDimension.LEVEL
                ? 1000 + LuoxiaSectComplexResidence.VERSION * 100 + SectComplexGenerator.VERSION
                : SectComplexGenerator.VERSION;
    }

    private static boolean validPlan(ServerLevel level,BlockPos origin,SectComplexGenerator.Plan plan) {
        if(!SectComplexGenerator.validSite(level,origin))return false;
        if(level.dimension()==LuoxiaInnerDimension.LEVEL&&!origin.equals(LuoxiaSectComplexResidence.ORIGIN))return false;
        for(var region:plan.clearances)for(var pos:List.of(origin.offset(region.minX(),region.minY(),region.minZ()),
                origin.offset(region.maxX(),region.minY(),region.maxZ())))
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        for(var op:plan.placements)for(var pos:List.of(origin.offset(op.minX(),op.minY(),op.minZ()),origin.offset(op.maxX(),op.maxY(),op.maxZ())))
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        return true;
    }

    private static SectComplexGenerator.Plan plan(ServerLevel level) {
        return level.dimension()==LuoxiaInnerDimension.LEVEL
                ? LuoxiaSectComplexResidence.createPlan() : SectComplexGenerator.createPlan();
    }

    static final class Job {
        final BlockPos origin;
        final SectComplexGenerator.Plan plan;
        final List<ChunkPos> chunks=new ArrayList<>();
        final java.util.Set<ChunkPos> placementChunks=new HashSet<>();
        List<SectComplexGenerator.Placement> operations=List.of();
        List<SiteClearance.Region> clearances=List.of();
        ChunkPos workChunk;
        boolean validated;
        Job(BlockPos origin,SectComplexGenerator.Plan plan) {
            this.origin=origin;this.plan=plan;
            var touched=new HashSet<ChunkPos>();
            for(var region:plan.clearances)addChunks(touched,origin,region.minX(),region.minZ(),region.maxX(),region.maxZ());
            for(var op:plan.placements)addChunks(placementChunks,origin,op.minX(),op.minZ(),op.maxX(),op.maxZ());
            touched.addAll(placementChunks);
            chunks.addAll(touched);
            chunks.sort(Comparator.comparingInt((ChunkPos c)->c.z).thenComparingInt(c->c.x));
        }
        LevelChunk load(ServerLevel level,ChunkPos chunk) {
            if(!chunk.equals(workChunk)) {
                release(level);workChunk=chunk;level.getChunkSource().addRegionTicket(TICKET,chunk,0,chunk);
                operations=plan.placements.stream().filter(op->op.minX()+origin.getX()<=chunk.getMaxBlockX()
                        &&op.maxX()+origin.getX()>=chunk.getMinBlockX()&&op.minZ()+origin.getZ()<=chunk.getMaxBlockZ()
                        &&op.maxZ()+origin.getZ()>=chunk.getMinBlockZ()).toList();
                clearances=plan.clearances.stream().filter(r->r.minX()+origin.getX()<=chunk.getMaxBlockX()
                        &&r.maxX()+origin.getX()>=chunk.getMinBlockX()&&r.minZ()+origin.getZ()<=chunk.getMaxBlockZ()
                        &&r.maxZ()+origin.getZ()>=chunk.getMinBlockZ()).toList();
            }
            return level.getChunkSource().getChunkNow(chunk.x,chunk.z);
        }
        private static void addChunks(java.util.Set<ChunkPos> chunks,BlockPos origin,int minX,int minZ,int maxX,int maxZ) {
            for(int z=(origin.getZ()+minZ)>>4;z<=(origin.getZ()+maxZ)>>4;z++)
                for(int x=(origin.getX()+minX)>>4;x<=(origin.getX()+maxX)>>4;x++)chunks.add(new ChunkPos(x,z));
        }
        void release(ServerLevel level){if(workChunk!=null){level.getChunkSource().removeRegionTicket(TICKET,workChunk,0,workChunk);workChunk=null;}}
    }
}
