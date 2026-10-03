package xiuxian.sect;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Fixed local terrain, independent of chunk order, world seed and job restarts. */
public final class LuoxiaTerrain {
    private LuoxiaTerrain() {}

    public static int pathHeight(int z) {
        if (z >= 108) return 0;
        if (z >= 80) return (108 - z) * 18 / 28;
        if (z >= 40) return 18;
        if (z >= 10) return 48 - (z - 10);
        if (z >= -42) return 48;
        if (z >= -78) return 48 + (-42 - z);
        if (z >= -106) return 84;
        if (z >= -154) return 84 + (-106 - z);
        return 132;
    }

    public static int heightAt(int x, int z) {
        double width = 74 + Math.min(48, Math.max(0, (90 - z) * 0.18));
        double side = Math.max(0, 1 - Math.pow(Math.abs(x) / width, 2.4));
        double length = Math.min(1, Math.max(0, (z + 280) / 44.0));
        double ridge = Math.max(0, (105 - z) * 0.46) * Math.pow(side, 0.45) * length;
        double texture = (Math.sin(x * 0.21 + z * 0.08) * 3 + Math.cos(z * 0.19) * 2) * side;
        double peak = peak(x, z, 0, -241, 42, 33, 219);
        double west = peak(x, z, -113, -104, 39, 49, 96);
        double east = peak(x, z, 120, -183, 38, 51, 144);
        double spire1 = peak(x, z, -100, -218, 25, 37, 168);
        double spire2 = peak(x, z, 81, -239, 26, 30, 186);
        int height = (int) Math.max(-5, Math.max(ridge + texture, Math.max(peak,
                Math.max(Math.max(west, east), Math.max(spire1, spire2)))));
        if (ridge < 0.5 && peak < 0 && west < 0 && east < 0 && spire1 < 0 && spire2 < 0) height = -5;
        if (inside(x, z, -44, 44, 24, 83)) height = 18;
        if (inside(x, z, -61, 61, -52, 12)) height = 48;
        if (inside(x, z, -72, 72, -122, -76)) height = 84;
        if (inside(x, z, -81, 81, -222, -148)) height = 132;
        if (inside(x, z, -127, -97, -118, -88)) height = 84;
        if (inside(x, z, 103, 137, -196, -164)) height = 132;
        // Ravines expose the two bridge arches instead of burying them in the mountain skirt.
        if (inside(x, z, -96, -79, -113, -93)) height = Math.min(height, 39);
        if (inside(x, z, 84, 102, -191, -169)) height = Math.min(height, 84);
        // Cut the center approach before applying the stairs, never bury the usable route.
        if (Math.abs(x) <= 13 && z >= -162 && z <= 112) height = pathHeight(z);
        return Math.min(219, height);
    }

    private static double peak(int x, int z, int cx, int cz, int rx, int rz, int top) {
        double radius = Math.pow((x - cx) / (double) rx, 2) + Math.pow((z - cz) / (double) rz, 2);
        if (radius >= 1) return -6;
        return -5 + (top + 5) * Math.pow(1 - radius, 0.42)
                + Math.sin(x * 0.37 + z * 0.22) * 2;
    }

    public static boolean inside(int x, int z, int x1, int x2, int z1, int z2) {
        return x >= x1 && x <= x2 && z >= z1 && z <= z2;
    }

    public static BlockState blockAt(int x, int y, int z) {
        return blockAt(x, y, z, heightAt(x, z));
    }

    public static BlockState blockAt(int x, int y, int z, int top) {
        if (y > top) return Blocks.AIR.defaultBlockState();
        if (y == top && top < 145 && (Math.abs(x) > 82 || z > 83 || z < -224)) {
            return top <= -4 ? Blocks.GRAVEL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y < -7) return Blocks.STONE.defaultBlockState();
        int stripe = Math.floorMod(y + (int) (Math.sin(z * 0.09 + x * 0.03) * 4), 23);
        if (stripe <= 3) return Blocks.CALCITE.defaultBlockState();
        if (stripe == 4 || stripe == 5) return Blocks.TUFF.defaultBlockState();
        if (stripe == 12 || stripe == 13) return Blocks.ANDESITE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }
}
