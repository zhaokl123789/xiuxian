package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * A deliberately standalone, fixed-layout mortal cultivation town.
 *
 * <p>This is an inspection build.  It is not called by the cave-heaven
 * generator and it never performs chunk or exploration based refreshing.
 * The supplied origin is the central plaza foundation block; the whole town
 * is laid out from that point with a readable north/south axis.</p>
 */
public final class MortalTownGenerator {
    // The inspection town is intentionally a large city-scale landmark.
    private static final int HALF_X = 150;
    private static final int HALF_Z = 108;

    private static final BlockState FOUNDATION = material("town_black_bricks");
    private static final BlockState ROAD = material("town_paving");
    private static final BlockState ROAD_TRIM = material("town_white_jade");
    private static final BlockState WALL = material("town_white_plaster");
    private static final BlockState WALL_CAP = material("town_black_bricks");
    private static final BlockState WHITE = material("town_white_jade");
    private static final BlockState PILLAR = material("town_vermilion_pillar");
    private static final BlockState RED = material("town_vermilion_pillar");
    private static final BlockState RED_SLAB = material("town_red_roof_tiles");
    private static final BlockState WOOD = material("town_rosewood");
    private static final BlockState WOOD_LOG = material("town_vermilion_pillar");
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState COPPER = Blocks.CUT_COPPER.defaultBlockState();
    private static final BlockState LAMP = material("town_stone_lamp");
    private static final BlockState LANTERN = material("town_red_lantern");
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
    private static final BlockState BAMBOO = Blocks.BAMBOO.defaultBlockState();
    private static final BlockState FLOOR = material("town_paving");
    private static final BlockState FURNITURE = material("town_bench");
    private static final BlockState BOOKSHELF = material("town_scroll_shelf");
    private static final BlockState CHEST = material("town_cabinet");
    private static final BlockState TABLE = material("town_low_desk");
    private static final BlockState CAULDRON = material("town_porcelain_planter");
    private static final BlockState BRICK = Blocks.BRICKS.defaultBlockState();

    private MortalTownGenerator() {}

