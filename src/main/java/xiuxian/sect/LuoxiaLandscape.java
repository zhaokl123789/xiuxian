package xiuxian.sect;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

final class LuoxiaLandscape {
    private LuoxiaLandscape() {}

    static void addTo(LuoxiaBlueprint b) {
        // The lake is cut only where the designed rock surface is below its waterline.
        for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
            int start = Integer.MIN_VALUE;
            for (int x = LuoxiaBlueprint.MIN_X; x <= LuoxiaBlueprint.MAX_X + 1; x++) {
                boolean lake = x > LuoxiaBlueprint.MIN_X && x < LuoxiaBlueprint.MAX_X
                        && z > LuoxiaBlueprint.MIN_Z && z < LuoxiaBlueprint.MAX_Z && LuoxiaTerrain.heightAt(x, z) <= -5;
                if (lake && start == Integer.MIN_VALUE) start = x;
                if (!lake && start != Integer.MIN_VALUE) {
                    b.fill(start, -4, z, x - 1, -1, z, Blocks.WATER.defaultBlockState());
                    start = Integer.MIN_VALUE;
                }
            }
        }
        // A submerged shoreline contains the lake even when testing above a superflat world.
        for (int x : new int[] {LuoxiaBlueprint.MIN_X, LuoxiaBlueprint.MAX_X}) {
            b.fill(x, -4, LuoxiaBlueprint.MIN_Z, x, -1, LuoxiaBlueprint.MAX_Z, Blocks.GRAVEL.defaultBlockState());
        }
        for (int z : new int[] {LuoxiaBlueprint.MIN_Z, LuoxiaBlueprint.MAX_Z}) {
            b.fill(LuoxiaBlueprint.MIN_X, -4, z, LuoxiaBlueprint.MAX_X, -1, z, Blocks.GRAVEL.defaultBlockState());
        }
        waterfall(b, -86, -105);
        waterfall(b, 91, -186);
        waterfall(b, -138, -107);
        waterfall(b, 141, -183);
        for (int z = -246; z <= 82; z += 12) {
            for (int x = -148; x <= 148; x += 15) {
                int h = Math.floorMod(x * 734287 + z * 912931, 997);
                int tx = x + h % 7 - 3, tz = z + h % 5 - 2;
                int y = LuoxiaTerrain.heightAt(tx, tz);
                if (h % 3 == 0 || y < 2 || y > 142 || !treeSite(tx, tz)) continue;
                tree(b, tx, y + 1, tz, 8 + h % 5, h % 7 == 0);
            }
        }
        // A sealed scenic arch reserves the future sect's inner-world threshold.
        for (int x : new int[] {-8, 8}) {
            b.fill(x - 1, 133, -220, x + 1, 145, -218, Blocks.QUARTZ_PILLAR.defaultBlockState());
            b.fill(x - 2, 145, -221, x + 2, 146, -217, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
            b.block(x, 147, -219, Blocks.LANTERN.defaultBlockState());
        }
        b.fill(-10, 147, -220, 10, 148, -218, Blocks.DEEPSLATE_TILES.defaultBlockState());
        b.fill(-9, 149, -220, 9, 149, -218, Blocks.CUT_COPPER.defaultBlockState());
        b.fill(-6, 133, -220, 6, 133, -218, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        b.fill(-6, 134, -220, 6, 142, -220, Blocks.TINTED_GLASS.defaultBlockState());
        b.fill(-2, 134, -221, 2, 135, -221, Blocks.AMETHYST_BLOCK.defaultBlockState());
    }

    private static boolean treeSite(int x, int z) {
        if (Math.abs(x) < 88 && z >= -228 && z <= 86) return false;
        if (LuoxiaTerrain.inside(x, z, -133, -91, -126, -82)) return false;
        if (LuoxiaTerrain.inside(x, z, 96, 144, -204, -157)) return false;
        if (z >= -114 && z <= -97 && x >= -133 && x <= -68) return false;
        if (z >= -191 && z <= -169 && x >= 72 && x <= 144) return false;
        return true;
    }

    private static void tree(LuoxiaBlueprint b, int x, int y, int z, int height, boolean autumn) {
        BlockState leaves = (autumn ? Blocks.AZALEA_LEAVES : Blocks.SPRUCE_LEAVES)
                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        b.fill(x, y, z, x, y + height, z, Blocks.SPRUCE_LOG.defaultBlockState());
        for (int tier = 0; tier < 4; tier++) {
            int radius = 4 - tier, ly = y + height - 7 + tier * 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int depth = radius - Math.abs(dx) / 2;
                b.fill(x + dx, ly, z - depth, x + dx, ly, z + depth, leaves);
            }
        }
        b.block(x, y + height + 1, z, leaves);
        b.fill(x, y, z, x, y + height - 1, z, Blocks.SPRUCE_LOG.defaultBlockState());
    }

    private static void waterfall(LuoxiaBlueprint b, int x, int z) {
        int top = LuoxiaTerrain.heightAt(x, z);
        BlockState falling = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
        b.fill(x - 3, -5, z - 2, x + 3, top, z, Blocks.CALCITE.defaultBlockState());
        b.fill(x - 3, -5, z + 1, x + 3, top + 1, z + 3, Blocks.AIR.defaultBlockState());
        b.fill(x - 2, 0, z + 1, x + 2, top, z + 1, falling);
        b.fill(x - 2, top + 1, z - 1, x + 2, top + 1, z + 1, Blocks.WATER.defaultBlockState());
        b.fill(x - 3, top + 1, z - 2, x + 3, top + 1, z - 2, Blocks.STONE_BRICKS.defaultBlockState());
        b.fill(x - 3, top + 1, z - 1, x - 3, top + 1, z + 1, Blocks.STONE_BRICKS.defaultBlockState());
        b.fill(x + 3, top + 1, z - 1, x + 3, top + 1, z + 1, Blocks.STONE_BRICKS.defaultBlockState());
        b.fill(x - 5, -5, z + 1, x + 5, -5, z + 9, Blocks.GRAVEL.defaultBlockState());
        b.fill(x - 5, -4, z + 1, x + 5, -1, z + 9, Blocks.WATER.defaultBlockState());
    }
}
