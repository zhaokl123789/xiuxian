package xiuxian.sect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.OrientalBlocks;

/** Shared Jin-Dan furnace palace blueprint and standalone visual acceptance entry point. */
public final class JindanResidenceGenerator {
    /** Full top-down clearance revision; older construction cursors restart from clearing. */
    public static final int VERSION = 6;
    public static final int MIN_X = -178, MAX_X = 178;
    public static final int MIN_Z = -164, MAX_Z = 164;
    public static final int MIN_Y = -2, MAX_Y = 154;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BASALT = material("jindan_sunset_basalt");
    private static final BlockState CINDER = material("jindan_scorched_stone");
    private static final BlockState BRICK = material("jindan_cinnabar_brick");
    private static final BlockState COPPER = material("jindan_red_copper");
    private static final BlockState GOLD = material("jindan_gilded_copper");
    private static final BlockState JADE = material("jindan_white_jade");
    private static final BlockState FLOOR = material("jindan_formation_floor");
    private static final BlockState FIRE_TILE = material("jindan_fire_vein_tile");
    private static final BlockState LAMP = material("jindan_copper_lantern");
    private static final BlockState RED_LAMP = material("jindan_red_lantern");
    private static final BlockState FURNACE = material("jindan_four_symbol_cauldron");

    private JindanResidenceGenerator() {}

