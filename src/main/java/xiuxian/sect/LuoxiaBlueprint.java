package xiuxian.sect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ordered, compact volumes; the construction job clips them to one chunk at a time. */
public final class LuoxiaBlueprint {
    public static final int VERSION = 1;
    public static final int MIN_X = -160, MAX_X = 160;
    public static final int MIN_Z = -272, MAX_Z = 112;
    public static final int MIN_Y = -12, MAX_Y = 222;
    private final List<Placement> placements = new ArrayList<>();
    private final Map<Long, List<Placement>> columns = new HashMap<>();

    public record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }

    public static LuoxiaBlueprint create() {
        LuoxiaBlueprint result = new LuoxiaBlueprint();
        LuoxiaArchitecture.addTo(result);
        result.addAscent();
        LuoxiaLandscape.addTo(result);
        return result;
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        if (minX < MIN_X || maxX > MAX_X || minY < MIN_Y || maxY > MAX_Y
                || minZ < MIN_Z || maxZ > MAX_Z) {
            throw new IllegalArgumentException("Luoxia placement outside reserved bounds: "
                    + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ);
        }
        placements.add(new Placement(minX, minY, minZ, maxX, maxY, maxZ, state));
        columns.clear();
    }

    public void block(int x, int y, int z, BlockState state) {
        fill(x, y, z, x, y, z, state);
    }

    public void hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, int thickness, BlockState state) {
        fill(x1, y1, z1, x2, y1 + thickness - 1, z2, state);
        fill(x1, y2 - thickness + 1, z1, x2, y2, z2, state);
        fill(x1, y1, z1, x1 + thickness - 1, y2, z2, state);
        fill(x2 - thickness + 1, y1, z1, x2, y2, z2, state);
        fill(x1, y1, z1, x2, y2, z1 + thickness - 1, state);
        fill(x1, y1, z2 - thickness + 1, x2, y2, z2, state);
    }

    public List<Placement> placements() {
        return List.copyOf(placements);
    }

    public List<Placement> placementsAt(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        return columns.computeIfAbsent(key, ignored -> placements.stream().filter(p ->
                x >= p.minX && x <= p.maxX && z >= p.minZ && z <= p.maxZ).toList());
    }

    public BlockState sampleArchitecture(int x, int y, int z) {
        List<Placement> ops = placementsAt(x, z);
        for (int i = ops.size() - 1; i >= 0; i--) {
            Placement op = ops.get(i);
            if (y >= op.minY && y <= op.maxY) {
                return op.state;
            }
        }
        return null;
    }

    private void addAscent() {
        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int z = 112; z >= -162; z--) {
            int height = LuoxiaTerrain.pathHeight(z);
            int nextHeight = LuoxiaTerrain.pathHeight(z - 1);
            boolean incline = z >= 80 || z >= 10 && z <= 40 || z >= -78 && z <= -42
                    || z >= -154 && z <= -106;
            if (!incline && !(z > 0 && z < 10)) continue;
            fill(-9, height - 2, z, 9, height, z, Blocks.STONE_BRICKS.defaultBlockState());
            fill(-8, height + 1, z, 8, height + 4, z, Blocks.AIR.defaultBlockState());
            if (nextHeight > height) {
                fill(-8, height + 1, z, 8, height + 1, z, stair);
            } else {
                fill(-8, height, z, 8, height, z, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            if (incline) {
                block(-9, height + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                block(9, height + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
            }
            if (z % 8 == 0) {
                for (int x : new int[] {-10, 10}) {
                    fill(x, height, z, x, height + 2, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                    block(x, height + 3, z, Blocks.LANTERN.defaultBlockState());
                }
            }
        }
        // A one-block podium rise joins the summit ascent to the hall's south porch.
        fill(-8, 133, -163, 8, 133, -163, stair);
    }
}
