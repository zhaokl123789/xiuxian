package xiuxian.sect;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;
import xiuxian.sect.JindanResidenceGenerator.Plan;

/** Unique, resumable transplant of the accepted furnace palace into the western court. */
public final class LuoxiaJindanResidence {
    static final int VERSION = 1;
    public static final BlockPos ORIGIN = new BlockPos(-360, 112, 220);
    static final int APPROACH_LENGTH = 181;

    private LuoxiaJindanResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if (level.dimension() != LuoxiaInnerDimension.LEVEL) return;
        var construction = JindanResidenceConstruction.data(level);
        if (!construction.contains(ORIGIN) && construction.origin == null)
            JindanResidenceConstruction.start(level, ORIGIN, createPlan());
        var realm = LuoxiaInnerRealmData.get(level);
        Map<String, BlockPos> markers = Map.of(
                "jindan_residence", ORIGIN.offset(92, 10, 0),
                "jindan_core_furnace", ORIGIN.offset(4, 18, 4),
                "jindan_future_gate", ORIGIN.offset(121, 10, 0),
                "jindan_entrance", ORIGIN.offset(177, 8, 0),
                "jindan_approach", new BlockPos(0, 73, 160));
        markers.forEach((id, pos) -> {
            if (!pos.equals(realm.markers.get(id))) {
                realm.marker(id, pos);
                realm.setDirty();
            }
        });
    }

    public static boolean isReady(ServerLevel level) {
        return level != null && level.dimension() == LuoxiaInnerDimension.LEVEL
                && JindanResidenceConstruction.data(level).contains(ORIGIN);
    }

    static Plan createPlan() {
        Plan plan = new Plan();
        plan.beginSection("legacy_approach", "Legacy approach clearance");
        // Retire only the former sparse road's floor, lamps and headroom.
        append(plan, -4, 73, 160, -4, 76, 160, Blocks.AIR.defaultBlockState());
        for (int step = 1; step <= 100; step++) {
            int x = Math.round(-360.0F * step / 100), y = 72 + Math.round(40.0F * step / 100);
            int z = 160 + Math.round(60.0F * step / 100);
            append(plan, x - 2, y, z, x + 2, y + 2, z, Blocks.AIR.defaultBlockState());
            if (step % 10 == 0) append(plan, x - 5, y + 1, z - 1, x - 3, y + 6, z + 1,
                    Blocks.AIR.defaultBlockState());
        }
        plan.finishSection();
        Plan accepted = JindanResidenceGenerator.createPlan();
        for (var section : accepted.sections) {
            plan.beginSection(section.id(), section.label());
            plan.placements.addAll(accepted.placements.subList(section.firstPlacement(), section.lastPlacement()));
            plan.finishSection();
        }
        plan.beginSection("approach", "Inner realm approach");
        BlockState jade = OrientalBlocks.state("jindan_white_jade");
        for (int distance = 0; distance <= APPROACH_LENGTH; distance++) {
            int x = -distance, z = approachZ(distance), y = approachHeight(distance);
            append(plan, x - 2, y - 1, z - 2, x + 2, y, z + 2, jade);
        }
        // Open the passage after all adjacent floor columns have been written.
        for (int distance = 0; distance <= APPROACH_LENGTH; distance++) {
            int x = -distance, z = approachZ(distance), y = approachHeight(distance);
            append(plan, x, y + 1, z - 1, x, y + 4, z + 1, Blocks.AIR.defaultBlockState());
            if (distance < APPROACH_LENGTH && approachHeight(distance + 1) > y)
                append(plan, x, y + 1, z - 1, x, y + 1, z + 1,
                        Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            if (distance > 8 && distance % 12 == 0) {
                append(plan, x, y + 1, z - 2, x, y + 1, z - 2, OrientalBlocks.state("jindan_copper_lantern"));
                append(plan, x, y + 1, z + 2, x, y + 1, z + 2, OrientalBlocks.state("jindan_copper_lantern"));
            }
        }
        // Join the causeway to the east hall's floor without changing its furnishings.
        for (int relativeX = 178; relativeX >= 122; relativeX--) {
            int y = ORIGIN.getY() + 7 + Math.min(2, (178 - relativeX) / 20);
            int x = ORIGIN.getX() + relativeX;
            append(plan, x, y - 1, 217, x, y, 223, jade);
            append(plan, x, y + 1, 218, x, y + 4, 222, Blocks.AIR.defaultBlockState());
            if (relativeX == 159 || relativeX == 139)
                append(plan, x, y + 1, 219, x, y + 1, 221,
                        Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        }
        plan.finishSection();
        return plan;
    }

    static int approachHeight(int distance) { return 72 + distance * 47 / APPROACH_LENGTH; }
    static int approachZ(int distance) { return 160 + Math.min(60, distance); }

    private static void append(Plan plan, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        // Extras are world-positioned; only the palace footprint gets full-height clearance.
        plan.placements.add(new JindanResidenceGenerator.Placement(x1 - ORIGIN.getX(), y1 - ORIGIN.getY(),
                z1 - ORIGIN.getZ(), x2 - ORIGIN.getX(), y2 - ORIGIN.getY(), z2 - ORIGIN.getZ(), state));
    }
}