    public record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}
    public record Section(String id, String label, int firstPlacement, int lastPlacement) {}

    public static final class Plan {
        final List<Placement> placements = new ArrayList<>();
        final List<SiteClearance.Region> clearances = new ArrayList<>(List.of(
                new SiteClearance.Region(MIN_X,1,MIN_Z,MAX_X,MAX_Z)));
        final List<Placement> interiorClearances = new ArrayList<>();
        final List<Section> sections = new ArrayList<>();
        private String activeSectionId;
        private String activeSectionLabel;
        private int activeSectionStart;

        void clearInterior(int x1, int y1, int z1, int x2, int y2, int z2) {
            interiorClearances.add(new Placement(x1, y1, z1, x2, y2, z2, AIR));
        }

        void beginSection(String id, String label) {
            finishSection();
            activeSectionId = id;
            activeSectionLabel = label;
            activeSectionStart = placements.size();
        }

        void finishSection() {
            if (activeSectionId != null) {
                sections.add(new Section(activeSectionId, activeSectionLabel, activeSectionStart, placements.size()));
                activeSectionId = null;
                activeSectionLabel = null;
            }
        }

        void add(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            Placement op = new Placement(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), state);
            if (op.minX() < MIN_X || op.maxX() > MAX_X || op.minZ() < MIN_Z || op.maxZ() > MAX_Z
                    || op.minY() < MIN_Y || op.maxY() > MAX_Y) throw new IllegalArgumentException("Jin-Dan placement outside site: " + op);
            placements.add(op);
        }
    }

    public static Plan createPlan() {
        Plan p = new Plan();
        p.beginSection("foundation", "地火基座");
        fill(p, MIN_X, -2, MIN_Z, MAX_X, -1, MAX_Z, BASALT);
        // Keep the volcanic foundation inside the declared Z footprint (\u00b1164).
        // The previous radius 166 escaped the site by two blocks and made the
        // inspection item throw before it could queue construction.
        disk(p, 0, 0, 0, 162, 1, CINDER);
        ring(p, 0, 1, 0, 164, 158, BRICK);
        ring(p, 0, 2, 0, 154, 150, COPPER);
        p.beginSection("outer_courts", "外围丹宫");
        buildOuterCourts(p);
        p.beginSection("palaces", "四方主殿");
        buildPalace(p);
        p.beginSection("furnace", "四象丹炉");
        buildFurnace(p);
        p.beginSection("annexes", "炼丹偏殿");
        buildAnnexes(p);
        p.beginSection("roads", "丹火天街");
        buildRoads(p);
        p.finishSection();
        // Put room clearance first, preserving all later overlapping shells and furnishings.
        int rooms = p.interiorClearances.size();
        p.placements.addAll(0, p.interiorClearances);
        for (int i = 0; i < p.sections.size(); i++) {
            Section section = p.sections.get(i);
            p.sections.set(i, new Section(section.id(), section.label(),
                    i == 0 ? 0 : section.firstPlacement() + rooms, section.lastPlacement() + rooms));
        }
        return p;
    }

    static String sectionLabel(int index) {
        return switch (index) {
            case 0 -> "地火基座";
            case 1 -> "外围丹宫";
            case 2 -> "四方主殿";
            case 3 -> "四象丹炉";
            case 4 -> "炼丹偏殿";
            case 5 -> "丹火天街";
            default -> "施工";
        };
    }

    public static boolean generate(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null || level.dimension() != Level.OVERWORLD) return false;
        if ((long) origin.getY() + MIN_Y < level.getMinBuildHeight() || (long) origin.getY() + MAX_Y >= level.getMaxBuildHeight()) return false;
        for (int x : new int[] {MIN_X, MAX_X}) for (int z : new int[] {MIN_Z, MAX_Z})
            if (!level.getWorldBorder().isWithinBounds(origin.offset(x, 0, z))) return false;
        return JindanResidenceConstruction.start(level, origin, createPlan());
    }

    public static boolean isGenerated(ServerLevel level, BlockPos origin) {
        return level != null && origin != null && JindanResidenceConstruction.data(level).contains(origin);
    }

    private static void buildOuterCourts(Plan p) {
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24.0;
            int x = (int) Math.round(Math.sin(a) * 136), z = (int) Math.round(Math.cos(a) * 136);
            disk(p, x, 3, z, 17, 2, JADE);
            ring(p, x, 5, z, 15, 13, GOLD);
            pagoda(p, x, 5, z, 5 + i % 3, 18 + i % 4);
            set(p, x + (int)Math.round(Math.sin(a) * 7), 6, z + (int)Math.round(Math.cos(a) * 7), LAMP);
        }
        for (int side : new int[] {-1, 1}) {
            pavilion(p, side * 112, 5, -62, 25, 20, side + 4);
            pavilion(p, side * 112, 5, 62, 25, 20, side + 8);
        }
        ring(p, 0, 7, 0, 116, 112, GOLD);
        ring(p, 0, 8, 0, 110, 108, FIRE_TILE);
    }

    private static void buildPalace(Plan p) {
        hall(p, 0, 8, -78, 82, 25, 18, 1);
        hall(p, 0, 8, 78, 82, 25, 18, 2);
        hall(p, -92, 8, 0, 29, 44, 22, 3);
        hall(p, 92, 8, 0, 29, 44, 22, 4);
        hall(p, 0, 10, -20, 54, 36, 34, 5);
        for (int x = -78; x <= 78; x += 12) {
            set(p, x, 9, -52, material("jindan_ember_lamp"));
            set(p, x, 9, 52, material("jindan_ember_lamp"));
        }
    }

    private static void buildFurnace(Plan p) {
        disk(p, 0, 9, 0, 48, 3, BLACK_IRON());
        ring(p, 0, 12, 0, 45, 39, COPPER);
        ring(p, 0, 13, 0, 38, 35, GOLD);
        fill(p, -16, 13, -16, 16, 40, 16, FURNACE);
        fill(p, -12, 16, -12, 12, 37, 12, BRICK);
        fill(p, -8, 18, -8, 8, 35, 8, AIR);
        // Four open fire gates keep the central furnace palace walkable from
        // every cardinal court. The previous solid shell made the furnace an
        // inaccessible box after the surrounding halls were built.
        clear(p, -7, 13, -18, 7, 22, -10);
        clear(p, -7, 13, 10, 7, 22, 18);
        clear(p, -18, 13, -7, -10, 22, 7);
        clear(p, 10, 13, -7, 18, 22, 7);
        gateway(p, 0, 13, -16, Direction.NORTH);
        gateway(p, 0, 13, 16, Direction.SOUTH);
        gateway(p, -16, 13, 0, Direction.WEST);
        gateway(p, 16, 13, 0, Direction.EAST);
        ring(p, 0, 35, 0, 14, 10, GOLD);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int x = (int)Math.round(Math.sin(a) * 28), z = (int)Math.round(Math.cos(a) * 28);
            fill(p, x - 2, 13, z - 2, x + 2, 30, z + 2, COPPER);
            set(p, x, 31, z, material("jindan_formation_pillar"));
            set(p, x, 32, z, material("jindan_skyfire_lamp"));
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16.0;
            int x = (int)Math.round(Math.sin(a) * 58), z = (int)Math.round(Math.cos(a) * 58);
            set(p, x, 9, z, material(ARRAY_BLOCKS[i % ARRAY_BLOCKS.length]));
            set(p, x, 10, z, LAMP);
        }
        fill(p, -70, 8, -3, 70, 9, 3, FIRE_TILE);
        fill(p, -3, 8, -70, 3, 9, 70, FIRE_TILE);
        for (int i = -60; i <= 60; i += 12) {
            set(p, i, 10, -5, material("jindan_wall_sconce"));
            set(p, i, 10, 5, material("jindan_wall_sconce"));
            set(p, -5, 10, i, material("jindan_wall_sconce"));
            set(p, 5, 10, i, material("jindan_wall_sconce"));
        }
    }

    private static BlockState BLACK_IRON() { return material("jindan_black_iron"); }

    private static final String[] ARRAY_BLOCKS = {
            "jindan_solar_seal", "jindan_moon_seal", "jindan_five_element_array",
            "jindan_nine_flame_array", "jindan_tribulation_mark", "jindan_core_seal",
            "jindan_meridian_line", "jindan_heaven_cycle_seal"
    };

    private static void buildAnnexes(Plan p) {
        int[][] spots = {{-66,-66},{66,-66},{-66,66},{66,66}};
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0], z = spots[i][1];
            hall(p, x, 9, z, 26, 20, 17, i + 8);
            set(p, x - 8, 10, z - 10, material("jindan_pill_cabinet"));
            set(p, x + 8, 10, z - 10, material("jindan_pill_shelf"));
            set(p, x - 8, 10, z + 8, material("jindan_recipe_desk"));
            set(p, x + 8, 10, z + 8, material("jindan_meditation_mat"));
            set(p, x - 8, 10, z, material("jindan_herb_cabinet"));
            set(p, x + 8, 10, z, material("jindan_herb_drying_rack"));
            set(p, x, 10, z - 8, material("jindan_incense_burner"));
            set(p, x, 10, z + 8, material("jindan_pill_display"));
            set(p, x - 10, 13, z - 10, material("jindan_hanging_lamp"));
            set(p, x + 10, 13, z - 10, material("jindan_hanging_lamp"));
        }
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            int x = (int)Math.round(Math.sin(a) * 94), z = (int)Math.round(Math.cos(a) * 94);
            set(p, x, 8, z, material(i % 2 == 0 ? "jindan_mountain_stele" : "jindan_red_pine_bonsai"));
        }
    }

    private static void buildRoads(Plan p) {
        fill(p, -112, 6, -5, 112, 7, 5, JADE);
        fill(p, -5, 6, -112, 5, 7, 112, JADE);
        for (int i = -100; i <= 100; i += 10) {
            set(p, i, 8, -9, RED_LAMP); set(p, i, 8, 9, RED_LAMP);
            set(p, -9, 8, i, RED_LAMP); set(p, 9, 8, i, RED_LAMP);
        }
        // A final access pass runs after every shell, roof and furnishing
        // operation.  It guarantees that later overlapping masses cannot
        // seal the four entrances or the central walking lane again.
        accessPassages(p);
    }

    private static void accessPassages(Plan p) {
        int[][] halls = {
                {0, 8, -78, 82, 25, 18}, {0, 8, 78, 82, 25, 18},
                {-92, 8, 0, 29, 44, 22}, {92, 8, 0, 29, 44, 22},
                {0, 10, -20, 54, 36, 34},
                {-66, 9, -66, 26, 20, 17}, {66, 9, -66, 26, 20, 17},
                {-66, 9, 66, 26, 20, 17}, {66, 9, 66, 26, 20, 17}
        };
        for (int[] hall : halls) {
            int x = hall[0], y = hall[1], z = hall[2], hx = hall[3], hz = hall[4];
            int top = Math.min(y + hall[5] - 1, y + 11);
            clear(p, x - 6, y + 2, z - hz - 1, x + 6, top, z - hz + 4);
            clear(p, x - 6, y + 2, z + hz - 4, x + 6, top, z + hz + 1);
            clear(p, x - hx - 1, y + 2, z - 6, x - hx + 4, top, z + 6);
            clear(p, x + hx - 4, y + 2, z - 6, x + hx + 1, top, z + 6);
            // Keep a three-block processional aisle between each doorway and
            // the room centre while leaving the side furniture in place.
            clear(p, x - 1, y + 2, z - hz + 3, x + 1, top, z + hz - 3);
            clear(p, x - hx + 3, y + 2, z - 1, x + hx - 3, top, z + 1);
            doorwayTrim(p, x, y + 2, z - hz, Direction.NORTH);
            doorwayTrim(p, x, y + 2, z + hz, Direction.SOUTH);
            doorwayTrim(p, x - hx, y + 2, z, Direction.WEST);
            doorwayTrim(p, x + hx, y + 2, z, Direction.EAST);
        }
        // The furnace is a separate shell built after the halls; reopen its
        // four fire gates in this last pass as well.
        clear(p, -7, 13, -19, 7, 23, -9);
        clear(p, -7, 13, 9, 7, 23, 19);
        clear(p, -19, 13, -7, -9, 23, 7);
        clear(p, 9, 13, -7, 19, 23, 7);
        doorwayTrim(p, 0, 13, -16, Direction.NORTH);
        doorwayTrim(p, 0, 13, 16, Direction.SOUTH);
        doorwayTrim(p, -16, 13, 0, Direction.WEST);
        doorwayTrim(p, 16, 13, 0, Direction.EAST);
    }

    private static void doorwayTrim(Plan p, int x, int y, int z, Direction facing) {
        gateway(p, x, y, z, facing);
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            set(p, x - 3, y, z, material("jindan_door_panel"));
            set(p, x + 3, y, z, material("jindan_door_panel"));
        } else {
            set(p, x, y, z - 3, material("jindan_door_panel"));
            set(p, x, y, z + 3, material("jindan_door_panel"));
        }
    }

    private static void hall(Plan p, int x, int y, int z, int hx, int hz, int h, int variant) {
        fill(p, x - hx, y, z - hz, x + hx, y + 1, z + hz, FLOOR);
        p.clearInterior(x - hx + 3, y + 2, z - hz + 3, x + hx - 3, y + h, z + hz - 3);
        fill(p, x - hx, y + 2, z - hz, x + hx, y + h, z - hz + 2, BRICK);
        fill(p, x - hx, y + 2, z + hz - 2, x + hx, y + h, z + hz, BRICK);
        fill(p, x - hx, y + 2, z - hz, x - hx + 2, y + h, z + hz, BRICK);
        fill(p, x + hx - 2, y + 2, z - hz, x + hx, y + h, z + hz, BRICK);
        // Every hall has a real through-passage. Side annexes previously had
        // walls on all four sides because only the north wall was cleared.
        clear(p, x - 7, y + 2, z - hz - 1, x + 7, y + 9, z - hz + 3);
        clear(p, x - 7, y + 2, z + hz - 3, x + 7, y + 9, z + hz + 1);
        clear(p, x - hx - 1, y + 2, z - 7, x - hx + 3, y + 9, z + 7);
        clear(p, x + hx - 3, y + 2, z - 7, x + hx + 1, y + 9, z + 7);
        gateway(p, x, y + 2, z - hz, Direction.NORTH);
        gateway(p, x, y + 2, z + hz, Direction.SOUTH);
        gateway(p, x - hx, y + 2, z, Direction.WEST);
        gateway(p, x + hx, y + 2, z, Direction.EAST);
        for (int dx = -hx + 4; dx <= hx - 4; dx += 12) {
            pillar(p, x + dx, y + 2, z - hz, y + h, variant + dx);
            pillar(p, x + dx, y + 2, z + hz, y + h, variant - dx);
        }
        roof(p, x, y + h + 1, z, hx + 5, hz + 5, variant);
        interior(p, x, y, z, hx, hz, h, variant);
        for (int dx = -hx + 7; dx <= hx - 7; dx += 14) {
            set(p, x + dx, y + 4, z - hz + 3, material("jindan_pillar_lamp"));
            set(p, x + dx, y + 4, z + hz - 3, material("jindan_pillar_lamp"));
        }
    }

    private static void interior(Plan p, int x, int y, int z, int hx, int hz, int h, int variant) {
        int iy = y + 2;
        // A rear altar anchors the room, while the side furniture creates a
        // readable circulation lane from each doorway to the centre.
        set(p, x, iy, z - Math.max(4, hz - 5), material("jindan_core_pedestal"));
        set(p, x, iy + 1, z - Math.max(4, hz - 5), material("jindan_core_orb"));
        set(p, x - Math.max(5, hx - 7), iy, z - 4, material("jindan_pill_cabinet"));
        set(p, x + Math.max(5, hx - 7), iy, z - 4, material("jindan_herb_cabinet"));
        set(p, x - Math.max(5, hx - 7), iy, z + 5, material("jindan_pill_shelf"));
        set(p, x + Math.max(5, hx - 7), iy, z + 5, material("jindan_recipe_stand"));
        set(p, x - 5, iy, z + 10, material("jindan_tea_table"));
        set(p, x + 5, iy, z + 10, material("jindan_meditation_mat"));
        set(p, x - Math.max(6, hx - 8), iy, z, material("jindan_incense_burner"));
        set(p, x + Math.max(6, hx - 8), iy, z, material("jindan_ritual_bell"));
        set(p, x - Math.max(6, hx - 8), iy, z + 10, material("jindan_red_pine_bonsai"));
        set(p, x + Math.max(6, hx - 8), iy, z + 10, material("jindan_herb_planter"));
        int side = Math.max(8, hx - 8);
        int rear = Math.max(6, hz - 8);
        set(p, x - side, iy, z - rear + 2, material("jindan_bottle_rack"));
        set(p, x + side, iy, z - rear + 2, material("jindan_sealed_jar"));
        set(p, x - side, iy, z + rear - 2, material("jindan_tool_rack"));
        set(p, x + side, iy, z + rear - 2, material("jindan_spirit_water_jar"));
        set(p, x - Math.max(7, hx - 10), iy, z + 14, material("jindan_grinding_table"));
        set(p, x + Math.max(7, hx - 10), iy, z + 14, material("jindan_robe_stand"));
        set(p, x - Math.max(7, hx - 10), iy, z - 14, material("jindan_hourglass"));
        set(p, x + Math.max(7, hx - 10), iy, z - 14, material("jindan_ritual_banner"));
        int lampY = Math.min(y + h - 3, y + 12);
        set(p, x - Math.max(5, hx - 8), lampY, z - hz + 3, material("jindan_wall_sconce"));
        set(p, x + Math.max(5, hx - 8), lampY, z - hz + 3, material("jindan_wall_sconce"));
        set(p, x - Math.max(5, hx - 8), lampY, z + hz - 3, material("jindan_wall_sconce"));
        set(p, x + Math.max(5, hx - 8), lampY, z + hz - 3, material("jindan_wall_sconce"));
        set(p, x, y + h - 2, z, material(variant % 2 == 0 ? "jindan_hanging_lamp" : "jindan_altar_lamp"));
        if (hx >= 20 && hz >= 16) {
            set(p, x - 12, iy, z, material("jindan_fireguard_screen"));
            set(p, x + 12, iy, z, material("jindan_fireguard_screen"));
            set(p, x - 12, iy, z - 10, material("jindan_scripture_stele"));
            set(p, x + 12, iy, z - 10, material("jindan_mountain_stele"));
        }
    }

    private static void gateway(Plan p, int x, int y, int z, Direction facing) {
        // The frame is a non-solid decorative arch; the clear calls above keep
        // its centre open so players can walk through every hall.
        set(p, x, y, z, material("jindan_gateway_frame"));
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            set(p, x - 1, y, z, material("jindan_pillar_base"));
            set(p, x + 1, y, z, material("jindan_pillar_base"));
        } else {
            set(p, x, y, z - 1, material("jindan_pillar_base"));
            set(p, x, y, z + 1, material("jindan_pillar_base"));
        }
    }

    private static void pavilion(Plan p, int x, int y, int z, int hx, int hz, int v) { hall(p, x, y, z, hx, hz, 12, v); }
    private static void pagoda(Plan p, int x, int y, int z, int r, int v) {
        fill(p, x-r, y, z-r, x+r, y+1, z+r, JADE);
        p.clearInterior(x-r, y+2, z-r, x+r, y+14, z+r);
        for (int side : new int[] {-1,1}) for (int end : new int[] {-1,1}) pillar(p, x+side*(r-2), y+2, z+end*(r-2), y+14, v+side+end);
        roof(p, x, y+15, z, r+4, r+4, v);
    }
    private static void pillar(Plan p, int x, int y, int z, int top, int v) { fill(p, x-1, y, z-1, x+1, top, z+1, COPPER); set(p,x,top+1,z,material("jindan_pillar_cap")); }
    private static void roof(Plan p, int x, int y, int z, int hx, int hz, int v) {
        for (int dz=-hz; dz<=hz; dz++) for (int dx=-hx; dx<=hx; dx++) {
            int edge=Math.min(hx-Math.abs(dx),hz-Math.abs(dz)); set(p,x+dx,y+Math.max(0,Math.min(8,edge/3)),z+dz, edge<2?GOLD:material("jindan_roof_tiles"));
        }
        set(p,x,y+10,z,material("jindan_dragon_ridge"));
    }
    private static void clear(Plan p,int x1,int y1,int z1,int x2,int y2,int z2){fill(p,x1,y1,z1,x2,y2,z2,AIR);}
    private static void set(Plan p,int x,int y,int z,BlockState s){fill(p,x,y,z,x,y,z,s);}
    private static void fill(Plan p,int x1,int y1,int z1,int x2,int y2,int z2,BlockState s){p.add(x1,y1,z1,x2,y2,z2,s);}
    private static void disk(Plan p,int x,int y,int z,int r,int h,BlockState s){for(int dz=-r;dz<=r;dz++){int e=(int)Math.sqrt(r*r-dz*dz);fill(p,x-e,y,z+dz,x+e,y+h-1,z+dz,s);}}
    private static void ring(Plan p,int x,int y,int z,int outer,int inner,BlockState s){for(int dz=-outer;dz<=outer;dz++){int e=(int)Math.sqrt(outer*outer-dz*dz);int hole=(int)Math.sqrt(Math.max(0,inner*inner-dz*dz));if(Math.abs(dz)>=inner){fill(p,x-e,y,z+dz,x+e,y,z+dz,s);}else{fill(p,x-e,y,z+dz,x-hole,y,z+dz,s);fill(p,x+hole,y,z+dz,x+e,y,z+dz,s);}}}
    private static BlockState material(String id){return OrientalBlocks.state(id);}
}
