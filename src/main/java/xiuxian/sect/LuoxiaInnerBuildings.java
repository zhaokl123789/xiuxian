package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Materialize initial landmarks during world loading, before players can enter. */
final class LuoxiaInnerBuildings {
    static final int VERSION = 1;

    private record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}
    private record Site(BlockPos origin, List<SiteClearance.Region> clearances, List<Placement> placements,
                        Runnable complete) {}

    private LuoxiaInnerBuildings() {}

    static void ensureGenerated(ServerLevel level) {
        var realm=LuoxiaInnerRealmData.get(level);
        if(realm.initialBuildingsVersion>=VERSION)return;
        var sect=SectComplexConstruction.data(level);
        var dao=DaotaiResidenceConstruction.data(level);
        var jin=JindanResidenceConstruction.data(level);
        // Retired/custom sites still use the existing, restart-safe migration.
        if(hasOtherSite(sect.origins,sect.origin,LuoxiaSectComplexResidence.ORIGIN)
                ||hasOtherSite(dao.origins,dao.origin,LuoxiaDaotaiResidence.ORIGIN)
                ||hasOtherSite(jin.origins,jin.origin,LuoxiaJindanResidence.ORIGIN)) {
            realm.initialBuildingsVersion=VERSION;
            realm.setDirty();
            return;
        }
        var sites=new ArrayList<Site>();
        if(!sect.contains(LuoxiaSectComplexResidence.ORIGIN)) {
            var plan=realm.generated?LuoxiaSectComplexResidence.createPlan():LuoxiaSectComplexResidence.createInitialPlan();
            sites.add(new Site(LuoxiaSectComplexResidence.ORIGIN,plan.clearances,
                    plan.placements.stream().map(p->new Placement(p.minX(),p.minY(),p.minZ(),p.maxX(),p.maxY(),p.maxZ(),p.state())).toList(),
                    ()->{SectComplexConstruction.cancel(level);sect.origins.add(LuoxiaSectComplexResidence.ORIGIN.asLong());sect.setDirty();}));
        }
        if(!dao.contains(LuoxiaDaotaiResidence.ORIGIN)) {
            var plan=realm.generated?LuoxiaDaotaiResidence.createPlan():LuoxiaDaotaiResidence.createInitialPlan();
            sites.add(new Site(LuoxiaDaotaiResidence.ORIGIN,plan.clearances,
                    plan.placements.stream().map(p->new Placement(p.minX(),p.minY(),p.minZ(),p.maxX(),p.maxY(),p.maxZ(),p.state())).toList(),
                    ()->{DaotaiResidenceConstruction.cancel(level);dao.origins.add(LuoxiaDaotaiResidence.ORIGIN.asLong());dao.setDirty();}));
        }
        if(!jin.contains(LuoxiaJindanResidence.ORIGIN)) {
            var plan=realm.generated?LuoxiaJindanResidence.createPlan():LuoxiaJindanResidence.createInitialPlan();
            sites.add(new Site(LuoxiaJindanResidence.ORIGIN,plan.clearances,
                    plan.placements.stream().map(p->new Placement(p.minX(),p.minY(),p.minZ(),p.maxX(),p.maxY(),p.maxZ(),p.state())).toList(),
                    ()->{JindanResidenceConstruction.cancel(level);jin.origins.add(LuoxiaJindanResidence.ORIGIN.asLong());jin.setDirty();}));
        }
        // Validate all sites before any writes, then clear all footprints before placing roads/halls.
        for(var site:sites)validate(level,site);
        long began=System.nanoTime();
        for(var site:sites)for(var region:site.clearances) {
            var origin=site.origin;
            SiteClearance.clearBox(level,origin.getX()+region.minX(),origin.getY()+region.minY(),origin.getZ()+region.minZ(),
                    origin.getX()+region.maxX(),region.top(level,origin),origin.getZ()+region.maxZ());
        }
        for(var site:sites) {
            place(level,site);
            site.complete.run();
        }
        if(!sites.isEmpty())LuoxiaInnerRealmGenerator.restoreSurfaceHub(level);
        realm.routesVersion=1;
        realm.initialBuildingsVersion=VERSION;
        realm.setDirty();
        if(!sites.isEmpty())LogUtils.getLogger().info("Luoxia initial buildings ready: {} sites in {} ms",
                sites.size(),(System.nanoTime()-began)/1_000_000);
    }

    private static boolean hasOtherSite(List<Long> completed, BlockPos active, BlockPos origin) {
        return active!=null&&!origin.equals(active)||completed.stream().anyMatch(p->p!=origin.asLong());
    }

    private static void validate(ServerLevel level, Site site) {
        for(var region:site.clearances) {
            check(level,site.origin.offset(region.minX(),region.minY(),region.minZ()));
            check(level,new BlockPos(site.origin.getX()+region.maxX(),region.top(level,site.origin),site.origin.getZ()+region.maxZ()));
        }
        for(var p:site.placements) {
            check(level,site.origin.offset(p.minX,p.minY,p.minZ));
            check(level,site.origin.offset(p.maxX,p.maxY,p.maxZ));
        }
    }

    private static void check(ServerLevel level, BlockPos pos) {
        if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))
            throw new IllegalStateException("Initial Luoxia building exceeds world bounds at "+pos);
    }

    private static void place(ServerLevel level, Site site) {
        var pos=new BlockPos.MutableBlockPos();
        for(var p:site.placements) {
            int x1=site.origin.getX()+p.minX,x2=site.origin.getX()+p.maxX;
            int z1=site.origin.getZ()+p.minZ,z2=site.origin.getZ()+p.maxZ;
            for(int cz=z1>>4;cz<=z2>>4;cz++)for(int cx=x1>>4;cx<=x2>>4;cx++) {
                var chunk=level.getChunk(cx,cz);
                for(int y=site.origin.getY()+p.minY;y<=site.origin.getY()+p.maxY;y++)
                    for(int z=Math.max(z1,cz<<4);z<=Math.min(z2,(cz<<4)+15);z++)
                        for(int x=Math.max(x1,cx<<4);x<=Math.min(x2,(cx<<4)+15);x++) {
                            pos.set(x,y,z);
                            if(chunk.getBlockState(pos).equals(p.state))continue;
                            var entity=chunk.getBlockEntity(pos);
                            if(entity!=null){Clearable.tryClear(entity);level.removeBlockEntity(pos);}
                            if(!level.setBlock(pos,p.state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))
                                throw new IllegalStateException("Initial Luoxia building write failed at "+pos);
                        }
            }
        }
    }
}
