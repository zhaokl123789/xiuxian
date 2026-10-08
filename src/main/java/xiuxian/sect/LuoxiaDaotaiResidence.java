package xiuxian.sect;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;
import xiuxian.sect.DaotaiResidenceGenerator.Placement;
import xiuxian.sect.DaotaiResidenceGenerator.Plan;

/** Independent, once-per-realm transplant of the approved cloud palace. */
public final class LuoxiaDaotaiResidence {
    static final int VERSION = 2;
    public static final BlockPos ORIGIN = new BlockPos(0, 80, -500);

    private LuoxiaDaotaiResidence() {}

    static void ensureGenerated(ServerLevel level) {
        if (level.dimension() != LuoxiaInnerDimension.LEVEL) return;
        var construction = DaotaiResidenceConstruction.data(level);
        if (!construction.contains(ORIGIN) && construction.origin == null) {
            DaotaiResidenceConstruction.start(level, ORIGIN, createPlan());
        }
        var realm = LuoxiaInnerRealmData.get(level);
        Map<String, BlockPos> markers = Map.of(
                "daotai_residence", ORIGIN.offset(0, 75, -75),
                "daotai_star_pool", ORIGIN.offset(112, 75, -128),
                "daotai_approach", new BlockPos(0, 72, -12),
                "daotai_entrance", ORIGIN.offset(0, 2, 209),
                "daotai_upper", ORIGIN.offset(0, 144, -92));
        markers.forEach((id, pos) -> {
            if (!pos.equals(realm.markers.get(id))) {
                realm.marker(id, pos);
                realm.setDirty();
            }
        });
    }

    public static boolean isReady(ServerLevel level) {
        return level != null && level.dimension() == LuoxiaInnerDimension.LEVEL
                && DaotaiResidenceConstruction.data(level).contains(ORIGIN);
    }

    static Plan createPlan() {
        Plan plan = DaotaiResidenceGenerator.createPlan();
        // Retire the former landmark and its approach before placing the new palace.
        int firstClear = plan.placements.size();
        append(plan, -70, 226, 340, 70, 310, 520, Blocks.AIR.defaultBlockState());
        for (int step = 3; step <= 171; step++) {
            int y = 72 + step, z = 160 + Math.round(step * 270.0F / 171.0F);
            append(plan, -3, y, z, 3, y + 2, z, Blocks.AIR.defaultBlockState());
            if (step % 8 == 0) for (int x : new int[] {-6, 6})
                append(plan, x, y + 1, z, x, y + 4, z, Blocks.AIR.defaultBlockState());
        }
        var cleanup = java.util.List.copyOf(plan.placements.subList(firstClear, plan.placements.size()));
        plan.placements.subList(firstClear, plan.placements.size()).clear();
        plan.placements.addAll(0, cleanup);
        // Open the north side of the arrival pavilion, then climb nine blocks
        // over 290 metres to the unchanged southern entrance of the palace.
        BlockState jade = OrientalBlocks.state("town_white_jade");
        for (int distance = 0; distance < 290; distance++) {
            int z = -distance, y = approachHeight(distance);
            append(plan, -3, y - 1, z, 3, y, z, jade);
            append(plan, -3, y + 1, z, 3, y + 6, z, Blocks.AIR.defaultBlockState());
            if (approachHeight(distance + 1) > y) {
                append(plan, -3, y + 1, z, 3, y + 1, z,
                        Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            }
            if (distance >= 12) for (int x : new int[] {-4, 4}) {
                append(plan, x, y, z, x, y, z, jade);
                append(plan, x, y + 1, z, x, y + 1, z, OrientalBlocks.state("town_jade_railing"));
                if (distance % 12 == 0) {
                    append(plan, x, y + 2, z, x, y + 2, z, OrientalBlocks.state("daotai_lamp_01"));
                }
            }
        }
        return plan;
    }

    static int approachHeight(int distance) { return 71 + Math.min(9, distance * 9 / 290); }

    private static void append(Plan plan, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        Placement op = new Placement(x1 - ORIGIN.getX(), y1 - ORIGIN.getY(), z1 - ORIGIN.getZ(),
                x2 - ORIGIN.getX(), y2 - ORIGIN.getY(), z2 - ORIGIN.getZ(), state);
        plan.placements.add(op);
        plan.minX = Math.min(plan.minX, op.minX()); plan.maxX = Math.max(plan.maxX, op.maxX());
        plan.minY = Math.min(plan.minY, op.minY()); plan.maxY = Math.max(plan.maxY, op.maxY());
        plan.minZ = Math.min(plan.minZ, op.minZ()); plan.maxZ = Math.max(plan.maxZ, op.maxZ());
    }
}