    /** Queue one inspection town; full clearance, verification and building use bounded ticks. */
    public static boolean generate(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null || level.dimension() != Level.OVERWORLD) return false;
        Plan plan = createPlan();
        if ((long) origin.getY() + plan.minY < level.getMinBuildHeight()
                || (long) origin.getY() + plan.maxY >= level.getMaxBuildHeight()) return false;
        for (int x : new int[]{plan.minX, plan.maxX}) {
            for (int z : new int[]{plan.minZ, plan.maxZ}) {
                if (!level.getWorldBorder().isWithinBounds(origin.offset(x, 0, z))) return false;
            }
        }
        return MortalTownConstruction.start(level, origin, plan);
    }

    public static boolean isGenerated(ServerLevel level, BlockPos origin) {
        return level != null && origin != null && MortalTownConstruction.data(level).contains(origin);
    }

    static Plan createPlan() {
        Plan plan = new Plan();
        build(plan, 0, 0, 0);
        // Clear required rooms before any overlapping building's lamps or furniture are placed.
        plan.placements.addAll(0, plan.interiorClearances);
        return plan;
    }

    private static void build(Plan level, int cx, int floor, int cz) {
        // One continuous foundation makes the town readable on ordinary terrain
        // while all visible movement happens one block above it.
        fill(level, cx - 172, floor - 2, cz - 128, cx + 172, floor, cz + 128,
                material("town_black_bricks"));
        pave(level, cx, floor + 1, cz, HALF_X - 3, HALF_Z - 3);
        buildCanals(level, cx, floor, cz);
        buildWalls(level, cx, floor, cz);
        buildMainGates(level, cx, floor, cz);
        buildCentralAxis(level, cx, floor, cz);
        buildCentralPalace(level, cx, floor + 1, cz);
        buildMarketRows(level, cx, floor + 1, cz);
        buildCourtyards(level, cx, floor + 1, cz);
        buildGardensAndLanterns(level, cx, floor + 1, cz);
        buildOuterDistricts(level, cx, floor + 1, cz);
        buildLandmarks(level, cx, floor + 1, cz);
        // Outer district courtyards and landmark edging are intentionally
        // authored after the houses.  Restore all residential entry doors as
        // the final pass so their lower block cannot be replaced by a courtyard
        // rim or decorative path while keeping the surrounding facade intact.
        restoreResidentialDoors(level, cx, floor + 1, cz);
    }

    private static void pave(Plan level, int cx, int y, int cz, int hx, int hz) {
        BlockState road = material("town_paving");
        BlockState trim = material("town_white_jade");
        fill(level, cx - hx, y, cz - hz, cx + hx, y, cz + hz, road);
        for (int x = cx - hx; x <= cx + hx; x += 6) {
            set(level, x, y, cz - hz, trim);
            set(level, x, y, cz + hz, trim);
        }
        for (int z = cz - hz; z <= cz + hz; z += 6) {
            set(level, cx - hx, y, z, trim);
            set(level, cx + hx, y, z, trim);
        }
    }

    private static void buildWalls(Plan level, int cx, int y, int cz) {
        BlockState wallState = material("town_white_plaster");
        BlockState cap = material("town_black_bricks");
        wall(level, cx - HALF_X, y + 1, cz - HALF_Z, cx + HALF_X, y + 9, cz - HALF_Z, wallState);
        wall(level, cx - HALF_X, y + 1, cz + HALF_Z, cx + HALF_X, y + 9, cz + HALF_Z, wallState);
        wall(level, cx - HALF_X, y + 1, cz - HALF_Z, cx - HALF_X, y + 9, cz + HALF_Z, wallState);
        wall(level, cx + HALF_X, y + 1, cz - HALF_Z, cx + HALF_X, y + 9, cz + HALF_Z, wallState);
        // Battlements and four corner watchtowers establish a strong skyline.
        for (int x = cx - HALF_X + 4; x <= cx + HALF_X - 4; x += 8) {
            fill(level, x, y + 10, cz - HALF_Z, x + 2, y + 11, cz - HALF_Z + 1, cap);
            fill(level, x, y + 10, cz + HALF_Z - 1, x + 2, y + 11, cz + HALF_Z, cap);
        }
        for (int z = cz - HALF_Z + 4; z <= cz + HALF_Z - 4; z += 8) {
            fill(level, cx - HALF_X, y + 10, z, cx - HALF_X + 1, y + 11, z + 2, cap);
            fill(level, cx + HALF_X - 1, y + 10, z, cx + HALF_X, y + 11, z + 2, cap);
        }
        for (int[] p : new int[][]{{-HALF_X, -HALF_Z}, {-HALF_X, HALF_Z},
                {HALF_X, -HALF_Z}, {HALF_X, HALF_Z}}) {
            Direction inward = p[1] < 0 ? Direction.SOUTH
                    : p[1] > 0 ? Direction.NORTH
                    : p[0] < 0 ? Direction.EAST : Direction.WEST;
            watchTower(level, cx + p[0], y + 1, cz + p[1], inward);
        }
        // A second, lower inner wall gives the city a ceremonial outer and inner
        // city instead of one rectangular shell.
        int ix = 104, iz = 74;
        wall(level, cx - ix, y + 1, cz - iz, cx + ix, y + 6, cz - iz, cap);
        wall(level, cx - ix, y + 1, cz + iz, cx + ix, y + 6, cz + iz, cap);
        wall(level, cx - ix, y + 1, cz - iz, cx - ix, y + 6, cz + iz, cap);
        wall(level, cx + ix, y + 1, cz - iz, cx + ix, y + 6, cz + iz, cap);
    }

    private static void buildMainGates(Plan level, int cx, int y, int cz) {
        gate(level, cx, y + 1, cz - HALF_Z, true);
        gate(level, cx, y + 1, cz + HALF_Z, true);
        gate(level, cx - HALF_X, y + 1, cz, false);
        gate(level, cx + HALF_X, y + 1, cz, false);
        // Openings are cut after the wall pass, so every entrance is visibly
        // traversable even if the town was placed into an uneven hillside.
        clear(level, cx - 7, y + 1, cz - HALF_Z - 1, cx + 7, y + 6, cz - HALF_Z + 1);
        clear(level, cx - 7, y + 1, cz + HALF_Z - 1, cx + 7, y + 6, cz + HALF_Z + 1);
        clear(level, cx - HALF_X - 1, y + 1, cz - 6, cx - HALF_X + 1, y + 6, cz + 6);
        clear(level, cx + HALF_X - 1, y + 1, cz - 6, cx + HALF_X + 1, y + 6, cz + 6);
    }

    private static void gate(Plan level, int x, int y, int z, boolean northSouth) {
        int alongX = northSouth ? 1 : 0;
        int alongZ = northSouth ? 0 : 1;
        BlockState white = material("town_white_plaster");
        BlockState roofTile = material("town_red_roof_tiles");
        BlockState bracket = material("town_eave_bracket");
        BlockState lion = material("town_stone_lion");
        for (int side : new int[]{-1, 1}) {
            int px = x + side * (northSouth ? 11 : 0);
            int pz = z + side * (northSouth ? 0 : 11);
            tower(level, px, y, pz, 4, 10, white);
            Direction inward = northSouth
                    ? (z < 0 ? Direction.SOUTH : Direction.NORTH)
                    : (x < 0 ? Direction.EAST : Direction.WEST);
            towerEntrance(level, px, y, pz, 4, inward);
            fill(level, px + alongX * -4, y + 10, pz + alongZ * -4,
                    px + alongX * 4, y + 12, pz + alongZ * 4, roofTile);
            set(level, px + alongX * side, y + 1, pz + alongZ * side, lion);
            set(level, px + alongX * -side, y + 9, pz + alongZ * -side, bracket);
        }
        // A raised sign beam and paired lanterns read as a cultivation-town
        // gate rather than an ordinary Minecraft wall opening.
        fill(level, x - (northSouth ? 13 : 3), y + 13, z - (northSouth ? 3 : 13),
                x + (northSouth ? 13 : 3), y + 15, z + (northSouth ? 3 : 13), GOLD);
        lamp(level, x - (northSouth ? 8 : 0), y + 8, z - (northSouth ? 0 : 8));
        lamp(level, x + (northSouth ? 8 : 0), y + 8, z + (northSouth ? 0 : 8));
        // Character plaque pavilion blocks are compact, roofed sign details
        // made in the core mod and make each gate distinct at distance.
        BlockState plaque = material("town_cloud_plaque");
        set(level, x + (northSouth ? 0 : -2), y + 14, z + (northSouth ? -2 : 0), plaque);
        set(level, x + (northSouth ? 0 : 2), y + 14, z + (northSouth ? 2 : 0), plaque);
    }

    private static void buildCentralAxis(Plan level, int cx, int y, int cz) {
        // Broad ceremonial roads form a cross, with tiled strips and lanterns.
        BlockState trim = material("town_white_jade");
        BlockState gold = material("town_white_jade");
        BlockState white = material("town_white_jade");
        for (int z = cz - HALF_Z + 2; z <= cz + HALF_Z - 2; z++) {
            fill(level, cx - 6, y + 1, z, cx + 6, y + 1, z, trim);
            if ((z - cz) % 10 == 0) lamp(level, cx - 9, y + 2, z);
            if ((z - cz) % 10 == 0) lamp(level, cx + 9, y + 2, z);
        }
        for (int x = cx - HALF_X + 2; x <= cx + HALF_X - 2; x++) {
            fill(level, x, y + 1, cz - 6, x, y + 1, cz + 6, trim);
            if ((x - cx) % 12 == 0) lamp(level, x, y + 2, cz - 9);
            if ((x - cx) % 12 == 0) lamp(level, x, y + 2, cz + 9);
        }
        ring(level, cx, y + 2, cz, 17, 2, gold);
        circle(level, cx, y + 2, cz, 13, white);
        // A pair of bracketed ceremonial posts gives the plaza a readable
        // human-scale foreground instead of a flat paved disc.
        BlockState bracket = material("town_eave_bracket");
        for (int side : new int[]{-1, 1}) {
            set(level, cx + side * 11, y + 3, cz - 11, bracket);
            set(level, cx + side * 11, y + 3, cz + 11, bracket);
            set(level, cx + side * 11, y + 4, cz - 11, material("town_red_lantern"));
            set(level, cx + side * 11, y + 4, cz + 11, material("town_red_lantern"));
        }
    }

    private static void buildCentralPalace(Plan level, int cx, int y, int cz) {
        // Three lifted halls, broad eaves and a finial provide the skyline
        // landmark visible from each of the four city gates.
        palaceTier(level, cx, y, cz, 42, 27, 9);
        palaceTier(level, cx, y + 10, cz, 33, 22, 8);
        palaceTier(level, cx, y + 19, cz, 23, 16, 7);
        tower(level, cx, y + 26, cz, 3, 8, GOLD);
        set(level, cx, y + 35, cz, LAMP);
        for (int x = cx - 36; x <= cx + 36; x += 8) {
            lamp(level, x, y + 2, cz - 30);
            lamp(level, x, y + 2, cz + 30);
        }
        clear(level, cx - 5, y + 1, cz - 27, cx + 5, y + 5, cz - 27);
        doubleDoor(level, cx, y + 1, cz - 27, Direction.NORTH, Blocks.DARK_OAK_DOOR,
                material("town_black_bricks"), material("xian_dragon_relief"));
        interiorLamp(level, cx, y + 6, cz - 4);
        for (int x = cx - 28; x <= cx + 28; x += 8) {
            set(level, x, y + 1, cz - 12, FURNITURE);
            set(level, x, y + 2, cz - 12, material("town_red_lantern"));
        }
        for (int z = cz - 18; z <= cz + 18; z += 4) {
            set(level, cx - 35, y + 1, z, material("town_scroll_shelf"));
            set(level, cx + 35, y + 1, z, material("town_scroll_shelf"));
        }
        // Ceremonial side wings stay outside the entry and throne approach.
        for (int side : new int[]{-1, 1}) {
            int wing = cx + side * 22;
            set(level, wing, y + 1, cz - 16, material("xian_chime_rack"));
            set(level, wing, y + 1, cz - 19, material("xian_ritual_bell"));
            set(level, wing, y + 1, cz - 9, material("xian_bronze_incense_burner"));
            set(level, wing, y + 1, cz + 9, material("xian_scripture_pedestal"));
            set(level, wing, y + 1, cz + 12, material("xian_spirit_tablet"));
            set(level, wing + side * 3, y + 1, cz + 16, material("xian_sect_banner"));
            set(level, wing, y + 1, cz + 18, material("xian_bronze_candle"));
            set(level, wing, y, cz + 9, material("xian_lotus_floor"));
        }
        // The entrance hall is furnished as a ceremonial reception space. The
        // original multipart furniture gives the palace a readable interior when a
        // player walks through the open facade instead of seeing a hollow shell.
        set(level, cx, y + 1, cz - 8, material("town_throne"));
        set(level, cx - 7, y + 1, cz - 7, material("town_low_desk"));
        set(level, cx + 7, y + 1, cz - 7, material("town_low_desk"));
        set(level, cx - 10, y + 1, cz + 2, material("town_folding_screen"));
        set(level, cx + 10, y + 1, cz + 2, material("town_folding_screen"));
        for (int x = cx - 28; x <= cx + 28; x += 14) {
            set(level, x, y + 2, cz - 25, material("town_red_lantern"));
            set(level, x, y + 2, cz + 25, material("town_red_lantern"));
        }
    }

    private static void palaceTier(Plan level, int cx, int y, int cz, int hx, int hz, int height) {
        BlockState white = material("town_white_plaster");
        BlockState timber = material("town_rosewood");
        BlockState roofTile = material("town_red_roof_tiles");
        BlockState bracket = material("town_eave_bracket");
        fill(level, cx - hx, y, cz - hz, cx + hx, y, cz + hz, white);
        level.clearInterior(cx - hx + 1, y + 1, cz - hz + 1, cx + hx - 1, y + height, cz + hz - 1);
        for (int dy = 1; dy <= height; dy++) {
            frame(level, cx - hx, y + dy, cz - hz, cx + hx, y + dy, cz + hz,
                    dy == height ? RED : timber);
        }
        roof(level, cx, y + height + 1, cz, hx + 3, hz + 3, roofTile);
        for (int side : new int[]{-1, 1}) {
            set(level, cx + side * Math.max(2, hx - 2), y + height, cz - hz, bracket);
            set(level, cx + side * Math.max(2, hx - 2), y + height, cz + hz, bracket);
        }
        for (int dx = -hx + 5; dx <= hx - 5; dx += 10) {
            set(level, cx + dx, y + 2, cz - hz + 1, bracket);
            set(level, cx + dx, y + 2, cz + hz - 1, bracket);
        }
        clear(level, cx - 2, y + 1, cz - hz, cx + 2, y + 3, cz - hz);
        doubleDoor(level, cx, y + 1, cz - hz, Direction.NORTH, Blocks.SPRUCE_DOOR,
                material("town_vermilion_pillar"), material("xian_cloud_transom"));
        for (int side : new int[]{-1, 1}) {
            set(level, cx + side * 3, y + 2, cz - hz, material("xian_carved_door_panel"));
            set(level, cx + side * 12, y + 4, cz - hz, material("xian_dragon_relief"));
            set(level, cx + side * 12, y + 5, cz - hz, material("xian_cloud_transom"));
            set(level, cx + side * 12, y + height - 1, cz - hz + 1, material("xian_palace_lantern"));
        }
    }

    private static void buildMarketRows(Plan level, int cx, int y, int cz) {
        // Fixed merchants' lanes: tea houses, medicine shops, spirit-paper
        // stalls and inns alternate along two long streets.
        // Keep every shop footprint clear of the two longitudinal canals;
        // the market fronts face the water instead of accidentally filling it.
        int[] xs = {-96, -76, -52, -24, 24, 52, 76, 96};
        int[] zs = {-72, -54, -34, 34, 54, 72};
        int index = 0;
        for (int xOff : xs) {
            for (int zOff : zs) {
                int w = (index & 1) == 0 ? 7 : 9;
                int d = (index & 1) == 0 ? 8 : 7;
                shop(level, cx + xOff, y + 1, cz + zOff, w, d, 5 + index % 3,
                        (index & 1) == 0 ? RED : WOOD);
                furnishShop(level, cx + xOff, y + 1, cz + zOff, w, d, index % 6);
                index++;
            }
        }
        for (int x = cx - 70; x <= cx + 70; x += 14) {
            marketStall(level, x, y + 2, cz - 12, (x / 14 & 1) == 0 ? RED : WOOD);
            marketStall(level, x, y + 2, cz + 12, (x / 14 & 1) == 0 ? WOOD : RED);
        }
        // Covered arcades and display plinths make the market read as a lived-in
        // street, with furniture visible through the open fronts.
        for (int x = cx - 108; x <= cx + 108; x += 18) {
            shop(level, x, y + 1, cz - 88, 6, 5, 4, BRICK);
            shop(level, x, y + 1, cz + 88, 6, 5, 4, BRICK);
            furnishShop(level, x, y + 1, cz - 88, 6, 5, Math.floorMod(x / 18, 6));
            furnishShop(level, x, y + 1, cz + 88, 6, 5, Math.floorMod(x / 18 + 3, 6));
        }
    }

    private static void buildCourtyards(Plan level, int cx, int y, int cz) {
        for (int[] p : new int[][]{{-56, -8}, {-24, -8}, {24, -8}, {56, -8},
                {-56, 8}, {-24, 8}, {24, 8}, {56, 8}}) {
            courtyard(level, cx + p[0], y, cz + p[1], 7);
        }
    }

    private static void buildGardensAndLanterns(Plan level, int cx, int y, int cz) {
        for (int[] p : new int[][]{{-70, -52}, {-52, -52}, {52, -52}, {70, -52},
                {-70, 52}, {-52, 52}, {52, 52}, {70, 52}}) {
            garden(level, cx + p[0], y + 1, cz + p[1]);
        }
        for (int z = cz - 96; z <= cz + 96; z += 8) {
            lamp(level, cx - 16, y + 2, z);
            lamp(level, cx + 16, y + 2, z);
        }
    }

    private static void buildOuterDistricts(Plan level, int cx, int y, int cz) {
        int[][] districts = {{-118, -70}, {-118, 70}, {118, -70}, {118, 70}};
        for (int[] district : districts) {
            int dx = district[0], dz = district[1];
            for (int row = -1; row <= 1; row++) {
                for (int col = -2; col <= 2; col++) {
                    // The district center is reserved for its shared courtyard.
                    // A house here would be overwritten by the courtyard basin
                    // and would leave a false, unusable doorway in the shell.
                    if (row == 0 && col == 0) continue;
                    int hx = 8 + Math.abs(col % 2);
                    int hz = 7 + Math.abs(row);
                    house(level, cx + dx + col * 20, y, cz + dz + row * 20, hx, hz,
                            5 + ((row + col + 6) % 3), (row + col) % 2 == 0);
                }
            }
            courtyard(level, cx + dx, y, cz + dz, 13);
            lamp(level, cx + dx - 16, y + 2, cz + dz);
            lamp(level, cx + dx + 16, y + 2, cz + dz);
        }
        // Administrative and artisan lanes around the inner wall.
        for (int x = cx - 92; x <= cx + 92; x += 23) {
            house(level, x, y, cz - 62, 9, 8, 7, true);
            house(level, x, y, cz + 62, 9, 8, 7, false);
        }
    }

    private static void buildLandmarks(Plan level, int cx, int y, int cz) {
        // Four tall pagodas anchor the skyline and make the city legible from afar.
        for (int[] p : new int[][]{{-112, 0}, {112, 0}, {0, -86}, {0, 86}}) {
            Direction inward = p[0] < 0 ? Direction.EAST
                    : p[0] > 0 ? Direction.WEST
                    : p[1] < 0 ? Direction.SOUTH : Direction.NORTH;
            pagoda(level, cx + p[0], y, cz + p[1], inward);
        }
        for (int x = cx - 96; x <= cx + 96; x += 24) {
            lamp(level, x, y + 2, cz - 22);
            lamp(level, x, y + 2, cz + 22);
        }
    }

    private static void restoreResidentialDoors(Plan level, int cx, int y, int cz) {
        int[][] districts = {{-118, -70}, {-118, 70}, {118, -70}, {118, 70}};
        for (int[] district : districts) {
            int dx = district[0], dz = district[1];
            for (int row = -1; row <= 1; row++) {
                for (int col = -2; col <= 2; col++) {
                    if (row == 0 && col == 0) continue;
                    int hz = 7 + Math.abs(row);
                    doubleDoor(level, cx + dx + col * 20, y + 1,
                            cz + dz + row * 20 - hz, Direction.NORTH, Blocks.SPRUCE_DOOR,
                            material("town_vermilion_pillar"), material("xian_cloud_transom"));
                }
            }
        }
        // The two artisan lanes sit against the inner wall and are also
        // adjacent to market decor, so they receive the same final restoration.
        for (int x = cx - 92; x <= cx + 92; x += 23) {
            doubleDoor(level, x, y + 1, cz - 62 - 8, Direction.NORTH, Blocks.SPRUCE_DOOR,
                    material("town_vermilion_pillar"), material("xian_cloud_transom"));
            doubleDoor(level, x, y + 1, cz + 62 - 8, Direction.NORTH, Blocks.SPRUCE_DOOR,
                    material("town_vermilion_pillar"), material("xian_cloud_transom"));
        }
    }

    private static void house(Plan level, int cx, int y, int cz, int hx, int hz,
                              int height, boolean library) {
        BlockState floor = material("town_white_plaster");
        BlockState timber = material("xian_bamboo_wall");
        BlockState roofTile = material("town_red_roof_tiles");
        BlockState window = material(library ? "xian_bamboo_window" : "xian_plum_window");
        BlockState bracket = material("town_eave_bracket");
        fill(level, cx - hx, y, cz - hz, cx + hx, y, cz + hz, floor);
        level.clearInterior(cx - hx + 1, y + 1, cz - hz + 1, cx + hx - 1, y + height, cz + hz - 1);
        for (int dy = 1; dy <= height; dy++) {
            frame(level, cx - hx, y + dy, cz - hz, cx + hx, y + dy, cz + hz,
                    dy == height ? RED : timber);
        }
        roof(level, cx, y + height + 1, cz, hx + 2, hz + 2, roofTile);
        set(level, cx - hx, y + 2, cz - hz, bracket);
        set(level, cx + hx, y + 2, cz - hz, bracket);
        set(level, cx - hx, y + 2, cz + hz, bracket);
        set(level, cx + hx, y + 2, cz + hz, bracket);
        // Open doorway and windows expose an actual interior instead of a shell.
        clear(level, cx - 2, y + 1, cz - hz, cx + 2, y + 3, cz - hz);
        // Keep the doorway usable for every residence.  The earlier clear pass
        // intentionally removes the wall, but without the two block door state
        // the house still reads as an empty shell and cannot be entered through
        // a normal interaction.
        doubleDoor(level, cx, y + 1, cz - hz, Direction.NORTH, Blocks.SPRUCE_DOOR,
                material("town_rosewood"), material("xian_cloud_transom"));
        set(level, cx - hx, y + 2, cz, facing(window, Direction.WEST));
        set(level, cx + hx, y + 2, cz, facing(window, Direction.EAST));
        set(level, cx - hx + 1, y + 2, cz, LAMP);
        set(level, cx + hx - 1, y + 2, cz, material("town_red_lantern"));
        set(level, cx, y + 1, cz + 1, material("town_low_desk"));
        set(level, cx - 2, y + 1, cz + 1, FURNITURE);
        set(level, cx + 2, y + 1, cz + 1, material("town_cabinet"));
        if (library) {
            for (int z = cz - hz + 2; z <= cz + hz - 2; z += 2) {
                set(level, cx - hx + 1, y + 1, z, material("town_scroll_shelf"));
                set(level, cx + hx - 1, y + 1, z, material("town_scroll_shelf"));
            }
        } else {
            set(level, cx, y + 1, cz - 2, material("town_porcelain_planter"));
            set(level, cx + 2, y + 1, cz - 2, material("town_cabinet"));
        }
        set(level, cx - 3, y + 1, cz + hz - 2, material("xian_bamboo_screen"));
        set(level, cx + 3, y + 1, cz + hz - 2, material("xian_porcelain_vase"));
        set(level, cx - hx + 2, y + 1, cz - hz + 2, material("xian_bonsai_pine"));
        set(level, cx + hx - 2, y + 1, cz - hz + 2, material("xian_lotus_lamp"));
        set(level, cx, y + 1, cz + 1, material(library ? "xian_talisman_desk" : "xian_tea_table"));
        if (library) {
            set(level, cx + 2, y + 1, cz + hz - 3, material("xian_scroll_stand"));
            set(level, cx - 2, y + 2, cz + 1, material("xian_writing_set"));
        } else {
            set(level, cx + 2, y + 1, cz + 3, material("xian_qin_table"));
            set(level, cx - 2, y + 1, cz + 3, material("xian_meditation_cushion"));
        }
        // Every residence receives a usable sleeping corner and a ceiling lamp.
        // The four themes keep repeated blocks from feeling like copied shells.
        int theme = Math.floorMod(cx / 20 + cz / 20, 4);
        bed(level, cx + (theme < 2 ? 3 : -3), y + 1, cz - hz + 3,
                theme % 2 == 0 ? Direction.EAST : Direction.WEST);
        interiorLamp(level, cx, y + height - 1, cz);
        if (theme == 0) {
            set(level, cx - 3, y + 1, cz - 2, material("xian_wine_jars"));
            set(level, cx + 3, y + 1, cz - 2, material("xian_rice_sacks"));
            set(level, cx, y + 1, cz + hz - 2, material("xian_lotus_basin"));
        } else if (theme == 1) {
            set(level, cx - 4, y + 1, cz - 2, material("xian_herb_drying_rack"));
            set(level, cx + 4, y + 1, cz - 2, material("xian_alchemy_furnace"));
            set(level, cx, y + 1, cz + hz - 2, material("xian_bronze_incense_burner"));
        } else if (theme == 2) {
            set(level, cx - 4, y + 1, cz - 2, material("xian_sword_rack"));
            set(level, cx + 4, y + 1, cz - 2, material("xian_scripture_pedestal"));
            set(level, cx, y + 1, cz + hz - 2, material("xian_astrolabe"));
        } else {
            set(level, cx - 4, y + 1, cz - 2, material("xian_qin_table"));
            set(level, cx + 4, y + 1, cz - 2, material("xian_porcelain_vase"));
            set(level, cx, y + 1, cz + hz - 2, material("xian_bonsai_pine"));
        }
    }

    private static void pagoda(Plan level, int cx, int y, int cz, Direction entrance) {
        for (int tier = 0; tier < 4; tier++) {
            int radius = 10 - tier * 2;
            int base = y + tier * 7;
            tower(level, cx, base, cz, radius, 6, WHITE);
            if (tier == 0) towerEntrance(level, cx, base, cz, radius, entrance);
            roof(level, cx, base + 6, cz, radius + 3, radius + 3, material("town_blue_roof_tiles"));
            lamp(level, cx, base + 7, cz);
            set(level, cx - 3, base + 1, cz + 2, material("xian_pagoda_lamp"));
            set(level, cx + 3, base + 1, cz + 2, material("xian_stone_stele"));
        }
        tower(level, cx, y + 28, cz, 2, 8, GOLD);
    }

    private static void buildCanals(Plan level, int cx, int y, int cz) {
        // Two blue-green waterways give the city a Jiangnan silhouette. Their
        // crossings are raised after the water pass, so they remain walkable.
        for (int x : new int[]{cx - 38, cx + 38}) {
            fill(level, x - 3, y + 1, cz - 96, x + 3, y + 1, cz + 96, WATER);
            BlockState bank = material("town_white_jade");
            BlockState rail = material("town_jade_railing");
            fill(level, x - 5, y + 2, cz - 96, x - 4, y + 2, cz + 96, bank);
            fill(level, x + 4, y + 2, cz - 96, x + 5, y + 2, cz + 96, bank);
            for (int z = cz - 88; z <= cz + 88; z += 16) {
                set(level, x - 5, y + 3, z, facing(rail, Direction.EAST));
                set(level, x + 5, y + 3, z, facing(rail, Direction.EAST));
                set(level, x - 5, y + 3, z + 1, material("town_stone_lamp"));
                set(level, x + 5, y + 3, z + 1, material("town_stone_lamp"));
            }
            for (int z : new int[]{cz - 34, cz, cz + 34}) bridge(level, x, y + 2, z, true);
        }
        fill(level, cx - 104, y + 1, cz - 3, cx + 104, y + 1, cz + 3, WATER);
        BlockState crossBank = material("town_white_jade");
        fill(level, cx - 104, y + 2, cz - 5, cx + 104, y + 2, cz - 4, crossBank);
        fill(level, cx - 104, y + 2, cz + 4, cx + 104, y + 2, cz + 5, crossBank);
        bridge(level, cx, y + 2, cz, false);
    }

    private static void shop(Plan level, int cx, int y, int cz, int hx, int hz,
                             int height, BlockState wallState) {
        BlockState timber = material("town_rosewood");
        BlockState roofTile = material("town_red_roof_tiles");
        BlockState window = material("town_lattice_window");
        BlockState bracket = material("town_eave_bracket");
        fill(level, cx - hx, y, cz - hz, cx + hx, y, cz + hz, timber);
        level.clearInterior(cx - hx + 1, y + 1, cz - hz + 1, cx + hx - 1, y + height, cz + hz - 1);
        for (int dy = 1; dy <= height; dy++) {
            frame(level, cx - hx, y + dy, cz - hz, cx + hx, y + dy, cz + hz, wallState);
        }
        roof(level, cx, y + height + 1, cz, hx + 2, hz + 2, roofTile);
        clear(level, cx - 2, y + 1, cz - hz, cx + 2, y + 3, cz - hz);
        doubleDoor(level, cx, y + 1, cz - hz, Direction.NORTH, Blocks.SPRUCE_DOOR,
                material("town_rosewood"), material("xian_cloud_transom"));
        set(level, cx - hx, y + 2, cz, facing(window, Direction.WEST));
        set(level, cx + hx, y + 2, cz, facing(window, Direction.EAST));
        set(level, cx - hx, y + height, cz - hz, bracket);
        set(level, cx + hx, y + height, cz - hz, bracket);
        set(level, cx - 3, y + 1, cz + hz - 1, material("town_low_desk"));
        set(level, cx + 3, y + 1, cz + hz - 1, material("town_cabinet"));
        set(level, cx - hx + 2, y + 1, cz, material("town_scroll_shelf"));
        set(level, cx + hx - 2, y + 1, cz, material("town_cabinet"));
        set(level, cx, y + 1, cz + hz, material("town_cloud_plaque"));
        set(level, cx, y + 2, cz + hz, material("town_red_lantern"));
        set(level, cx, y + 3, cz - hz - 1, LAMP);
        interiorLamp(level, cx, y + height - 1, cz);
    }

    private static void marketStall(Plan level, int cx, int y, int cz, BlockState cloth) {
        fill(level, cx - 4, y, cz - 2, cx + 4, y, cz + 2, WOOD);
        for (int x : new int[]{cx - 4, cx + 4}) {
            set(level, x, y + 1, cz - 2, WOOD_LOG);
            set(level, x, y + 1, cz + 2, WOOD_LOG);
            set(level, x, y + 2, cz - 2, cloth);
            set(level, x, y + 2, cz + 2, cloth);
        }
        fill(level, cx - 4, y + 3, cz - 2, cx + 4, y + 3, cz + 2, cloth);
        set(level, cx - 2, y + 1, cz, material("town_low_desk"));
        set(level, cx + 2, y + 1, cz, material("town_cabinet"));
        set(level, cx, y + 1, cz, material("town_red_lantern"));
        String[] goods = {"xian_rice_sacks", "xian_fruit_basket", "xian_wine_jars", "xian_silk_display"};
        set(level, cx - 2, y + 1, cz, material("xian_abacus_counter"));
        set(level, cx + 2, y + 1, cz, material(goods[Math.floorMod(cx / 14 + cz, goods.length)]));
    }

    /** Six trades with distinct stock, rather than the same cabinet in every shop. */
    private static void furnishShop(Plan level, int cx, int y, int cz, int hx, int hz, int trade) {
        String[][] stock = {
                {"xian_apothecary_drawers", "xian_herb_drying_rack", "xian_alchemy_furnace"},
                {"xian_tea_table", "xian_qin_table", "xian_porcelain_vase"},
                {"xian_talisman_desk", "xian_scroll_stand", "xian_writing_set"},
                {"xian_silk_display", "xian_bamboo_screen", "xian_bonsai_pine"},
                {"xian_rice_sacks", "xian_wine_jars", "xian_fruit_basket"},
                {"xian_sword_rack", "xian_spirit_cauldron", "xian_spirit_crystal_lamp"}
        };
        set(level, cx - hx + 2, y + 1, cz + hz - 2, material(stock[trade][0]));
        set(level, cx + hx - 2, y + 1, cz + hz - 2, material(stock[trade][1]));
        set(level, cx + hx - 2, y + 1, cz, material(stock[trade][2]));
        set(level, cx - 3, y + 1, cz + hz - 1, material("xian_abacus_counter"));
        set(level, cx - 3, y + 3, cz - hz, material("xian_moon_window"));
        set(level, cx + 3, y + 3, cz - hz, material("xian_plum_window"));
        set(level, cx, y + 4, cz - hz, material("xian_cloud_transom"));
        set(level, cx - 3, y + 2, cz + hz - 1, material("xian_wall_sconce"));
        set(level, cx + 3, y + 3, cz - hz + 1, material("xian_palace_lantern"));
        // A back-room vignette makes the shop readable from the doorway.
        set(level, cx - hx + 3, y + 1, cz - hz + 2, material("xian_bamboo_screen"));
        set(level, cx + hx - 3, y + 1, cz - hz + 2, material("xian_porcelain_vase"));
        set(level, cx, y + 1, cz - hz + 2, material(trade == 0 ? "xian_herb_drying_rack"
                : trade == 1 ? "xian_qin_table" : trade == 2 ? "xian_writing_set"
                : trade == 3 ? "xian_silk_display" : trade == 4 ? "xian_wine_jars"
                : "xian_sword_rack"));
    }

    private static void courtyard(Plan level, int cx, int y, int cz, int r) {
        circle(level, cx, y, cz, r, WHITE);
        ring(level, cx, y + 1, cz, r, 1, COPPER);
        circle(level, cx, y + 1, cz, 2, WATER);
        set(level, cx, y + 2, cz, material("town_stone_lamp"));
        set(level, cx - 4, y + 1, cz, material("town_bench"));
        set(level, cx + 4, y + 1, cz, material("town_bench"));
        set(level, cx, y + 1, cz + 4, material("town_porcelain_planter"));
        set(level, cx - 3, y + 1, cz + 3, material("xian_tea_table"));
        set(level, cx + 3, y + 1, cz + 3, material("xian_lotus_basin"));
        set(level, cx + 3, y + 1, cz - 3, material("xian_crane_statue"));
        set(level, cx - 3, y + 1, cz - 3, material("xian_garden_lamp"));
        for (int dx = -r + 2; dx <= r - 2; dx += 4) {
            bamboo(level, cx + dx, y + 1, cz - r + 1);
            bamboo(level, cx + dx, y + 1, cz + r - 1);
        }
    }

    private static void garden(Plan level, int cx, int y, int cz) {
        circle(level, cx, y, cz, 7, Blocks.MOSS_BLOCK.defaultBlockState());
        ring(level, cx, y + 1, cz, 7, 1, material("town_jade_railing"));
        for (int dx = -3; dx <= 3; dx += 3) {
            bamboo(level, cx + dx, y + 1, cz);
            set(level, cx + dx, y + 3, cz, LEAVES);
            set(level, cx + dx, y + 1, cz + 2, material("town_porcelain_planter"));
        }
        set(level, cx, y + 1, cz, WATER);
        set(level, cx, y + 2, cz, material("town_stone_lamp"));
        set(level, cx - 4, y + 1, cz - 3, material("xian_stone_stele"));
        set(level, cx + 4, y + 1, cz - 3, material("xian_crane_statue"));
        set(level, cx - 4, y + 1, cz + 3, material("xian_lotus_basin"));
        set(level, cx + 4, y + 1, cz + 3, material("xian_garden_lamp"));
    }

    private static void bridge(Plan level, int cx, int y, int cz, boolean alongZ) {
        for (int i = -10; i <= 10; i++) {
            int crown = y + Math.max(0, 2 - Math.abs(i) / 5);
            if (alongZ) fill(level, cx + i, crown, cz - 4, cx + i, crown, cz + 4, WHITE);
            else fill(level, cx - 4, crown, cz + i, cx + 4, crown, cz + i, WHITE);
        }
        for (int i = -8; i <= 8; i += 4) {
            if (alongZ) lamp(level, cx + i, y + 3, cz);
            else lamp(level, cx, y + 3, cz + i);
        }
        BlockState rail = material("town_jade_railing");
        if (alongZ) {
            for (int i = -8; i <= 8; i += 4) {
                set(level, cx + i, y + 3, cz - 4, rail);
                set(level, cx + i, y + 3, cz + 4, rail);
            }
        } else {
            for (int i = -8; i <= 8; i += 4) {
                set(level, cx - 4, y + 3, cz + i, facing(rail, Direction.EAST));
                set(level, cx + 4, y + 3, cz + i, facing(rail, Direction.EAST));
            }
        }
    }

    private static void bamboo(Plan level, int x, int y, int z) {
        set(level, x, y, z, BAMBOO);
        set(level, x, y + 1, z, BAMBOO);
    }

    private static void watchTower(Plan level, int x, int y, int z, Direction entrance) {
        tower(level, x, y, z, 6, 14, WALL);
        towerEntrance(level, x, y, z, 6, entrance);
        roof(level, x, y + 14, z, 8, 8, RED_SLAB);
        set(level, x, y + 16, z, LAMP);
    }

    /** Cut a narrow, lit entrance through a circular landmark tower. */
    private static void towerEntrance(Plan level, int x, int y, int z, int radius,
                                      Direction facing) {
        int edgeX = x + facing.getStepX() * radius;
        int edgeZ = z + facing.getStepZ() * radius;
        if (facing.getAxis() == Direction.Axis.Z) {
            clear(level, x - 2, y + 1, edgeZ, x + 2, y + 3, edgeZ);
        } else {
            clear(level, edgeX, y + 1, z - 2, edgeX, y + 3, z + 2);
        }
        doubleDoor(level, edgeX, y + 1, edgeZ, facing, Blocks.SPRUCE_DOOR,
                material("town_white_jade"), material("xian_dragon_relief"));
        interiorLamp(level, x, y + 5, z);
    }

    private static void tower(Plan level, int x, int y, int z, int radius, int height, BlockState state) {
        circle(level, x, y, z, radius, state);
        for (int dy = 1; dy < height; dy++) {
            ring(level, x, y + dy, z, radius, 1, state);
            clear(level, x - radius + 1, y + dy, z - radius + 1,
                    x + radius - 1, y + dy, z + radius - 1);
        }
    }

    private static void roof(Plan level, int x, int y, int z, int hx, int hz, BlockState state) {
        for (int layer = 0; layer < 3; layer++) {
            int ax = Math.max(2, hx - layer * 2);
            int az = Math.max(2, hz - layer * 2);
            fill(level, x - ax, y + layer, z - az, x + ax, y + layer, z + az, state);
        }
        BlockState ridge = material("town_roof_ridge");
        set(level, x, y + 3, z, ridge);
        set(level, x - Math.max(2, hx - 2), y + 1, z - Math.max(2, hz - 2), material("town_ridge_finial"));
        set(level, x + Math.max(2, hx - 2), y + 1, z + Math.max(2, hz - 2), material("town_ridge_finial"));
        for (int dx = -hx + 2; dx <= hx - 2; dx += 4) {
            set(level, x + dx, y - 1, z - hz, material("xian_sunset_eave"));
            set(level, x + dx, y - 1, z + hz, facing(material("xian_sunset_eave"), Direction.SOUTH));
        }
        for (int side : new int[]{-1, 1}) {
            set(level, x + side * Math.max(2, hx - 4), y + 3, z, material("xian_phoenix_ridge"));
            set(level, x + side * hx, y + 1, z - hz, material("xian_bronze_corner_beast"));
            set(level, x + side * hx, y + 1, z + hz, facing(material("xian_bronze_corner_beast"), Direction.SOUTH));
        }
    }

    private static void frame(Plan level, int minX, int y, int minZ,
                              int maxX, int ignoredY, int maxZ, BlockState state) {
        fill(level, minX, y, minZ, maxX, y, minZ, state);
        fill(level, minX, y, maxZ, maxX, y, maxZ, state);
        fill(level, minX, y, minZ, minX, y, maxZ, state);
        fill(level, maxX, y, minZ, maxX, y, maxZ, state);
    }

    private static void wall(Plan level, int minX, int minY, int minZ,
                             int maxX, int maxY, int maxZ, BlockState state) {
        if (minX == maxX) fill(level, minX, minY, minZ, maxX, maxY, maxZ, state);
        else if (minZ == maxZ) fill(level, minX, minY, minZ, maxX, maxY, maxZ, state);
    }

    private static void lamp(Plan level, int x, int y, int z) {
        set(level, x, y, z, COPPER);
        set(level, x, y + 1, z, COPPER);
        set(level, x, y + 2, z, LANTERN);
    }

    private static void interiorLamp(Plan level, int x, int y, int z) {
        set(level, x, y, z, material("xian_spirit_crystal_lamp"));
    }

    private static void door(Plan level, int x, int y, int z, Direction facing,
                             Block doorBlock) {
        BlockState lower = doorBlock.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.OPEN, false);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        set(level, x, y, z, lower);
        set(level, x, y + 1, z, upper);
    }

    /**
     * A two-block-wide entrance with a real pair of interactive door leaves.
     * The centre leaf is kept at the supplied coordinate so existing inspection
     * markers and navigation points remain valid.  The surrounding frame is
     * deliberately made from the town's custom oriental blocks rather than
     * trying to turn a decorative block into a new block entity.
     */
    private static void doubleDoor(Plan level, int x, int y, int z, Direction facing,
                                   Block doorBlock, BlockState frame, BlockState lintel) {
        Direction side = facing.getClockWise();
        int sx = side.getStepX();
        int sz = side.getStepZ();
        BlockState lower = doorBlock.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.OPEN, false);
        BlockState leftLower = lower.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
        BlockState rightLower = lower.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(level, x - sx, y, z - sz, leftLower);
        set(level, x - sx, y + 1, z - sz, leftLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(level, x, y, z, rightLower);
        set(level, x, y + 1, z, rightLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // A three-block-high frame gives a monumental opening even when the
        // building behind it is much wider than a normal Minecraft doorway.
        for (int dy = 0; dy <= 2; dy++) {
            set(level, x - sx * 2, y + dy, z - sz * 2, frame);
            set(level, x + sx, y + dy, z + sz, frame);
        }
        for (int offset = -2; offset <= 1; offset++) {
            set(level, x + sx * offset, y + 3, z + sz * offset, lintel);
        }
        set(level, x - sx * 2, y + 3, z - sz * 2,
                material("xian_palace_lantern"));
        set(level, x + sx, y + 3, z + sz,
                material("xian_palace_lantern"));
        set(level, x, y + 4, z, material("town_cloud_plaque"));
    }

    private static void bed(Plan level, int x, int y, int z, Direction facing) {
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.OCCUPIED, false);
        BlockPos headPos = new BlockPos(x, y, z).relative(facing);
        BlockState head = foot.setValue(BedBlock.PART, BedPart.HEAD);
        set(level, x, y, z, foot);
        set(level, headPos.getX(), headPos.getY(), headPos.getZ(), head);
    }

    private static void fill(Plan level, int minX, int minY, int minZ,
                             int maxX, int maxY, int maxZ, BlockState state) {
        level.add(minX, minY, minZ, maxX, maxY, maxZ, state);
    }

    private static void clear(Plan level, int minX, int minY, int minZ,
                              int maxX, int maxY, int maxZ) {
        fill(level, minX, minY, minZ, maxX, maxY, maxZ, Blocks.AIR.defaultBlockState());
    }

    private static void circle(Plan level, int cx, int y, int cz, int radius, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void ring(Plan level, int cx, int y, int cz, int radius,
                             int thickness, BlockState state) {
        int inner = Math.max(0, radius - thickness);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance <= radius * radius && distance >= inner * inner) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void set(Plan level, int x, int y, int z, BlockState state) {
        level.add(x, y, z, x, y, z, state);
    }

    private static BlockState material(String id) {
        return OrientalBlocks.state(id);
    }

    private static BlockState facing(BlockState state, Direction direction) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.setValue(HorizontalDirectionalBlock.FACING, direction) : state;
    }

    record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}

    static final class Plan {
        final List<Placement> placements = new ArrayList<>();
        final List<Placement> interiorClearances = new ArrayList<>();
        int minX, minY, minZ, maxX, maxY, maxZ;

        void clearInterior(int x1, int y1, int z1, int x2, int y2, int z2) {
            interiorClearances.add(new Placement(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState()));
        }

        void add(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            Placement p = new Placement(Math.min(x1,x2), Math.min(y1,y2), Math.min(z1,z2),
                    Math.max(x1,x2), Math.max(y1,y2), Math.max(z1,z2), state);
            placements.add(p);
            minX = Math.min(minX,p.minX()); maxX = Math.max(maxX,p.maxX());
            minY = Math.min(minY,p.minY()); maxY = Math.max(maxY,p.maxY());
            minZ = Math.min(minZ,p.minZ()); maxZ = Math.max(maxZ,p.maxZ());
        }
    }
}
