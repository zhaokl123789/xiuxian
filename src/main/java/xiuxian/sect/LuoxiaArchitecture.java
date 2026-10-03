package xiuxian.sect;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Walkable buildings and courtyards; coordinates share the mountain blueprint's north axis. */
public final class LuoxiaArchitecture {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState TRIM = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState WALL = Blocks.CALCITE.defaultBlockState();
    private static final BlockState COLUMN = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
    private static final BlockState TIMBER = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState GOLD = Blocks.CUT_COPPER.defaultBlockState();
    private static final BlockState RIDGE = Blocks.WAXED_CUT_COPPER.defaultBlockState();

    private LuoxiaArchitecture() {
    }

    public static void addTo(LuoxiaBlueprint b) {
        courtyard(b, 40, 28, 80, 18);
        courtyard(b, 57, -48, 0, 48);
        courtyard(b, 68, -116, -76, 84);
        courtyard(b, 76, -214, -150, 132);

        gatehouse(b, 0, 64, 19, 41, 19, 14, 7);
        gatehouse(b, 0, -13, 49, 33, 15, 12, 5);
        gatehouse(b, 0, -91, 85, 37, 15, 13, 6);

        galleries(b, 38, 30, 78, 18);
        galleries(b, 55, -46, -2, 48);
        galleries(b, 66, -114, -78, 84);
        galleries(b, 73, -212, -152, 132);

        for (int side : new int[]{-1, 1}) {
            tower(b, side * 36, 33, 19, 9, 2);
            tower(b, side * 36, 76, 19, 9, 2);
            tower(b, side * 53, -5, 49, 9, 2);
            tower(b, side * 53, -43, 49, 9, 2);
            tower(b, side * 63, -81, 85, 9, 2);
            tower(b, side * 63, -111, 85, 9, 2);
            tower(b, side * 70, -158, 133, 11, 3);
            tower(b, side * 70, -205, 133, 11, 3);

            hall(b, side * 40, -26, 49, 19, 23, 8, 3, false);
            hall(b, side * 48, -98, 85, 25, 25, 10, 4, false);
            hall(b, side * 53, -183, 133, 23, 35, 13, 5, false);
            garden(b, side * 23, 41, 18, 10, 7);
            garden(b, side * 25, -37, 48, 7, 5);
            garden(b, side * 23, -103, 84, 8, 5);
            lanternPillar(b, side * 15, 51, 19, 6);
            lanternPillar(b, side * 20, -4, 49, 6);
            lanternPillar(b, side * 24, -81, 85, 7);
            lanternPillar(b, side * 19, -157, 133, 9);
        }

        hall(b, 0, -189, 133, 65, 41, 16, 8, true);
        mainHallPorch(b);
        bridge(b, -112, -67, -103, 84);
        bridge(b, 76, 120, -180, 132);
        tower(b, -112, -103, 85, 17, 3);
        tower(b, 120, -180, 133, 21, 3);
        bridgeEntrance(b, -69, -63, -103, 84);
        bridgeEntrance(b, 70, 79, -180, 132);
        bridgeEntrance(b, -105, -101, -103, 84);
        bridgeEntrance(b, 108, 112, -180, 132);

        // The north door remains connected to the future inner-sect entrance.
        b.fill(-7, 132, -220, 7, 133, -210, STONE);
        b.fill(-5, 133, -220, 5, 133, -210, TRIM);
        railZ(b, -7, -220, -211, 134);
        railZ(b, 7, -220, -211, 134);
    }

