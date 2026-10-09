package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;

/** Standalone, walkable cloud palace for visual acceptance before cave-heaven placement. */
public final class DaotaiResidenceGenerator {
    public static final int VERSION = 4;
    public static final int MIN_X = -220, MAX_X = 220;
    public static final int MIN_Z = -210, MAX_Z = 210;
    public static final int MIN_Y = -2, MAX_Y = 222;
    public static final int PALACE_Y = 74, PALACE_Z = -92;

    private static final BlockState JADE = OrientalBlocks.state("town_white_jade");
    private static final BlockState QUARTZ = Blocks.QUARTZ_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState BLUE = Blocks.DARK_PRISMARINE.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    private DaotaiResidenceGenerator() {}

    public record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                            BlockState state) {}

    public static final class Plan {
        final List<Placement> placements = new ArrayList<>();
        final List<SiteClearance.Region> clearances = new ArrayList<>(List.of(
                new SiteClearance.Region(MIN_X,1,MIN_Z,MAX_X,MAX_Z)));
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void add(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            Placement op = new Placement(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), state);
            if (op.minX() < MIN_X || op.maxX() > MAX_X || op.minZ() < MIN_Z
                    || op.maxZ() > MAX_Z || op.minY() < MIN_Y || op.maxY() > MAX_Y) {
                throw new IllegalArgumentException("Dao-Tai placement outside reserved site: " + op);
            }
            placements.add(op);
            minX = Math.min(minX, op.minX()); maxX = Math.max(maxX, op.maxX());
            minY = Math.min(minY, op.minY()); maxY = Math.max(maxY, op.maxY());
            minZ = Math.min(minZ, op.minZ()); maxZ = Math.max(maxZ, op.maxZ());
        }
    }

    public static Plan createPlan() {
        Plan p = new Plan();
        buildCloudCourts(p);
        buildPalaces(p);
        buildHalo(p);
        buildGardens(p);
        buildRoutes(p);
        return p;
    }

    public static boolean generate(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null || level.dimension() != Level.OVERWORLD) return false;
        Plan plan = createPlan();
        if ((long) origin.getY() + MIN_Y < level.getMinBuildHeight()
                || (long) origin.getY() + MAX_Y >= level.getMaxBuildHeight()) return false;
        for (int x : new int[] {MIN_X, MAX_X}) for (int z : new int[] {MIN_Z, MAX_Z}) {
            if (!level.getWorldBorder().isWithinBounds(origin.offset(x, 0, z))) return false;
        }
        return DaotaiResidenceConstruction.start(level, origin, plan);
    }

    public static boolean isGenerated(ServerLevel level, BlockPos origin) {
        return level != null && origin != null && DaotaiResidenceConstruction.data(level).contains(origin);
    }

    private static void buildCloudCourts(Plan p) {
        // Thin, complete decks carry decorative models instead of stacking non-full blocks.
        int[] radii = {182, 134, 86}, heights = {28, 48, 66};
        for (int tier = 0; tier < 3; tier++) {
            int r = radii[tier], y = heights[tier];
            annulus(p, 0, y - 3, 0, r + 8, r - 8, QUARTZ);
            annulus(p, 0, y - 2, 0, r + 8, r - 8, JADE);
            annulus(p, 0, y, 0, r + 8, r - 8, JADE);
            annulus(p, 0, y - 1, 0, r + 9, r + 8, GOLD);
            ringRail(p, 0, y + 1, 0, r + 8, true);
            ringRail(p, 0, y + 1, 0, r - 8, true);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int x = (int) Math.round(Math.sin(a) * r);
                int z = (int) Math.round(Math.cos(a) * r);
                if (Math.abs(x) < 22) continue;
                set(p, x, y + 1, z, decor("sky_floor", tier * 5 + i));
                set(p, x - 5, y + 1, z, decor("lamp", tier * 4 + i));
                set(p, x + 5, y + 1, z, decor("lamp", tier * 4 + i + 8));
            }
        }
        // Ten inhabited floating islands punctuate the outer ring.
        for (int i = 0; i < 10; i++) {
            double a = (i + 0.5) * Math.PI * 2 / 10;
            int x = (int) Math.round(Math.sin(a) * 182);
            int z = (int) Math.round(Math.cos(a) * 182);
            island(p, x, 28, z, 23, 22, i);
            pavilion(p, x, 28, z, 9, 8, 14, i);
            pine(p, x + 14, 29, z + 6, i);
        }
        // Four cardinal sanctuaries on the middle tier.
        for (int side : new int[] {-1, 1}) {
            island(p, side * 134, 48, 0, 24, 26, side + 3);
            pavilion(p, side * 134, 48, -8, 11, 10, 18, side + 7);
            island(p, side * 88, 66, -28, 20, 28, side + 11);
            pavilion(p, side * 88, 66, -28, 10, 9, 18, side + 9);
            // Framing observatories reach the full 441-block width.
            // Keep the outer pagoda and its gold eave trim inside the reserved 441-block site.
            island(p, side * 199, 28, -30, 20, 24, side + 6);
            pagoda(p, side * 199, 28, -30, 9, 4, side + 12);
        }
        island(p, 0, 48, -182, 27, 30, 8);
        pavilion(p, 0, 48, -182, 15, 11, 18, 12);
        island(p, 0, 74, PALACE_Z, 70, 38, 3);
        for (int side : new int[] {-1, 1}) {
            island(p, side * 112, 74, -86, 36, 35, side + 5);
        }
        terrace(p, 0, 38, 158, 24);
        terrace(p, 0, 57, 110, 23);
        terrace(p, 0, 71, 52, 25);
        starPool(p, -14, 38, 158, 7, 1);
        starPool(p, 14, 57, 110, 7, 6);
        starPool(p, -14, 71, 52, 7, 11);
        fill(p, -12, -2, 203, 12, 0, 210, JADE);
    }

    private static void buildPalaces(Plan p) {
        hall(p, 0, PALACE_Y, PALACE_Z, 44, 30, 24, 0);
        // Three real halls, balconies and swept eaves form the central silhouette.
        hall(p, 0, 113, PALACE_Z, 26, 19, 15, 4);
        hall(p, 0, 143, PALACE_Z, 16, 12, 12, 8);
        spire(p, 0, 172, PALACE_Z, 24, 0);
        for (int side : new int[] {-1, 1}) {
            hall(p, side * 112, PALACE_Y, -86, 24, 19, 18, side < 0 ? 1 : 2);
            pagoda(p, side * 57, 74, -146, 10, 6, side + 6);
            pagoda(p, side * 53, 74, -42, 8, 4, side + 10);
            // Narrow aerial minarets frame the halo from a distance.
            column(p, side * 65, 75, -130, 131, 2, side + 10);
            spire(p, side * 65, 132, -130, 33, side + 10);
        }
        // Monumental gate frames remain open throughout the central ascent.
        gate(p, 0, 0, 205, 10, 16, 1);
        gate(p, 0, 28, 181, 11, 19, 5);
        gate(p, 0, 66, 85, 12, 24, 9);
        gate(p, 0, 74, -49, 13, 21, 13);
    }

    private static void buildHalo(Plan p) {
        // A vertical gold mandala behind the main palace is the distant focal point.
        int cy = 160, z = -150;
        for (int radius : new int[] {48, 42, 34}) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int d = dx * dx + dy * dy;
                    if (d <= radius * radius && d >= (radius - 2) * (radius - 2)) {
                        set(p, dx, cy + dy, z, radius == 42 ? LIGHT : GOLD);
                    }
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            int x = (int) Math.round(Math.sin(a) * 51);
            int y = cy + (int) Math.round(Math.cos(a) * 51);
            set(p, x, y, z, decor("formation", i));
            set(p, x, y, z + 1, decor("lamp", i));
        }
        // Delicate axial crown rather than a massive solid central pillar.
        fill(p, -1, 209, z, 1, 218, z, GOLD);
        set(p, 0, 219, z, LIGHT);
        fill(p, 0, 220, z, 0, MAX_Y, z, Blocks.END_ROD.defaultBlockState());
    }

    private static void buildGardens(Plan p) {
        for (int side : new int[] {-1, 1}) {
            for (int z : new int[] {-128, -58}) {
                int x = side * 112;
                terrace(p, x, 74, z, 18);
                starPool(p, x, 74, z, 10, side + z);
                pine(p, x - 13, 75, z + 6, z);
                pine(p, x + 12, 75, z - 5, z + 3);
                for (int i = 0; i < 4; i++) {
                    set(p, x - 12 + i * 8, 75, z - 13, decor("garden", i + z));
                    set(p, x - 12 + i * 8, 75, z + 13, decor("lamp", i + z));
                }
            }
        }
        // Falls start in sealed, decorative spillways and terminate in a contained basin.
        for (int side : new int[] {-1, 1}) {
            waterfall(p, side * 62, 74, -100, 36, 4);
            waterfall(p, side * 112, 74, -114, 42, 3);
            waterfall(p, side * 134, 48, 16, 18, 3);
            waterfall(p, side * 199, 28, -22, 4, 3);
        }
    }

    private static void buildRoutes(Plan p) {
        // Build circulation last, so later decoration cannot silently close an entrance.
        for (int z = 210; z >= -121; z--) {
            int y = axisHeight(z), next = axisHeight(z - 1);
            fill(p, -7, y - 2, z, 7, y, z, JADE);
            fill(p, -7, y + 1, z, 7, y + 6, z, AIR);
            if (next > y) fill(p, -7, y + 1, z, 7, y + 1, z,
                    Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            if (z % 8 == 0 && z > -55) {
                for (int x : new int[] {-9, 9}) {
                    set(p, x, y + 1, z, decor("pillar", z / 8));
                    set(p, x, y + 2, z, decor("lamp", z / 8 + x));
                }
            }
        }
        for (int side : new int[] {-1, 1}) {
            // Cardinal bridges descend from the palace terrace to all three rings.
            for (int distance = 12; distance <= 182; distance++) {
                int x = side * distance, y = axisHeight(distance);
                fill(p, x, y - 2, -3, x, y, 3, JADE);
                fill(p, x, y + 1, -3, x, y + 5, 3, AIR);
                if (axisHeight(distance - 1) > y) fill(p, x, y + 1, -3, x, y + 1, 3,
                        Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,
                                side < 0 ? Direction.EAST : Direction.WEST));
                if (distance % 8 == 0) {
                    for (int z : new int[] {-5, 5}) set(p, x, y + 1, z, decor("lamp", distance / 8 + z));
                }
            }
            straightBridge(p, side * 12, 74, -58, side * 112, -58, 4);
            straightBridge(p, side * 112, 74, -58, side * 112, -86, 4);
            // The stairs stay outside the swept eaves; landings meet side entrances at floor level.
            straightBridge(p, 0, 74, -14, side * 54, -14, 2);
            straightBridge(p, side * 54, 113, PALACE_Z, side * 23, PALACE_Z, 2);
            straightBridge(p, side * 42, 113, PALACE_Z, side * 42, -32, 2);
            straightBridge(p, side * 42, 113, -32, side * 34, -32, 2);
            straightBridge(p, side * 34, 143, PALACE_Z, side * 13, PALACE_Z, 2);
            // Reopen graded approaches after the flat landing decks have been placed.
            ascent(p, side * 54, 74, -14, side * 54, 113, PALACE_Z, 2);
            ascent(p, side * 34, 113, -32, side * 34, 143, PALACE_Z, 2);
        }
    }

    public static int axisHeight(int z) {
        if (z >= 182) return Math.max(0, Math.min(28, 210 - z));
        if (z >= 134) return 28 + (182 - z) * 20 / 48;
        if (z >= 86) return 48 + (134 - z) * 18 / 48;
        if (z >= 38) return 66 + (86 - z) * 8 / 48;
        return PALACE_Y;
    }

    private static void hall(Plan p, int cx, int y, int cz, int hx, int hz, int h, int kind) {
        fill(p, cx - hx - 3, y - 2, cz - hz - 3, cx + hx + 3, y, cz + hz + 3, QUARTZ);
        fill(p, cx - hx, y, cz - hz, cx + hx, y, cz + hz, JADE);
        fill(p, cx - hx + 1, y + 1, cz - hz + 1, cx + hx - 1, y + h - 1, cz + hz - 1, AIR);
        for (int z : new int[] {cz - hz, cz + hz}) {
            fill(p, cx - hx, y + 1, z, cx + hx, y + 4, z, QUARTZ);
            fill(p, cx - hx, y + 5, z, cx + hx, y + h - 2, z, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            fill(p, cx - hx, y + h - 1, z, cx + hx, y + h, z, JADE);
            for (int x = cx - hx; x <= cx + hx; x += 8) {
                column(p, x, y + 1, z, y + h, 0, kind + x);
                set(p, x, y + 5, z + (z < cz ? 1 : -1), decor("gate", kind + x));
            }
        }
        for (int x : new int[] {cx - hx, cx + hx}) {
            fill(p, x, y + 1, cz - hz, x, y + 4, cz + hz, QUARTZ);
            fill(p, x, y + 5, cz - hz, x, y + h - 2, cz + hz, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            for (int z = cz - hz; z <= cz + hz; z += 8) column(p, x, y + 1, z, y + h, 0, z + kind);
        }
        // Open portal, decorated leaves on each side, with no tiny vanilla door.
        for (int z : new int[] {cz - hz, cz + hz}) {
            fill(p, cx - 7, y + 1, z - 1, cx + 7, y + 10, z + 1, AIR);
            for (int x : new int[] {cx - 8, cx + 8}) {
                column(p, x, y + 1, z, y + 12, 0, x + kind);
                for (int dy = 1; dy <= 8; dy++) set(p, x, y + dy, z + 1, decor("gate", kind + dy));
            }
            fill(p, cx - 8, y + 11, z, cx + 8, y + 12, z, JADE);
        }
        for (int x : new int[] {cx - hx, cx + hx}) {
            fill(p, x - 1, y + 1, cz - 4, x + 1, y + 7, cz + 4, AIR);
        }
        // Low light sources cover the occupied floor as well as the lofty ceiling.
        for (int x = cx - hx + 5; x < cx + hx; x += 10) for (int z = cz - hz + 5; z < cz + hz; z += 10) {
            set(p, x, y, z, LIGHT);
            if (Math.abs(x - cx) > 9) {
                set(p, x, y + 1, z, decor("lamp", x + z + kind));
                set(p, x + 2, y + 1, z + 1, decor("sky_floor", x + z));
            }
        }
        roof(p, cx, y + h + 1, cz, hx + 5, hz + 5, kind);
        furnish(p, cx, y, cz, hx, hz, kind);
    }

    private static void furnish(Plan p, int cx, int y, int cz, int hx, int hz, int kind) {
        int off = Math.min(hx - 7, Math.max(11, hx / 2));
        for (int side : new int[] {-1, 1}) {
            int x = cx + side * off;
            // Each wing has a separate purpose: scripture, alchemy, tea and meditation.
            for (int i = 0; i < 5; i++) {
                int z = cz - hz + 5 + i * Math.max(2, (2 * hz - 10) / 5);
                set(p, x, y + 1, z, decor("furniture", kind * 4 + i + (side < 0 ? 0 : 10)));
                set(p, x + side * 3, y + 1, z, OrientalBlocks.state(side < 0
                        ? "town_scroll_shelf" : "xian_talisman_desk"));
                set(p, x - side * 2, y + 1, z, OrientalBlocks.state("xian_meditation_cushion"));
            }
            int screenZ = cz + hz / 2;
            for (int dx = -4; dx <= 4; dx++) set(p, x + dx, y + 1, screenZ,
                    OrientalBlocks.state("town_folding_screen"));
            set(p, x, y + 1, cz + hz - 5, OrientalBlocks.state(kind % 2 == 0
                    ? "xian_qin_table" : "xian_alchemy_furnace"));
            set(p, x + side * 4, y + 1, cz + hz - 5, OrientalBlocks.state("xian_bronze_incense_burner"));
            set(p, x, y + 1, cz - hz + 5, decor("garden", kind + side));
            set(p, x + side * 4, y + 1, cz - hz + 5, decor("formation", kind + side));
        }
        // The rear seat sits outside the walk-through aisle.
        set(p, cx - 10, y + 1, cz - hz + 4, OrientalBlocks.state("town_throne"));
        set(p, cx + 10, y + 1, cz - hz + 4, OrientalBlocks.state("xian_scripture_pedestal"));
    }

    private static void pavilion(Plan p, int x, int y, int z, int hx, int hz, int h, int variant) {
        fill(p, x - hx, y, z - hz, x + hx, y, z + hz, JADE);
        for (int dx : new int[] {-hx + 1, hx - 1}) for (int dz : new int[] {-hz + 1, hz - 1}) {
            column(p, x + dx, y + 1, z + dz, y + h, 0, variant + dx + dz);
        }
        roof(p, x, y + h, z, hx + 4, hz + 4, variant);
        set(p, x - 4, y + 1, z, decor("furniture", variant));
        set(p, x + 4, y + 1, z, OrientalBlocks.state("xian_tea_table"));
        set(p, x, y + 1, z - 4, decor("formation", variant));
        for (int dx : new int[] {-hx + 2, hx - 2}) for (int dz : new int[] {-hz + 2, hz - 2}) {
            set(p, x + dx, y + 1, z + dz, decor("lamp", variant + dx + dz));
        }
    }

    private static void pagoda(Plan p, int x, int y, int z, int radius, int tiers, int variant) {
        for (int tier = 0; tier < tiers; tier++) {
            int base = y + tier * 14, r = Math.max(4, radius - tier);
            fill(p, x - r, base, z - r, x + r, base, z + r, JADE);
            for (int dx : new int[] {-r + 1, r - 1}) for (int dz : new int[] {-r + 1, r - 1}) {
                column(p, x + dx, base + 1, z + dz, base + 8, 0, tier + variant);
            }
            roof(p, x, base + 8, z, r + 3, r + 3, variant + tier);
            set(p, x - 3, base + 1, z, decor("lamp", tier + variant));
            set(p, x + 3, base + 1, z, decor("furniture", tier + variant));
        }
        spire(p, x, y + tiers * 14, z, 15, variant);
    }

    private static void roof(Plan p, int cx, int y, int cz, int hx, int hz, int variant) {
        // Thin hipped roof strips, a continuous tiled surface and swept-up eave corners.
        int rise = Math.max(4, Math.min(12, hz / 2));
        for (int z = -hz; z <= hz; z++) for (int x = -hx; x <= hx; x++) {
            int edge = Math.min(hx - Math.abs(x), hz - Math.abs(z));
            int roofY = y + Math.min(rise, edge * rise / Math.max(1, hz));
            boolean rim = edge == 0;
            set(p, cx + x, roofY, cz + z, rim ? GOLD : BLUE);
        }
        fill(p, cx - Math.max(0, hx - hz), y + rise + 1, cz,
                cx + Math.max(0, hx - hz), y + rise + 1, cz, GOLD);
        for (int side : new int[] {-1, 1}) for (int end : new int[] {-1, 1}) {
            for (int i = 0; i < 4; i++) {
                set(p, cx + side * (hx - i), y + 4 - i, cz + end * (hz - i), GOLD);
            }
            set(p, cx + side * hx, y + 5, cz + end * hz, decor("roof", variant + side + end));
        }
        for (int x = -hx + 3; x <= hx - 3; x += 6) {
            for (int z : new int[] {-hz + 1, hz - 1}) {
                set(p, cx + x, y - 1, cz + z, OrientalBlocks.state("town_eave_bracket"));
                set(p, cx + x, y - 2, cz + z, decor("lamp", variant + x));
            }
        }
        set(p, cx, y + rise + 2, cz, decor("roof", variant));
    }

    private static void column(Plan p, int x, int y, int z, int top, int half, int variant) {
        fill(p, x - half, y, z - half, x + half, top, z + half, PILLAR);
        fill(p, x - half - 1, y, z - half - 1, x + half + 1, y, z + half + 1, JADE);
        set(p, x, top + 1, z, decor("pillar", variant));
    }

    private static void spire(Plan p, int x, int y, int z, int h, int variant) {
        fill(p, x - 1, y, z - 1, x + 1, y + 4, z + 1, GOLD);
        fill(p, x, y + 5, z, x, y + h - 2, z, GOLD);
        set(p, x, y + h - 1, z, decor("lamp", variant));
        set(p, x, y + h, z, Blocks.END_ROD.defaultBlockState());
    }

    private static void gate(Plan p, int x, int y, int z, int half, int h, int variant) {
        for (int side : new int[] {-1, 1}) {
            column(p, x + side * half, y, z, y + h, 1, variant + side);
        }
        fill(p, x - half, y + h - 2, z - 1, x + half, y + h, z + 1, JADE);
        roof(p, x, y + h + 1, z, half + 3, 5, variant);
        for (int dx = -half + 2; dx <= half - 2; dx += 3) {
            set(p, x + dx, y + h - 3, z, decor("gate", variant + dx));
        }
    }

    private static void island(Plan p, int x, int y, int z, int r, int depth, int variant) {
        for (int drop = depth; drop >= 4; drop -= 3) {
            int radius = Math.max(3, r * (depth - drop + 4) / depth);
            disk(p, x, y - drop, z, radius, 3, (drop / 3 + variant) % 3 == 0
                    ? Blocks.TUFF.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
        }
        disk(p, x, y - 3, z, r, 3, QUARTZ);
        disk(p, x, y, z, r, 1, JADE);
        annulus(p, x, y - 1, z, r + 1, r, GOLD);
        ringRail(p, x, y + 1, z, r, false);
    }

    private static void terrace(Plan p, int x, int y, int z, int r) {
        disk(p, x, y - 2, z, r, 3, JADE);
        annulus(p, x, y - 1, z, r + 1, r, GOLD);
        ringRail(p, x, y + 1, z, r, true);
    }

    private static void starPool(Plan p, int x, int y, int z, int r, int variant) {
        disk(p, x, y, z, r, 1, BLUE);
        annulus(p, x, y + 1, z, r, r - 1, JADE);
        disk(p, x, y + 1, z, r - 2, 1, WATER);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int dx = (int) Math.round(Math.sin(a) * (r - 2));
            int dz = (int) Math.round(Math.cos(a) * (r - 2));
            set(p, x + dx, y, z + dz, LIGHT);
        }
        fill(p, x, y + 1, z, x, y + 2, z, JADE);
        set(p, x, y + 3, z, decor("formation", variant));
        set(p, x + 2, y + 2, z, decor("garden", variant));
    }

    private static void pine(Plan p, int x, int y, int z, int variant) {
        disk(p, x, y - 1, z, 5, 1, Blocks.MOSS_BLOCK.defaultBlockState());
        fill(p, x, y, z, x, y + 11, z, Blocks.SPRUCE_LOG.defaultBlockState());
        fill(p, x, y + 7, z, x + 4, y + 7, z + 2, Blocks.SPRUCE_LOG.defaultBlockState());
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(
                net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        disk(p, x + 2, y + 8, z + 1, 6, 2, leaves);
        disk(p, x - 1, y + 12, z, 5, 2, leaves);
        set(p, x - 3, y, z + 3, decor("garden", variant));
    }

    private static void waterfall(Plan p, int x, int y, int z, int bottom, int half) {
        fill(p, x - half - 1, bottom - 1, z - 2, x + half + 1, bottom - 1, z + 2, JADE);
        fill(p, x - half - 1, bottom, z - 2, x + half + 1, bottom, z + 2,
                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        fill(p, x - half, bottom + 1, z - 1, x + half, y + 1, z + 1,
                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        fill(p, x - half + 1, bottom + 1, z, x + half - 1, y, z, WATER);
        set(p, x - half, y + 2, z, decor("lamp", half + x));
        set(p, x + half, y + 2, z, decor("garden", half + x));
    }

    private static void straightBridge(Plan p, int x1, int y, int z1, int x2, int z2, int half) {
        if (x1 != x2 && z1 != z2) throw new IllegalArgumentException("Bridge must follow a cardinal axis");
        fill(p, Math.min(x1, x2) - (x1 == x2 ? half : 0), y - 2,
                Math.min(z1, z2) - (z1 == z2 ? half : 0),
                Math.max(x1, x2) + (x1 == x2 ? half : 0), y,
                Math.max(z1, z2) + (z1 == z2 ? half : 0), JADE);
        fill(p, Math.min(x1, x2) - (x1 == x2 ? half : 0), y + 1,
                Math.min(z1, z2) - (z1 == z2 ? half : 0),
                Math.max(x1, x2) + (x1 == x2 ? half : 0), y + 5,
                Math.max(z1, z2) + (z1 == z2 ? half : 0), AIR);
        int length = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= length; i++) {
            int x = x1 + Integer.signum(x2 - x1) * i, z = z1 + Integer.signum(z2 - z1) * i;
            for (int side : new int[] {-1, 1}) {
                int rx = x + (x1 == x2 ? side * (half + 1) : 0);
                int rz = z + (z1 == z2 ? side * (half + 1) : 0);
                if (Math.abs(rx) <= 7 || Math.abs(rz) <= 3
                        || Math.abs(Math.abs(rx) - 54) <= 3 && rz >= -94 && rz <= -12
                        || Math.abs(Math.abs(rx) - 34) <= 3 && rz >= -94 && rz <= -30
                        || Math.abs(Math.abs(rx) - 42) <= 3 && rz >= -94 && rz <= -30) continue;
                set(p, rx, y + 1, rz, oriented("town_jade_railing", x1 == x2 ? Direction.EAST : Direction.NORTH));
                if (i % 8 == 0) set(p, rx, y + 2, rz, decor("lamp", i + side));
            }
        }
    }

    private static void ascent(Plan p, int x1, int y1, int z1, int x2, int y2, int z2, int half) {
        int length = Math.abs(z2 - z1);
        for (int i = 0; i <= length; i++) {
            int z = z1 + Integer.signum(z2 - z1) * i;
            int y = y1 + i * (y2 - y1) / length;
            int next = y1 + (i + 1) * (y2 - y1) / length;
            fill(p, x1 - half, y - 1, z, x2 + half, y, z, JADE);
            fill(p, x1 - half, y + 1, z, x2 + half, y + 4, z, AIR);
            if (next > y && i < length) fill(p, x1 - half, y + 1, z, x2 + half, y + 1, z,
                    Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,
                            z2 > z1 ? Direction.SOUTH : Direction.NORTH));
            if (i <= half + 1 || i >= length - half - 1) continue;
            for (int side : new int[] {-1, 1}) {
                set(p, x1 + side * (half + 1), y + 1, z, oriented("town_jade_railing", Direction.EAST));
                if (i % 8 == 0) set(p, x1 + side * (half + 1), y + 2, z, decor("lamp", i));
            }
        }
    }

    private static void ringRail(Plan p, int cx, int y, int cz, int r, boolean axisOpening) {
        for (int z = -r; z <= r; z++) for (int x = -r; x <= r; x++) {
            int d = x * x + z * z;
            if (d > r * r || d < (r - 1) * (r - 1)) continue;
            if (axisOpening && (Math.abs(cx + x) <= 10 || Math.abs(cz + z) <= 6)) continue;
            set(p, cx + x, y, cz + z, oriented("town_jade_railing",
                    Math.abs(x) > Math.abs(z) ? Direction.EAST : Direction.NORTH));
            if ((x + z) % 18 == 0) {
                set(p, cx + x, y + 1, cz + z, decor("lamp", x + z));
                set(p, cx + x, y - 1, cz + z, LIGHT);
            }
        }
    }

    private static void disk(Plan p, int cx, int y, int cz, int radius, int thickness, BlockState state) {
        annulus(p, cx, y, cz, radius, 0, state, thickness);
    }

    private static void annulus(Plan p, int cx, int y, int cz, int outer, int inner, BlockState state) {
        annulus(p, cx, y, cz, outer, inner, state, 1);
    }

    private static void annulus(Plan p, int cx, int y, int cz, int outer, int inner, BlockState state, int h) {
        // Row runs keep the build plan compact enough to rebuild safely after a restart.
        for (int z = -outer; z <= outer; z++) {
            int edge = (int) Math.floor(Math.sqrt(outer * outer - z * z));
            if (Math.abs(z) >= inner || inner == 0) {
                fill(p, cx - edge, y, cz + z, cx + edge, y + h - 1, cz + z, state);
            } else {
                int hole = (int) Math.ceil(Math.sqrt(inner * inner - z * z));
                if (hole <= edge) {
                    fill(p, cx - edge, y, cz + z, cx - hole, y + h - 1, cz + z, state);
                    fill(p, cx + hole, y, cz + z, cx + edge, y + h - 1, cz + z, state);
                }
            }
        }
    }

    private static BlockState decor(String category, int index) {
        int count = category.equals("furniture") ? 20 : 16;
        return OrientalBlocks.state("daotai_" + category + "_" + String.format("%02d", Math.floorMod(index, count) + 1));
    }

    private static BlockState oriented(String id, Direction facing) {
        return OrientalBlocks.state(id).setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static void fill(Plan p, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        p.add(x1, y1, z1, x2, y2, z2, state);
    }

    private static void set(Plan p, int x, int y, int z, BlockState state) {
        fill(p, x, y, z, x, y, z, state);
    }
}