    private static void courtyard(LuoxiaBlueprint b, int halfWidth, int north, int south, int y) {
        b.fill(-halfWidth, y - 3, north, halfWidth, y, south, STONE);
        b.fill(-halfWidth, y, north, halfWidth, y, south, Blocks.SMOOTH_STONE.defaultBlockState());
        b.fill(-halfWidth, y, north, halfWidth, y, north + 1, TRIM);
        b.fill(-halfWidth, y, south - 1, halfWidth, y, south, TRIM);
        b.fill(-halfWidth, y, north, -halfWidth + 1, y, south, TRIM);
        b.fill(halfWidth - 1, y, north, halfWidth, y, south, TRIM);
        for (int x = -halfWidth + 4; x <= halfWidth - 4; x += 8) {
            b.fill(x, y, north + 2, x, y, south - 2, STONE);
        }
        for (int z = north + 4; z <= south - 4; z += 8) {
            b.fill(-halfWidth + 2, y, z, halfWidth - 2, y, z, STONE);
        }
        b.fill(-8, y, north, 8, y, south, TRIM);
        for (int z = north + 3; z < south - 3; z += 4) {
            b.fill(-1, y, z, 1, y, z, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        }
        railZ(b, -halfWidth, north, south, y + 1);
        railZ(b, halfWidth, north, south, y + 1);
        railX(b, -halfWidth, -13, north, y + 1);
        railX(b, 13, halfWidth, north, y + 1);
        railX(b, -halfWidth, -13, south, y + 1);
        railX(b, 13, halfWidth, south, y + 1);
    }

    private static void gatehouse(LuoxiaBlueprint b, int cx, int cz, int feet,
                                  int width, int depth, int height, int passageHalfWidth) {
        int x1 = cx - width / 2;
        int x2 = cx + width / 2;
        int z1 = cz - depth / 2;
        int z2 = cz + depth / 2;
        b.fill(x1 - 1, feet - 1, z1 - 1, x2 + 1, feet - 1, z2 + 1, TRIM);
        b.fill(x1, feet, z1, x2, feet + height - 1, z2, WALL);
        b.fill(x1 + 2, feet, z1 + 2, x2 - 2, feet + height - 2, z2 - 2, AIR);
        b.fill(cx - passageHalfWidth, feet, z1, cx + passageHalfWidth,
                feet + height - 3, z2, AIR);
        b.fill(cx - passageHalfWidth, feet - 1, z1, cx + passageHalfWidth, feet - 1, z2, TRIM);
        for (int z : new int[]{z1, z2}) {
            b.fill(x1, feet, z, x2, feet + 1, z, TRIM);
            b.fill(cx - passageHalfWidth, feet, z, cx + passageHalfWidth,
                    feet + height - 3, z, AIR);
            for (int x : new int[]{x1 + 1, cx - passageHalfWidth - 1,
                    cx + passageHalfWidth + 1, x2 - 1}) {
                column(b, x, z, feet, height);
            }
            b.fill(x1, feet + height - 1, z, x2, feet + height, z, COLUMN);
            b.fill(cx - passageHalfWidth - 1, feet + height - 2, z,
                    cx + passageHalfWidth + 1, feet + height - 2, z, RIDGE);
            plaque(b, cx, z, feet + height - 1, 9);
            for (int x : new int[]{x1 + 4, x2 - 4}) {
                window(b, x - 1, feet + 5, z, x + 1, feet + 8, z);
            }
        }
        b.fill(x1, feet + height, z1, x2, feet + height, z2, TIMBER);
        hipRoof(b, cx, cz, feet + height + 1, width + 6, depth + 6);
        int upperY = feet + height + 7;
        b.fill(cx - width / 4, upperY, cz - 3, cx + width / 4, upperY + 4, cz + 3, COLUMN);
        b.fill(cx - width / 4 + 1, upperY + 1, cz - 3, cx + width / 4 - 1,
                upperY + 3, cz + 3, WALL);
        hipRoof(b, cx, cz, upperY + 5, width - 4, 15);
        for (int x : new int[]{cx - passageHalfWidth + 1, cx + passageHalfWidth - 1}) {
            hangingLantern(b, x, feet + height - 4, z1 + 1);
            hangingLantern(b, x, feet + height - 4, z2 - 1);
        }
    }

    private static void hall(LuoxiaBlueprint b, int cx, int cz, int floor,
                             int width, int depth, int wallHeight, int doorHalfWidth, boolean grand) {
        int x1 = cx - width / 2;
        int x2 = cx + width / 2;
        int z1 = cz - depth / 2;
        int z2 = cz + depth / 2;
        int feet = floor + 1;
        b.fill(x1 - 1, floor - 2, z1 - 1, x2 + 1, floor, z2 + 1, TRIM);
        b.fill(x1, feet, z1, x2, floor + wallHeight - 1, z2, WALL);
        b.fill(x1 + 1, feet, z1 + 1, x2 - 1, floor + wallHeight - 1, z2 - 1, AIR);
        b.fill(x1 + 1, floor, z1 + 1, x2 - 1, floor, z2 - 1, TIMBER);
        b.fill(cx - doorHalfWidth, floor, z1, cx + doorHalfWidth, floor, z2, TRIM);
        b.fill(cx - doorHalfWidth, feet, z2, cx + doorHalfWidth,
                floor + wallHeight - 3, z2, AIR);
        b.fill(cx - Math.min(6, doorHalfWidth), feet, z1, cx + Math.min(6, doorHalfWidth),
                floor + wallHeight - 4, z1, AIR);
        b.fill(x1, feet, cz - 3, x1, feet + 5, cz + 3, AIR);
        b.fill(x2, feet, cz - 3, x2, feet + 5, cz + 3, AIR);

        for (int x = x1 + 1; x <= x2; x += 8) {
            if (Math.abs(x - cx) > Math.min(6, doorHalfWidth)) {
                column(b, x, z1, feet, wallHeight - 1);
            }
            if (Math.abs(x - cx) > doorHalfWidth) {
                column(b, x, z2, feet, wallHeight - 1);
            }
            if (Math.abs(x - cx) > doorHalfWidth + 3 && x + 4 < x2) {
                window(b, x + 1, feet + 3, z1, x + 4, feet + 6, z1);
                window(b, x + 1, feet + 3, z2, x + 4, feet + 6, z2);
            }
        }
        for (int z = z1 + 1; z <= z2; z += 8) {
            column(b, x1, z, feet, wallHeight - 1);
            column(b, x2, z, feet, wallHeight - 1);
            if (Math.abs(z - cz) > 6 && z + 4 < z2) {
                window(b, x1, feet + 3, z + 1, x1, feet + 6, z + 4);
                window(b, x2, feet + 3, z + 1, x2, feet + 6, z + 4);
            }
        }
        for (int z : new int[]{z1, z2}) {
            for (int x : new int[]{cx - doorHalfWidth - 1, cx + doorHalfWidth + 1}) {
                column(b, x, z, feet, wallHeight - 1);
            }
            b.fill(x1, floor + wallHeight - 1, z, x2, floor + wallHeight, z, COLUMN);
        }
        b.fill(x1, floor + wallHeight - 1, z1, x1, floor + wallHeight, z2, COLUMN);
        b.fill(x2, floor + wallHeight - 1, z1, x2, floor + wallHeight, z2, COLUMN);
        b.fill(x1, floor + wallHeight, z1, x2, floor + wallHeight, z2, TIMBER);
        hipRoof(b, cx, cz, floor + wallHeight + 1, width + 8, depth + 8);
        plaque(b, cx, z2, floor + wallHeight - 2, Math.min(width - 8, grand ? 15 : 7));

        for (int side : new int[]{-1, 1}) {
            for (int z = z1 + 4; z < z2 - 4; z += 8) {
                hangingLantern(b, cx + side * (width / 2 - 3), floor + wallHeight - 3, z);
            }
            int furnishingX = cx + side * (width / 2 - 4);
            b.fill(furnishingX - 1, feet, z1 + 3, furnishingX + 1, feet + 2, z1 + 7,
                    Blocks.BOOKSHELF.defaultBlockState());
            for (int z = cz - 3; z <= cz + 7; z += 5) {
                b.fill(furnishingX - 1, feet, z, furnishingX + 1, feet, z, TIMBER);
                b.block(furnishingX, feet + 1, z, Blocks.FLOWER_POT.defaultBlockState());
            }
        }
        if (grand) {
            int upper = floor + wallHeight + 16;
            upperTier(b, cx, cz, upper, 43, 25, 5);
            upperTier(b, cx, cz, upper + 12, 27, 13, 3);
            for (int side : new int[]{-1, 1}) {
                column(b, cx + side * 19, cz - 11, feet, wallHeight - 1);
                column(b, cx + side * 19, cz + 11, feet, wallHeight - 1);
                b.fill(cx + side * 15 - 3, feet, z1 + 10,
                        cx + side * 15 + 3, feet, z1 + 14, TRIM);
                b.block(cx + side * 15, feet + 1, z1 + 12, Blocks.LODESTONE.defaultBlockState());
                b.block(cx + side * 15, feet + 2, z1 + 12, Blocks.AMETHYST_BLOCK.defaultBlockState());
                b.fill(cx + side * 15 - 2, feet, z2 - 7,
                        cx + side * 15 + 2, feet, z2 - 3, Blocks.RED_CARPET.defaultBlockState());
            }
        } else {
            podiumEntrances(b, cx, cz, floor, x1, x2, z1, z2);
        }
    }

    private static void podiumEntrances(LuoxiaBlueprint b, int cx, int cz, int floor,
                                         int west, int east, int north, int south) {
        BlockState quartzStair = Blocks.QUARTZ_STAIRS.defaultBlockState();
        b.fill(cx - 2, floor, south + 2, cx + 2, floor, south + 2,
                stair(quartzStair, Direction.NORTH));
        b.fill(cx - 2, floor, north - 2, cx + 2, floor, north - 2,
                stair(quartzStair, Direction.SOUTH));
        b.fill(west - 2, floor, cz - 2, west - 2, floor, cz + 2,
                stair(quartzStair, Direction.EAST));
        b.fill(east + 2, floor, cz - 2, east + 2, floor, cz + 2,
                stair(quartzStair, Direction.WEST));
        // Wall framing must not turn a side doorway into an unbroken column.
        b.fill(west, floor + 1, cz - 1, west, floor + 4, cz + 1, AIR);
        b.fill(east, floor + 1, cz - 1, east, floor + 4, cz + 1, AIR);
        b.fill(cx - 1, floor + 1, north, cx + 1, floor + 4, north, AIR);
        b.fill(cx - 1, floor + 1, south, cx + 1, floor + 4, south, AIR);
    }

    private static void bridgeEntrance(LuoxiaBlueprint b, int west, int east, int cz, int floor) {
        b.fill(west, floor, cz - 1, east, floor, cz + 1, TRIM);
        b.fill(west, floor + 1, cz - 1, east, floor + 4, cz + 1, AIR);
    }

    private static void upperTier(LuoxiaBlueprint b, int cx, int cz, int y, int width, int depth, int height) {
        int x = width / 2 - 3;
        int z = depth / 2 - 3;
        b.fill(cx - x, y, cz - z, cx + x, y + height, cz + z, WALL);
        for (int px = cx - x; px <= cx + x; px += 6) {
            b.fill(px, y, cz - z, px, y + height, cz - z, COLUMN);
            b.fill(px, y, cz + z, px, y + height, cz + z, COLUMN);
        }
        b.fill(cx - x, y + height, cz - z, cx + x, y + height, cz + z, TIMBER);
        hipRoof(b, cx, cz, y + height + 1, width, depth);
    }

    private static void mainHallPorch(LuoxiaBlueprint b) {
        b.fill(-23, 132, -168, 23, 133, -164, TRIM);
        b.fill(-9, 133, -163, 9, 133, -163, stair(Blocks.QUARTZ_STAIRS.defaultBlockState(), Direction.NORTH));
        for (int side : new int[]{-1, 1}) {
            for (int x : new int[]{13, 21}) {
                column(b, side * x, -164, 134, 13);
                hangingLantern(b, side * x, 143, -165);
            }
            railX(b, side < 0 ? -23 : 10, side < 0 ? -10 : 23, -164, 134);
        }
        b.fill(-24, 147, -168, 24, 147, -162, COLUMN);
        hipRoof(b, 0, -166, 148, 55, 13);
    }

    private static void tower(LuoxiaBlueprint b, int cx, int cz, int feet, int width, int levels) {
        int half = width / 2;
        int height = levels * 6;
        b.fill(cx - half - 1, feet - 3, cz - half - 1,
                cx + half + 1, feet - 1, cz + half + 1, TRIM);
        b.fill(cx - half, feet, cz - half, cx + half, feet + height, cz + half, WALL);
        b.fill(cx - half + 1, feet, cz - half + 1,
                cx + half - 1, feet + height - 1, cz + half - 1, AIR);
        b.fill(cx - 1, feet, cz + half, cx + 1, feet + 3, cz + half, AIR);
        b.fill(cx - 1, feet, cz - half, cx + 1, feet + 3, cz - half, AIR);
        b.fill(cx - half, feet, cz - 1, cx - half, feet + 3, cz + 1, AIR);
        b.fill(cx + half, feet, cz - 1, cx + half, feet + 3, cz + 1, AIR);
        for (int level = 1; level <= levels; level++) {
            int y = feet + level * 6;
            b.fill(cx - half + 1, y - 1, cz - half + 1,
                    cx + half - 1, y - 1, cz + half - 1, TIMBER);
            b.block(cx, y - 1, cz - half + 2, AIR);
            for (int z : new int[]{cz - half, cz + half}) {
                window(b, cx - 1, y - 4, z, cx + 1, y - 2, z);
            }
            for (int x : new int[]{cx - half, cx + half}) {
                window(b, x, y - 4, cz - 1, x, y - 2, cz + 1);
            }
        }
        b.fill(cx, feet, cz - half + 1, cx, feet + height - 1, cz - half + 1, COLUMN);
        b.fill(cx, feet, cz - half + 2, cx, feet + height - 1, cz - half + 2,
                Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        for (int x : new int[]{cx - half, cx + half}) {
            for (int z : new int[]{cz - half, cz + half}) {
                column(b, x, z, feet, height + 1);
            }
        }
        hipRoof(b, cx, cz, feet + height + 1, width + 6, width + 6);
        upperTier(b, cx, cz, feet + height + 7, width - 2, width - 2, 3);
        b.block(cx, feet + height + 15, cz, Blocks.LIGHTNING_ROD.defaultBlockState());
        hangingLantern(b, cx, feet + 4, cz + half + 1);
    }

    private static void galleries(LuoxiaBlueprint b, int x, int north, int south, int floor) {
        galleryZ(b, -x, north, south, floor);
        galleryZ(b, x, north, south, floor);
        galleryX(b, -x, -14, north + 2, floor);
        galleryX(b, 14, x, north + 2, floor);
        galleryX(b, -x, -14, south - 2, floor);
        galleryX(b, 14, x, south - 2, floor);
    }

    private static void galleryZ(LuoxiaBlueprint b, int cx, int north, int south, int floor) {
        b.fill(cx - 2, floor - 1, north, cx + 2, floor, south, TRIM);
        b.fill(cx - 1, floor + 1, north, cx + 1, floor + 5, south, AIR);
        for (int z = north; z <= south; z += 6) {
            column(b, cx - 2, z, floor + 1, 5);
            column(b, cx + 2, z, floor + 1, 5);
            hangingLantern(b, cx, floor + 4, z);
        }
        b.fill(cx - 2, floor + 6, north, cx + 2, floor + 6, south, TIMBER);
        for (int offset = -3; offset <= 3; offset++) {
            int roofY = floor + 7 + (3 - Math.abs(offset)) / 2;
            b.fill(cx + offset, roofY, north - 1, cx + offset, roofY, south + 1, ROOF);
        }
        b.fill(cx, floor + 9, north - 1, cx, floor + 9, south + 1, RIDGE);
    }

    private static void galleryX(LuoxiaBlueprint b, int west, int east, int cz, int floor) {
        b.fill(west, floor - 1, cz - 2, east, floor, cz + 2, TRIM);
        b.fill(west, floor + 1, cz - 1, east, floor + 5, cz + 1, AIR);
        for (int x = west; x <= east; x += 6) {
            column(b, x, cz - 2, floor + 1, 5);
            column(b, x, cz + 2, floor + 1, 5);
            hangingLantern(b, x, floor + 4, cz);
        }
        b.fill(west, floor + 6, cz - 2, east, floor + 6, cz + 2, TIMBER);
        for (int offset = -3; offset <= 3; offset++) {
            int roofY = floor + 7 + (3 - Math.abs(offset)) / 2;
            b.fill(west - 1, roofY, cz + offset, east + 1, roofY, cz + offset, ROOF);
        }
        b.fill(west - 1, floor + 9, cz, east + 1, floor + 9, cz, RIDGE);
    }

    private static void bridge(LuoxiaBlueprint b, int west, int east, int cz, int floor) {
        b.fill(west, floor - 2, cz - 4, east, floor - 1, cz + 4, STONE);
        b.fill(west, floor, cz - 4, east, floor, cz + 4, TRIM);
        for (int x = west; x <= east; x++) {
            double t = (double) (x - west) / (east - west);
            int arch = floor - 27 + (int) Math.round(22 * Math.sin(Math.PI * t));
            b.fill(x, arch, cz - 4, x, floor - 2, cz - 3, TRIM);
            b.fill(x, arch, cz + 3, x, floor - 2, cz + 4, TRIM);
            if (x < west + 3 || x > east - 3) {
                b.fill(x, floor - 31, cz - 4, x, floor - 2, cz + 4, STONE);
            }
        }
        railX(b, west, east, cz - 4, floor + 1);
        railX(b, west, east, cz + 4, floor + 1);
        galleryX(b, west, east, cz, floor);
    }

    private static void hipRoof(LuoxiaBlueprint b, int cx, int cz, int eaveY, int width, int depth) {
        int hx = width / 2;
        int hz = depth / 2;
        int rings = Math.min(hx, hz);
        for (int ring = 0; ring < rings; ring++) {
            int x1 = cx - hx + ring;
            int x2 = cx + hx - ring;
            int z1 = cz - hz + ring;
            int z2 = cz + hz - ring;
            int y = eaveY + roofRise(ring);
            BlockState tiles = ring == 0 ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
                    .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP) : ROOF;
            b.fill(x1, y, z1, x2, y, z1, tiles);
            b.fill(x1, y, z2, x2, y, z2, tiles);
            b.fill(x1, y, z1, x1, y, z2, tiles);
            b.fill(x2, y, z1, x2, y, z2, tiles);
            if (ring > 0 && roofRise(ring) > roofRise(ring - 1)) {
                b.fill(x1, y - 1, z1, x2, y - 1, z1, ROOF);
                b.fill(x1, y - 1, z2, x2, y - 1, z2, ROOF);
                b.fill(x1, y - 1, z1, x1, y - 1, z2, ROOF);
                b.fill(x2, y - 1, z1, x2, y - 1, z2, ROOF);
            }
        }
        int ridgeY = eaveY + roofRise(rings);
        int ridgeHalf = hx - rings;
        b.fill(cx - ridgeHalf, ridgeY, cz - 1, cx + ridgeHalf, ridgeY, cz + 1, ROOF);
        b.fill(cx - ridgeHalf, ridgeY + 1, cz, cx + ridgeHalf, ridgeY + 1, cz, RIDGE);
        b.block(cx - ridgeHalf - 1, ridgeY + 2, cz, GOLD);
        b.block(cx + ridgeHalf + 1, ridgeY + 2, cz, GOLD);
        b.block(cx, ridgeY + 2, cz, Blocks.GOLD_BLOCK.defaultBlockState());
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                for (int step = 0; step <= 2; step++) {
                    b.block(cx + sx * (hx + step), eaveY + step,
                            cz + sz * (hz + step), ROOF);
                    b.block(cx + sx * (hx + step), eaveY + step + 1,
                            cz + sz * (hz + step), RIDGE);
                }
                b.block(cx + sx * (hx + 2), eaveY + 4, cz + sz * (hz + 2),
                        Blocks.LIGHTNING_ROD.defaultBlockState());
                for (int ring = 1; ring < rings; ring += 3) {
                    b.block(cx + sx * (hx - ring), eaveY + roofRise(ring) + 1,
                            cz + sz * (hz - ring), RIDGE);
                }
            }
        }
    }

    private static int roofRise(int ring) {
        return ring < 5 ? ring / 2 : 2 + (ring - 4) * 2 / 3;
    }

    private static void column(LuoxiaBlueprint b, int x, int z, int feet, int height) {
        b.block(x, feet, z, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        b.fill(x, feet + 1, z, x, feet + height - 1, z, COLUMN);
        b.block(x, feet + height, z, GOLD);
    }

    private static void window(LuoxiaBlueprint b, int x1, int y1, int z1, int x2, int y2, int z2) {
        BlockState panes = Blocks.BLACK_STAINED_GLASS_PANE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.SOUTH, true)
                .setValue(BlockStateProperties.EAST, true)
                .setValue(BlockStateProperties.WEST, true);
        b.fill(x1, y1, z1, x2, y2, z2, panes);
        b.fill(x1, y1 - 1, z1, x2, y1 - 1, z2, COLUMN);
        b.fill(x1, y2 + 1, z1, x2, y2 + 1, z2, COLUMN);
    }

    private static void plaque(LuoxiaBlueprint b, int x, int z, int y, int width) {
        b.fill(x - width / 2, y, z, x + width / 2, y + 1, z, RIDGE);
        b.fill(x - width / 2 + 1, y, z, x + width / 2 - 1, y, z,
                Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    private static void hangingLantern(LuoxiaBlueprint b, int x, int y, int z) {
        b.block(x, y + 1, z, Blocks.CHAIN.defaultBlockState());
        b.block(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    private static void lanternPillar(LuoxiaBlueprint b, int x, int z, int feet, int height) {
        b.fill(x - 1, feet, z - 1, x + 1, feet, z + 1, TRIM);
        b.fill(x, feet + 1, z, x, feet + height - 1, z, COLUMN);
        b.fill(x - 1, feet + height, z - 1, x + 1, feet + height, z + 1, RIDGE);
        b.block(x, feet + height + 1, z, Blocks.LANTERN.defaultBlockState());
        hangingLantern(b, x - 1, feet + height - 1, z);
        hangingLantern(b, x + 1, feet + height - 1, z);
    }

    private static void railX(LuoxiaBlueprint b, int x1, int x2, int z, int y) {
        b.fill(x1, y, z, x2, y, z, Blocks.QUARTZ_SLAB.defaultBlockState());
        for (int x = x1; x <= x2; x += 4) {
            b.fill(x, y, z, x, y + 1, z, TRIM);
            b.block(x, y + 2, z, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        }
    }

    private static void railZ(LuoxiaBlueprint b, int x, int z1, int z2, int y) {
        b.fill(x, y, z1, x, y, z2, Blocks.QUARTZ_SLAB.defaultBlockState());
        for (int z = z1; z <= z2; z += 4) {
            b.fill(x, y, z, x, y + 1, z, TRIM);
            b.block(x, y + 2, z, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        }
    }

    private static void garden(LuoxiaBlueprint b, int cx, int cz, int floor, int halfX, int halfZ) {
        b.fill(cx - halfX, floor, cz - halfZ, cx + halfX, floor, cz + halfZ,
                Blocks.MOSS_BLOCK.defaultBlockState());
        for (int x : new int[]{cx - halfX, cx + halfX}) {
            b.fill(x, floor + 1, cz - halfZ, x, floor + 1, cz + halfZ,
                    Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        b.fill(cx - halfX, floor + 1, cz - halfZ, cx + halfX, floor + 1, cz - halfZ,
                Blocks.STONE_BRICK_SLAB.defaultBlockState());
        b.fill(cx - halfX, floor + 1, cz + halfZ, cx + halfX, floor + 1, cz + halfZ,
                Blocks.STONE_BRICK_SLAB.defaultBlockState());
        b.fill(cx - 2, floor, cz - 1, cx + 2, floor, cz + 1, Blocks.WATER.defaultBlockState());
        for (int side : new int[]{-1, 1}) {
            b.block(cx + side * (halfX - 2), floor + 1, cz - 2,
                    Blocks.AZALEA.defaultBlockState());
            b.block(cx + side * (halfX - 2), floor + 1, cz + 2,
                    Blocks.FLOWERING_AZALEA.defaultBlockState());
        }
    }

    private static BlockState stair(BlockState state, Direction facing) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
}
