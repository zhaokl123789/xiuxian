package xiuxian.sect;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.block.XiuxianBlocks;

/**
 * Versioned procedural generator for the Luoxia cave-heaven.
 *
 * <p>This is intentionally code generated rather than a random chunk feature:
 * the two high-rank residences and the future boss arena are unique landmarks
 * and must keep their coordinates after a restart.  The pass is guarded by a
 * dimension-local {@link LuoxiaInnerRealmData} record and therefore runs once
 * per world.</p>
 */
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class LuoxiaInnerRealmGenerator {
    /** Bumped when the visual pass changes; an older generated realm is rebuilt once. */
    public static final int VERSION = 3;

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState DEEPSLATE_TILE = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState POLISHED_DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState RED = Blocks.RED_TERRACOTTA.defaultBlockState();
    private static final BlockState CRIMSON = Blocks.CRIMSON_PLANKS.defaultBlockState();
    private static final BlockState COPPER = Blocks.CUT_COPPER.defaultBlockState();
    private static final BlockState DARK = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
    private static final BlockState TREE_LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState TREE_LEAVES = Blocks.OAK_LEAVES.defaultBlockState()
            .setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState MOSSY_COBBLE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private LuoxiaInnerRealmGenerator() {}

    /** Generate the whole realm once, if this is the dedicated dimension. */
    public static void ensureGenerated(ServerLevel level) {
        if (level == null || level.dimension() != LuoxiaInnerDimension.LEVEL) return;
        LuoxiaInnerRealmData data = LuoxiaInnerRealmData.get(level);
        if (data.generated && data.version == VERSION) {
            LuoxiaInnerBuildings.ensureGenerated(level);
            LuoxiaInnerRealmLayout.ensureArrival(level);
            LuoxiaDaotaiResidence.ensureGenerated(level);
            LuoxiaJindanResidence.ensureGenerated(level);
            LuoxiaSectComplexResidence.ensureGenerated(level);
            return;
        }
        if (data.generated) clearLegacySurface(level);

        long seed = level.getSeed() ^ 0x4C554F5849415F49L;
        data.seed = seed;
        data.version = VERSION;
        data.markers.clear();
        generate(level, data, seed);
        LuoxiaInnerBuildings.ensureGenerated(level);
        data.generated = true;
        data.setDirty();
        LuoxiaInnerRealmLayout.ensureArrival(level);
        LuoxiaDaotaiResidence.ensureGenerated(level);
        LuoxiaJindanResidence.ensureGenerated(level);
        LuoxiaSectComplexResidence.ensureGenerated(level);
    }

    private static void clearLegacySurface(ServerLevel level) {
        SiteClearance.clearBox(level, -70, 64, -30, 70, 100, 180);
        SiteClearance.clearBox(level, -35, -64, 130, 35, 90, 195);
        SiteClearance.clearBox(level, -60, -55, 190, 60, 20, 390);
        SiteClearance.clearBox(level, -410, 64, 130, 40, 150, 260);
        SiteClearance.clearBox(level, -20, 64, 140, 300, 100, 230);
        SiteClearance.clearBox(level, 210, 55, 70, 570, 110, 355);
        SiteClearance.clearBox(level, -70, 64, 340, 70, 310, 520);
    }

    /** A level-load hook covers server starts before a player enters. */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) ensureGenerated(level);
    }

    /** Returns a durable landmark coordinate for later quests and NPC logic. */
    public static BlockPos marker(ServerLevel level, String id) {
        if (level == null || level.dimension() != LuoxiaInnerDimension.LEVEL) return null;
        LuoxiaInnerRealmData data = LuoxiaInnerRealmData.get(level);
        ensureGenerated(level);
        return data.markers.get(id);
    }

    private static void generate(ServerLevel level, LuoxiaInnerRealmData data, long seed) {
        Random random = new Random(seed);
        // The vanilla flat preset is only a safe substrate for the generator.
        // Turn it into a cave-heaven landscape before placing landmarks so the
        // realm reads as terrain rather than a grass plane around structures.
        buildNaturalRealm(level, seed);
        buildMainAxis(level, data);
        buildCity(level, data, random);
        if (level.getChunkSource().getGenerator() instanceof xiuxian.vein.VeinChunkGenerator) {
            buildVeinAccess(level,data);
        } else {
            buildVein(level, data);
            buildBossArena(level, data);
        }
        addLighting(level);
    }

    private static void buildVeinAccess(ServerLevel level, LuoxiaInnerRealmData data) {
        restoreVeinLadder(level);
        for(int layer=1;layer<=9;layer++) data.marker("vein_l"+layer,new BlockPos(0,xiuxian.vein.VeinTerrain.floorY(layer)+1,163));
        data.marker("vein_entrance",new BlockPos(0,73,160));
        data.marker("vein_core",new BlockPos(0,-56,163));
        var boss=xiuxian.vein.VeinTerrain.room(0,160,9);
        data.marker("boss_center",boss.offset(0,0,3));data.marker("boss_gate",boss.offset(0,0,-26));
        data.marker("boss_reward_vault",xiuxian.vein.VeinTerrain.cache(boss));
    }

    private static void restoreVeinLadder(ServerLevel level) {
        int x=0,z=160;
        for(int y=56;y<=73;y++) {
            set(level,x,y,z,AIR());
            set(level,x+2,y,z,xiuxian.block.VeinBlocks.state("vein_reinforced_floor"));
            set(level,x+1,y,z,Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING,net.minecraft.core.Direction.WEST));
        }
    }

    /**
     * Shape the flat dimension substrate into rolling islands, streams and
     * planted groves.  The large landmarks retain their fixed coordinates,
     * while everything between them gets a seed-stable natural arrangement.
     */
    private static void buildNaturalRealm(ServerLevel level, long seed) {
        final int minX = -500, maxX = 500, minZ = -45, maxZ = 505;
        Random random = new Random(seed ^ 0x4E41545552414CL);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Keep the processional spine and the two ground settlements
                // level.  Their plazas are later dressed by their builders.
                boolean route = Math.abs(x) <= 12 && z >= -5 && z <= 180;
                boolean sect = x * x + (z - 105) * (z - 105) <= 58 * 58;
                boolean city = Math.abs(x - 390) <= 176 && Math.abs(z - 210) <= 132;
                boolean jindan = (x + 360) * (x + 360) + (z - 220) * (z - 220) <= 52 * 52;
                boolean entry = x * x + (z + 0) * (z + 0) <= 28 * 28;
                int surface = terrainHeight(x, z, seed);
                if (route) {
                    // The avenue is a ground road, not a bridge floating six
                    // blocks above the landscape. Match its gentle descent.
                    surface = z <= 110
                            ? 69 - Math.min(4, Math.max(0, (z - 5) / 28))
                            : 66 + Math.round((z - 110) * 5.0F / 50.0F);
                } else if (sect) {
                    surface = 64;
                }
                if (city) surface = 63;
                // The arrival dais sits at y=71; give it a shallow planted
                // terrace rather than leaving a nine-block grass pit around
                // the pylons.
                if (jindan) surface = Math.min(surface, 62);
                if (entry && !route) surface = 68;

                // The flat preset already contains stone up to y=62 and a
                // grass cap at y=63. Only touch the delta, keeping this pass
                // cheap enough for a first login while still making relief.
                if (surface >= 63) {
                    for (int y = 64; y < surface; y++) {
                        set(level, x, y, z, y == surface - 1 ? DIRT : STONE);
                    }
                    set(level, x, surface, z, GRASS);
                } else {
                    for (int y = surface + 1; y <= 63; y++) set(level, x, y, z, AIR());
                    // Lower pockets become clear water basins.  The shore is
                    // intentionally irregular because the height field uses
                    // two incommensurate waves rather than a square grid.
                    int waterTop = Math.min(62, Math.max(60, surface + 2));
                    for (int y = surface + 1; y <= waterTop; y++) set(level, x, y, z, WATER);
                    set(level, x, surface, z, random.nextInt(5) == 0 ? MOSS : DIRT);
                }
            }
        }

        // Place the large waterways first so random scenery never grows in a
        // lake or through a waterfall basin.
        buildScenicGeography(level, seed);

        // Seed-stable spirit groves, shrine stones and fallen-bank lanterns
        // make the spaces between guaranteed landmarks feel hand-authored.
        for (int i = 0; i < 86; i++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);
            if (nearLandmark(x, z)) continue;
            int y = terrainHeight(x, z, seed);
            if (y < 62 || (Math.abs(x) < 24 && z > 0 && z < 175)) continue;
            if ((i & 3) == 0) spiritTree(level, x, y + 1, z, 3 + random.nextInt(3));
            else if ((i & 3) == 1) shrineStone(level, x, y + 1, z, 2 + random.nextInt(3));
            else if ((i & 3) == 2) bankLantern(level, x, y + 1, z);
        }
    }

    /** Fixed scenic set pieces give the cave-heaven a readable horizon. */
    private static void buildScenicGeography(ServerLevel level, long seed) {
        // A broad lake and a winding river occupy the empty land between the
        // sect and the ancient city. Their banks are stepped instead of cut
        // as a square trench, so roads can cross them with small bridges.
        int lakeX = 168, lakeZ = 74;
        for (int dx = -42; dx <= 42; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                double ellipse = dx * dx / 1764.0D + dz * dz / 900.0D;
                if (ellipse > 1.0D) continue;
                int shore = 61 + (int) Math.round(ellipse * 2.0D);
                for (int y = shore + 1; y <= 63; y++) set(level, lakeX + dx, y, lakeZ + dz, AIR());
                for (int y = shore + 1; y <= 62; y++) set(level, lakeX + dx, y, lakeZ + dz, WATER);
                if (ellipse > 0.80D) set(level, lakeX + dx, shore, lakeZ + dz, MOSSY_COBBLE);
            }
        }
        for (int z = 18; z <= 205; z++) {
            int cx = 92 + (int) Math.round(Math.sin((z + (seed & 31)) * 0.075D) * 24.0D);
            for (int dx = -3; dx <= 3; dx++) {
                int x = cx + dx;
                for (int y = 60; y <= 63; y++) set(level, x, y, z, y <= 62 ? WATER : AIR());
                if (Math.abs(dx) == 4) {
                    set(level, x, 63, z, MOSSY_COBBLE);
                    if ((z & 15) == 0) bankLantern(level, x, 64, z);
                }
            }
        }

        // A stepped cliff and hanging fall form a strong vista from the main
        // axis. The fall ends in a small pool and uses no unsupported blocks.
        int cliffX = 278, cliffZ = 92;
        for (int tier = 0; tier < 4; tier++) {
            int y = 66 + tier * 4;
            int depth = 28 - tier * 5;
            fill(level, cliffX - 6, y, cliffZ - depth, cliffX + 6, y + 3, cliffZ + depth, STONE);
            fill(level, cliffX - 7, y + 3, cliffZ - depth, cliffX + 7, y + 3, cliffZ + depth, MOSS);
        }
        for (int y = 62; y <= 80; y++) {
            set(level, cliffX, y, cliffZ - 31, WATER);
            set(level, cliffX + 1, y, cliffZ - 31, WATER);
        }
        circle(level, cliffX, 61, cliffZ - 31, 8, WATER);
        ring(level, cliffX, 63, cliffZ - 31, 9, 1, MOSSY_COBBLE);
        lampPost(level, cliffX - 5, 64, cliffZ - 31);

        // Two sky islands make the high residences feel part of a vertical
        // realm. Each has a tapered underside, a small pavilion and a beacon.
        floatingIsland(level, 214, 105, 28, 24, 16);
        floatingIsland(level, -186, 94, 42, 20, 13);
    }

    private static void floatingIsland(ServerLevel level, int cx, int y, int cz, int rx, int rz) {
        for (int dy = 0; dy < 9; dy++) {
            int shrink = dy * 2;
            int sx = Math.max(2, rx - shrink);
            int sz = Math.max(2, rz - shrink / 2);
            ellipse(level, cx, y - dy, cz, sx, sz, dy == 0 ? GRASS : STONE, 1.0D);
        }
        for (int[] p : new int[][]{{-rx / 2, -rz / 2}, {rx / 2, -rz / 2},
                {-rx / 2, rz / 2}, {rx / 2, rz / 2}}) {
            set(level, cx + p[0], y + 1, cz + p[1], MOSSY_COBBLE);
            set(level, cx + p[0], y + 2, cz + p[1], COPPER);
            set(level, cx + p[0], y + 3, cz + p[1], LANTERN);
        }
        pavilion(level, cx, y + 1, cz, Math.max(5, Math.min(rx, rz) / 2));
        for (int dy = 1; dy <= 12; dy++) {
            if ((dy & 1) == 0) set(level, cx, y - 9 - dy, cz, GLOW);
        }
    }

    private static int terrainHeight(int x, int z, long seed) {
        double wave = Math.sin((x + (seed & 255)) * 0.021D)
                + Math.cos((z - ((seed >>> 8) & 255)) * 0.027D)
                + Math.sin((x + z) * 0.011D);
        int height = 63 + (int) Math.round(wave * 2.2D);
        // Keep a few real lowlands for water, and occasional island ridges.
        return Math.max(58, Math.min(69, height));
    }

    private static boolean nearLandmark(int x, int z) {
        return (x * x + (z - 105) * (z - 105) <= 72 * 72)
                || (Math.abs(x - 390) <= 138 && Math.abs(z - 210) <= 108)
                || ((x + 360) * (x + 360) + (z - 220) * (z - 220) <= 64 * 64)
                || ((x - 168) * (x - 168) <= 48 * 48 && (z - 74) * (z - 74) <= 36 * 36)
                || (x >= 55 && x <= 130 && z >= 10 && z <= 215)
                || (Math.abs(x - 278) <= 20 && Math.abs(z - 92) <= 42)
                || (Math.abs(x) <= 18 && z > -8 && z < 190);
    }

    private static void spiritTree(ServerLevel level, int x, int y, int z, int height) {
        for (int dy = 0; dy < height; dy++) set(level, x, y + dy, z, TREE_LOG);
        int crownY = y + height - 1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy <= 13) set(level, x + dx, crownY + dy, z + dz, TREE_LEAVES);
                }
            }
        }
        set(level, x, crownY + 3, z, AMETHYST);
        set(level, x, crownY + 4, z, LAMP);
    }

    private static void shrineStone(ServerLevel level, int x, int y, int z, int radius) {
        for (int dy = 0; dy < radius + 1; dy++) {
            int r = Math.max(1, radius - dy / 2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz,
                            dy == radius ? CALCITE : MOSSY_COBBLE);
                }
            }
        }
        set(level, x, y + radius + 1, z, SOUL_LANTERN);
    }

    private static void bankLantern(ServerLevel level, int x, int y, int z) {
        // Small curved-looking bank marker; its offset footing avoids a rigid
        // lamp grid and gives the water pockets a readable human scale.
        set(level, x, y, z, MOSSY_COBBLE);
        set(level, x, y + 1, z, COPPER);
        set(level, x, y + 2, z, COPPER);
        set(level, x, y + 3, z, LANTERN);
    }

    private static void buildEntry(ServerLevel level, LuoxiaInnerRealmData data) {
        BlockPos center = LuoxiaInnerRealmLayout.LEGACY_ENTRY;
        data.marker("entry_gate", center);
        circle(level, center.getX(), center.getY() - 1, center.getZ(), 8, CALCITE);
        ring(level, center.getX(), center.getY() - 1, center.getZ(), 8, 1, POLISHED_DEEPSLATE);
        for (int[] offset : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
            tower(level, center.getX() + offset[0], center.getY() - 1,
                    center.getZ() + offset[1], 4, 5, AMETHYST);
        }
        // The flat dimension is solid stone above its grass layer, and the
        // pylon footprints overlap the inner ring. Carve the landing after
        // placing them so ENTRY itself stays open on the first visit.
        clearBox(level, center.getX() - 3, center.getY(), center.getZ() - 3,
                center.getX() + 3, center.getY() + 2, center.getZ() + 3);
        // The north pylon sits on the route to the processional road.  Cut a
        // narrow two-block passage through its center so the decorative gate
        // remains visible without turning the arrival pad into a dead end.
        clearBox(level, center.getX() - 2, center.getY(), center.getZ() + 2,
                center.getX() + 2, center.getY() + 2, center.getZ() + 8);
        for (int i = -3; i <= 3; i++) {
            set(level, center.getX() + i, center.getY(), center.getZ() + 9, LAMP);
            set(level, center.getX() + i, center.getY() + 1, center.getZ() + 9, AIR());
        }
    }

    private static void buildMainAxis(ServerLevel level, LuoxiaInnerRealmData data) {
        data.marker("main_axis", new BlockPos(0, 66, 72));
        // A fixed, gently descending processional road from the floating gate.
        for (int z = 5; z <= 110; z++) {
            int y = 70 - Math.min(4, Math.max(0, (z - 5) / 28));
            for (int x = -3; x <= 3; x++) {
                set(level, x, y, z, x == -3 || x == 3 ? POLISHED_DEEPSLATE : QUARTZ);
                set(level, x, y + 1, z, AIR());
                set(level, x, y + 2, z, AIR());
            }
            if (z % 8 == 0) {
                lampPost(level, -6, y, z);
                lampPost(level, 6, y, z);
            }
        }
        // Close the gap between the central avenue and the y=72 hub from
        // which the high-rank residences branch.
        for (int z = 111; z <= 160; z++) {
            int y = 67 + Math.round((z - 110) * 5.0F / 50.0F);
            for (int x = -3; x <= 3; x++) {
                set(level, x, y, z, x == -3 || x == 3 ? POLISHED_DEEPSLATE : QUARTZ);
                set(level, x, y + 1, z, AIR());
                set(level, x, y + 2, z, AIR());
            }
            if ((z - 111) % 8 == 0) {
                lampPost(level, -6, y, z);
                lampPost(level, 6, y, z);
            }
        }
        for (int z = 9; z <= 108; z += 9) {
            for (int x = -1; x <= 1; x++) set(level, x, 70, z, COPPER);
        }
    }

    private static void buildSect(ServerLevel level, LuoxiaInnerRealmData data, Random random) {
        int cx = 0;
        int floor = 65;
        int cz = 105;
        data.marker("sect_core", new BlockPos(cx, floor + 1, cz));
        data.marker("sect_court", new BlockPos(cx, floor + 1, cz - 24));
        fill(level, cx - 48, floor, cz - 42, cx + 48, floor, cz + 42, POLISHED_DEEPSLATE);
        ring(level, cx, floor + 1, cz, 47, 1, RED);
        gate(level, cx, floor + 1, cz - 42, 15, 12, RED, COPPER);
        courtyard(level, cx, floor + 1, cz - 20, 16);
        hall(level, cx, floor + 1, cz + 14, 25, 18, 12);
        // The core is fixed, while the surrounding halls are deterministic
        // modules.  This gives each save a little local variation without
        // moving the navigation spine or any unique residence.
        int[][] moduleSites = {{-34, 76}, {34, 76}, {-34, 128}, {34, 128},
                {-32, 105}, {32, 105}, {-18, 78}, {18, 78}, {-18, 132}, {18, 132},
                {-42, 105}, {42, 105}, {-24, 92}, {24, 92}};
        // Shuffle the candidate sites with the realm seed. The spine/core
        // remain fixed, but the peripheral halls are genuinely laid out per
        // save instead of always taking the first N positions.
        for (int i = moduleSites.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            int[] tmp = moduleSites[i];
            moduleSites[i] = moduleSites[swap];
            moduleSites[swap] = tmp;
        }
        int moduleCount = 8 + random.nextInt(7);
        for (int i = 0; i < moduleCount; i++) {
            int[] p = moduleSites[i];
            module(level, p[0], floor + 1, p[1], 9 + random.nextInt(3), random.nextInt(3));
            data.marker("sect_module_" + i, new BlockPos(p[0], floor + 2, p[1]));
        }
        // The central teaching stairs and an identifiable plaque marker.
        for (int i = 0; i < 9; i++) {
            fill(level, -10 + i, floor + 2 + i / 3, cz + 2 - i * 2,
                    10 - i, floor + 2 + i / 3, cz + 2 - i * 2, QUARTZ);
        }
        for (int x = -20; x <= 20; x += 10) lampPost(level, x, floor + 2, cz - 20);
        lampPost(level, -25, floor + 2, cz + 22);
        lampPost(level, 25, floor + 2, cz + 22);
    }

    static void buildCity(ServerLevel level, LuoxiaInnerRealmData data, Random random) {
        int cx = 390;
        int floor = 64;
        int cz = 210;
        data.marker("city_center", new BlockPos(cx, floor + 1, cz));
        MortalTownGenerator.Plan plan = MortalTownGenerator.createPlan();
        // Clear the entire town site, including sky above its highest roof.
        SiteClearance.clearBox(level, cx + plan.minX, floor + 1, cz + plan.minZ,
                cx + plan.maxX, level.getMaxBuildHeight() - 1, cz + plan.maxZ);
        SiteClearance.clearBox(level, cx + plan.minX, floor + 1, cz + plan.minZ,
                cx + plan.maxX, level.getMaxBuildHeight() - 1, cz + plan.maxZ);
        for (MortalTownGenerator.Placement placement : plan.placements) {
            for (int x = placement.minX(); x <= placement.maxX(); x++) {
                for (int y = placement.minY(); y <= placement.maxY(); y++) {
                    for (int z = placement.minZ(); z <= placement.maxZ(); z++) {
                        set(level, cx + x, floor + y, cz + z, placement.state());
                    }
                }
            }
        }
        data.marker("city_square", new BlockPos(cx, floor + 2, cz));
        // Keep the legacy block markers stable for quests and NPC placement.
        // They now point into the corresponding districts of the richer town.
        for (int ix = -4; ix <= 4; ix++) {
            for (int iz = -3; iz <= 3; iz++) {
                if (Math.abs(ix) <= 1 && Math.abs(iz) <= 1) continue;
                int markerIndex = (ix + 4) * 7 + (iz + 3);
                data.marker("city_block_" + markerIndex,
                        new BlockPos(cx + ix * 30, floor + 2, cz + iz * 30));
            }
        }
        // Preserve the established approach from the central spine to the
        // western gate, stopping at the town wall so the gate remains open.
        for (int step = 0; step <= 240; step++) {
            int x = step;
            int z = 160 + Math.round(step * 50.0F / 270.0F);
            int y = 72 - Math.round(step * 8.0F / 270.0F);
            for (int dx = -3; dx <= 3; dx++) {
                set(level, x + dx, y, z, dx == -3 || dx == 3 ? POLISHED_DEEPSLATE : QUARTZ);
                set(level, x + dx, y + 1, z, AIR());
                set(level, x + dx, y + 2, z, AIR());
            }
            if (step % 12 == 0) {
                lampPost(level, x - 6, y, z);
                lampPost(level, x + 6, y, z);
            }
        }
        // The branch road crosses the lake/lowlands as a supported causeway,
        // with occasional stone piers reaching the natural bed below it.
        for (int step = 18; step < 240; step += 24) {
            int x = step;
            int z = 160 + Math.round(step * 50.0F / 270.0F);
            int roadY = 72 - Math.round(step * 8.0F / 270.0F);
            for (int y = 57; y < roadY; y++) {
                set(level, x, y, z, y == 57 ? MOSSY_COBBLE : POLISHED_DEEPSLATE);
                if ((step / 24) % 2 == 0) set(level, x + 1, y, z, POLISHED_DEEPSLATE);
            }
            ring(level, x, roadY, z, 3, 1, CALCITE);
        }
    }

    private static void buildCityWaterways(ServerLevel level, int cx, int floor, int cz) {
        // Two offset canals and arched stone bridges break up the city grid.
        // Water sits below the street grade, so the city remains traversable.
        for (int z = cz - 72; z <= cz + 72; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                set(level, cx - 72 + dx, floor + 1, z, WATER);
                set(level, cx + 72 + dx, floor + 1, z, WATER);
                set(level, cx - 75 + dx, floor + 2, z, MOSSY_COBBLE);
                set(level, cx + 75 + dx, floor + 2, z, MOSSY_COBBLE);
            }
        }
        for (int x = cx - 72; x <= cx + 72; x++) {
            for (int dz = -2; dz <= 2; dz++) set(level, x, floor + 1, cz - 60 + dz, WATER);
        }
        for (int z : new int[]{cz - 45, cz, cz + 45}) {
            bridge(level, cx - 78, floor + 2, z, 12, true);
            bridge(level, cx + 78, floor + 2, z, 12, true);
        }
        bridge(level, cx, floor + 2, cz - 60, 14, false);
    }

    private static void bridge(ServerLevel level, int cx, int y, int cz, int half, boolean alongZ) {
        if (alongZ) {
            for (int dx = -half; dx <= half; dx++) {
                int crown = y + Math.max(0, 2 - (Math.abs(dx) * 2 / Math.max(1, half)));
                for (int dz = -3; dz <= 3; dz++) set(level, cx + dx, crown, cz + dz, QUARTZ);
            }
        } else {
            for (int dz = -half; dz <= half; dz++) {
                int crown = y + Math.max(0, 2 - (Math.abs(dz) * 2 / Math.max(1, half)));
                for (int dx = -3; dx <= 3; dx++) set(level, cx + dx, crown, cz + dz, QUARTZ);
            }
        }
        for (int i = -half + 2; i <= half - 2; i += 4) {
            if (alongZ) lampPost(level, cx + i, y + 2, cz);
            else lampPost(level, cx, y + 2, cz + i);
        }
    }

    private static void buildVein(ServerLevel level, LuoxiaInnerRealmData data) {
        int cx = 0, entranceY = 62, cz = 160;
        data.marker("vein_entrance", new BlockPos(cx, entranceY, cz));
        // A visible central shaft and six safe platforms represent the nine
        // layers while leaving room for later detailed cave features.
        for (int y = -57; y <= entranceY; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) set(level, cx + dx, y, cz + dz, AIR());
            }
            if (y % 8 == 0) {
                ring(level, cx, y, cz, 7, 1, y < 0 ? AMETHYST : POLISHED_DEEPSLATE);
                set(level, cx, y + 1, cz, LAMP);
            }
        }
        int[] layers = {62, 46, 30, 14, -2, -18, -34, -50, -56};
        for (int i = 0; i < layers.length; i++) {
            int y = layers[i];
            // The flat dimension starts as solid stone.  Carve a chamber
            // around each platform before laying its floor so the route is
            // actually walkable rather than a set of buried markers.
            clearCylinder(level, cx, y - 2, cz, 15, 8);
            circle(level, cx, y, cz, 13, i < 2 ? CALCITE : i < 5 ? DEEPSLATE : DARK);
            ring(level, cx, y + 1, cz, 13, 1, i < 3 ? AMETHYST : COPPER);
            lampPost(level, -9, y + 1, cz);
            lampPost(level, 9, y + 1, cz);
            data.marker("vein_l" + (i + 1), new BlockPos(cx, y + 1, cz));
        }
        // The platform carving above clears the upper part of the first
        // chamber, so lay the connecting stair after that pass to keep all
        // of its intermediate treads intact.
        buildVeinEntryStair(level, cx, entranceY, cz);
        data.marker("vein_core", new BlockPos(cx, -56, cz));
        // These are real mod ore blocks (rather than decorative crystals), so
        // the cave-heaven has an immediately playable high-quality resource
        // route.  Their fixed coordinates are written by this one pass and do
        // not depend on the overworld biome modifiers.
        BlockState ordinary = XiuxianBlocks.SPIRIT_STONE_ORE.get().defaultBlockState();
        BlockState mid = XiuxianBlocks.MID_SPIRIT_STONE_ORE.get().defaultBlockState();
        BlockState high = XiuxianBlocks.HIGH_SPIRIT_STONE_ORE.get().defaultBlockState();
        BlockState supreme = XiuxianBlocks.SUPREME_SPIRIT_STONE_ORE.get().defaultBlockState();
        int[][] ordinaryPockets = {{-13, 58, -2}, {13, 54, 2}, {-15, 48, 5}, {15, 40, -5},
                {-14, 32, -4}, {14, 26, 4}, {-16, 20, 7}, {16, 12, -7},
                {-14, 4, -3}, {14, -8, 3}, {-15, -20, 6}, {15, -30, -6},
                {-12, -42, 2}, {12, -50, -2}};
        for (int[] p : ordinaryPockets) orePocket(level, cx + p[0], p[1], cz + p[2], ordinary, 1);
        for (int[] p : new int[][]{{-17, 38, 8}, {17, 22, -8}, {-17, 6, 10}, {17, -14, -10},
                {-16, -32, 8}, {16, -46, -8}, {0, -24, 16}}) {
            orePocket(level, cx + p[0], p[1], cz + p[2], mid, 1);
        }
        for (int[] p : new int[][]{{-20, 18, 12}, {20, -2, -12}, {-20, -28, 12}, {20, -48, -12}}) {
            orePocket(level, cx + p[0], p[1], cz + p[2], high, 1);
        }
        for (int[] p : new int[][]{{-23, -22, 16}, {23, -50, -16}}) {
            orePocket(level, cx + p[0], p[1], cz + p[2], supreme, 1);
        }
    }

    /**
     * Join the surface approach to the first vein platform.  The flat
     * dimension generator starts as solid stone, so merely placing an
     * entrance marker at y=62 would leave an eight-block-high sealed wall
     * below the jindan approach (which ends at y=72).  A short, lit stair
     * gives the player a real walkable transition while keeping the L1
     * platform centered on the documented vein coordinates.
     */
    private static void buildVeinEntryStair(ServerLevel level, int cx, int entranceY, int cz) {
        int topY = entranceY + 10;
        // Carve first, then lay every tread.  If each step were placed while
        // carving the next one, the next step's headroom would erase the
        // previous tread at the shared z edge.
        for (int step = 0; step <= 10; step++) {
            int floorY = topY - step;
            int z = cz + step;
            // Clear a two-block-high walking volume around this tread.  The
            // final tread lands inside the radius-13 L1 chamber below.
            clearBox(level, cx - 3, floorY + 1, z - 1,
                    cx + 3, floorY + 2, z + 1);
        }
        // Clear the chamber landing before laying treads that pass through it.
        clearBox(level, cx - 4, entranceY + 1, cz + 7,
                cx + 4, entranceY + 3, cz + 12);
        for (int step = 0; step <= 10; step++) {
            int floorY = topY - step;
            int z = cz + step;
            for (int x = cx - 3; x <= cx + 3; x++) {
                set(level, x, floorY, z, x == cx - 3 || x == cx + 3
                        ? POLISHED_DEEPSLATE : QUARTZ);
            }
            if (step % 3 == 0 || step == 10) {
                set(level, cx - 4, floorY + 2, z, LANTERN);
                set(level, cx + 4, floorY + 2, z, LANTERN);
            }
        }
    }

    static void restoreSurfaceHub(ServerLevel level) {
        for(int z=151;z<=160;z++) {
            fill(level,-3,72,z,3,72,z,QUARTZ);
            clearBox(level,-3,73,z,3,76,z);
        }
        buildVeinEntryStair(level,0,62,160);
        if (level.getChunkSource().getGenerator() instanceof xiuxian.vein.VeinChunkGenerator) restoreVeinLadder(level);
    }

    private static void buildBossArena(ServerLevel level, LuoxiaInnerRealmData data) {
        int cx = 0, cy = -40, cz = 300;
        data.marker("boss_gate", new BlockPos(cx, cy + 4, cz - 78));
        data.marker("boss_approach", new BlockPos(cx, cy + 2, cz - 40));
        data.marker("boss_center", new BlockPos(cx, cy + 2, cz));
        data.marker("boss_ring_inner", new BlockPos(cx, cy + 2, cz + 16));
        data.marker("boss_ring_outer", new BlockPos(cx, cy + 2, cz + 36));
        data.marker("boss_exit", new BlockPos(cx, cy + 3, cz + 78));
        data.marker("boss_reward_vault", new BlockPos(cx + 48, cy + 2, cz));
        // Hollow the arena and its approach out of the flat generator's
        // stone.  Only the floor and intentionally placed walls remain solid.
        clearBox(level, cx - 52, cy + 1, cz - 84, cx + 52, cy + 22, cz + 84);
        // Continue the tunnel back to the bottom of the central vein shaft;
        // without this segment the arena gate would be a sealed island.
        clearBox(level, cx - 4, cy + 1, cz - 140, cx + 4, cy + 8, cz - 82);
        clearBox(level, cx - 4, cy + 1, cz - 84, cx + 4, cy + 8, cz - 38);
        wall(level, cx - 4, cy, cz - 82, cx + 4, cy + 12, cz - 76, DARK);
        gate(level, cx, cy + 1, cz - 78, 9, 10, DARK, AMETHYST);
        for (int z = cz - 74; z <= cz - 40; z++) {
            for (int x = -3; x <= 3; x++) set(level, cx + x, cy + 1, z, DEEPSLATE_TILE);
        }
        for (int z = cz - 140; z <= cz - 82; z++) {
            for (int x = -3; x <= 3; x++) set(level, cx + x, cy, z, DEEPSLATE_TILE);
        }
        circle(level, cx, cy, cz, 45, POLISHED_DEEPSLATE);
        ring(level, cx, cy + 1, cz, 45, 2, DARK);
        ring(level, cx, cy + 2, cz, 32, 2, AMETHYST);
        ring(level, cx, cy + 3, cz, 11, 2, QUARTZ);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6.0D;
            int x = cx + (int) Math.round(Math.cos(angle) * 38);
            int z = cz + (int) Math.round(Math.sin(angle) * 38);
            tower(level, x, cy + 2, z, 3, 8, AMETHYST);
            lampPost(level, x, cy + 2, z);
            data.marker("boss_seal_" + i, new BlockPos(x, cy + 3, z));
        }
        lampPost(level, cx, cy + 4, cz);
        fill(level, cx + 43, cy + 1, cz - 6, cx + 53, cy + 7, cz + 6, DARK);
        for (int x = cx + 45; x <= cx + 51; x += 2) set(level, x, cy + 3, cz, GLOW);
    }

    private static void addLighting(ServerLevel level) {
        // A cheap final pass keeps the processional route and landmark halls
        // readable even with the fixed-time dimension sky.
        for (int z = 10; z <= 110; z += 10) {
            set(level, -5, 70 - Math.min(4, Math.max(0, (z - 5) / 28)) + 2, z, LANTERN);
            set(level, 5, 70 - Math.min(4, Math.max(0, (z - 5) / 28)) + 2, z, LANTERN);
        }
        for (int y = 54; y >= -54; y -= 8) set(level, 4, y, 160, SOUL_LANTERN);
    }

    private static void courtyard(ServerLevel level, int cx, int y, int cz, int radius) {
        circle(level, cx, y, cz, radius, QUARTZ);
        ring(level, cx, y + 1, cz, radius, 1, COPPER);
        for (int x = cx - radius + 3; x <= cx + radius - 3; x += 8) lampPost(level, x, y + 1, cz);
    }

    private static void module(ServerLevel level, int cx, int y, int cz, int half, int variant) {
        if (variant == 1) {
            pavilion(level, cx, y, cz, half);
        } else if (variant == 2) {
            pagoda(level, cx, y, cz, half);
        } else {
            house(level, cx, y, cz, half, half, 6, RED);
        }
        lampPost(level, cx - half, y + 1, cz - half);
    }

    private static void house(ServerLevel level, int cx, int y, int cz, int halfX, int halfZ,
                               int height, BlockState wallState) {
        // Elliptical wall with a two-tier eave: this keeps the repeated city
        // modules distinct from Minecraft's default rectangular hut silhouette.
        ellipse(level, cx, y, cz, halfX, halfZ, POLISHED_DEEPSLATE, 1.0D);
        for (int dy = 1; dy <= height; dy++) {
            double taper = dy >= height - 2 ? 0.92D : 1.0D;
            ellipseBoundary(level, cx, y + dy, cz, halfX, halfZ, wallState, taper);
            ellipseClear(level, cx, y + dy, cz, halfX - 1, halfZ - 1, 0.67D);
        }
        ellipse(level, cx, y + height + 1, cz, halfX + 1, halfZ + 1, COPPER, 1.0D);
        ellipse(level, cx, y + height + 2, cz, Math.max(2, halfX - 2), Math.max(2, halfZ - 2),
                wallState, 1.0D);
        set(level, cx, y + 1, cz - halfZ, AIR());
        set(level, cx, y + 2, cz - halfZ, AIR());
        set(level, cx, y + 2, cz + halfZ, LANTERN);
        set(level, cx - halfX / 2, y + 3, cz - halfZ, LAMP);
        set(level, cx + halfX / 2, y + 3, cz - halfZ, LAMP);
    }

    private static void pavilion(ServerLevel level, int cx, int y, int cz, int radius) {
        ellipse(level, cx, y, cz, radius, radius, POLISHED_DEEPSLATE, 1.0D);
        for (int[] p : new int[][]{{-radius + 1, -radius + 1}, {-radius + 1, radius - 1},
                {radius - 1, -radius + 1}, {radius - 1, radius - 1}}) {
            pillar(level, cx + p[0], y + 1, cz + p[1], QUARTZ_PILLAR);
        }
        ellipse(level, cx, y + 7, cz, radius + 2, radius + 2, COPPER, 1.0D);
        ellipse(level, cx, y + 8, cz, Math.max(2, radius - 2), Math.max(2, radius - 2), RED, 1.0D);
        clearBox(level, cx - radius + 1, y + 1, cz - radius + 1,
                cx + radius - 1, y + 6, cz + radius - 1);
        lampPost(level, cx, y + 1, cz);
    }

    private static void gardenCourt(ServerLevel level, int cx, int y, int cz, int halfX, int halfZ) {
        ellipse(level, cx, y, cz, halfX, halfZ, MOSS, 1.0D);
        ring(level, cx, y + 1, cz, Math.max(2, Math.min(halfX, halfZ) - 2), 1, COPPER);
        for (int i = -1; i <= 1; i++) {
            shrineStone(level, cx + i * Math.max(2, halfX / 2), y + 1, cz, 2);
        }
        set(level, cx, y + 1, cz, WATER);
        set(level, cx, y + 2, cz, AMETHYST);
        lampPost(level, cx, y + 1, cz - halfZ + 1);
    }

    private static void pagoda(ServerLevel level, int cx, int y, int cz, int radius) {
        int floors = 3;
        for (int floor = 0; floor < floors; floor++) {
            int fy = y + floor * 4;
            int r = Math.max(3, radius - floor * 2);
            house(level, cx, fy, cz, r, r, 3, floor == 1 ? CALCITE : RED);
            ring(level, cx, fy + 4, cz, r + 2, 1, COPPER);
        }
        set(level, cx, y + floors * 4 + 2, cz, AMETHYST);
        set(level, cx, y + floors * 4 + 3, cz, LAMP);
    }

    private static void hall(ServerLevel level, int cx, int y, int cz, int halfX, int halfZ, int height) {
        house(level, cx, y, cz, halfX, halfZ, height, QUARTZ);
        for (int x = cx - halfX + 2; x <= cx + halfX - 2; x += 5) {
            pillar(level, x, y + 1, cz - halfZ + 1, QUARTZ_PILLAR);
            pillar(level, x, y + 1, cz + halfZ - 1, QUARTZ_PILLAR);
        }
        ring(level, cx, y + height + 2, cz, Math.max(4, Math.min(halfX, halfZ) - 2), 1, COPPER);
        lampPost(level, cx, y + 2, cz);
    }

    private static void gate(ServerLevel level, int cx, int y, int cz, int halfWidth, int height,
                              BlockState wallState, BlockState trim) {
        wall(level, cx - halfWidth, y, cz - 3, cx - halfWidth + 3, y + height, cz + 3, wallState);
        wall(level, cx + halfWidth - 3, y, cz - 3, cx + halfWidth, y + height, cz + 3, wallState);
        fill(level, cx - halfWidth, y + height, cz - 3, cx + halfWidth, y + height + 2, cz + 3, trim);
        for (int dx = -halfWidth + 4; dx <= halfWidth - 4; dx++) {
            set(level, cx + dx, y + 1, cz, AIR());
            set(level, cx + dx, y + 2, cz, AIR());
        }
    }

    private static void lampPost(ServerLevel level, int x, int y, int z) {
        for (int dy = 0; dy < 3; dy++) set(level, x, y + dy, z, COPPER);
        set(level, x, y + 3, z, LANTERN);
    }

    private static void tower(ServerLevel level, int x, int y, int z, int radius, int height, BlockState state) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y + dy, z + dz, state);
                }
            }
        }
        set(level, x, y + height, z, LAMP);
    }

    private static void pillar(ServerLevel level, int x, int y, int z, BlockState state) {
        for (int dy = 0; dy < 6; dy++) set(level, x, y + dy, z, state);
    }

    private static void wall(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                             BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                set(level, x, y, minZ, state);
                set(level, x, y, maxZ, state);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                set(level, minX, y, z, state);
                set(level, maxX, y, z, state);
            }
        }
    }

    private static void fill(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                             BlockState state) {
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) set(level, x, y, z, state);
            }
        }
    }

    private static void circle(ServerLevel level, int cx, int y, int cz, int radius, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void ellipse(ServerLevel level, int cx, int y, int cz, int rx, int rz,
                                BlockState state, double scale) {
        int sx = Math.max(1, (int) Math.ceil(rx * scale));
        int sz = Math.max(1, (int) Math.ceil(rz * scale));
        for (int dx = -sx; dx <= sx; dx++) {
            for (int dz = -sz; dz <= sz; dz++) {
                if (ellipseValue(dx, dz, sx, sz) <= 1.0D) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void ellipseBoundary(ServerLevel level, int cx, int y, int cz, int rx, int rz,
                                         BlockState state, double scale) {
        int sx = Math.max(1, (int) Math.ceil(rx * scale));
        int sz = Math.max(1, (int) Math.ceil(rz * scale));
        for (int dx = -sx; dx <= sx; dx++) {
            for (int dz = -sz; dz <= sz; dz++) {
                double value = ellipseValue(dx, dz, sx, sz);
                if (value <= 1.0D && value >= 0.58D) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void ellipseClear(ServerLevel level, int cx, int y, int cz, int rx, int rz,
                                      double scale) {
        int sx = Math.max(1, (int) Math.ceil(rx * scale));
        int sz = Math.max(1, (int) Math.ceil(rz * scale));
        for (int dx = -sx; dx <= sx; dx++) {
            for (int dz = -sz; dz <= sz; dz++) {
                if (ellipseValue(dx, dz, sx, sz) <= 1.0D) set(level, cx + dx, y, cz + dz, AIR());
            }
        }
    }

    private static double ellipseValue(int dx, int dz, int rx, int rz) {
        return (dx * (double) dx) / (rx * (double) rx)
                + (dz * (double) dz) / (rz * (double) rz);
    }

    private static void ring(ServerLevel level, int cx, int y, int cz, int radius, int thickness, BlockState state) {
        int inner = Math.max(0, radius - thickness);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance <= radius * radius && distance >= inner * inner) set(level, cx + dx, y, cz + dz, state);
            }
        }
    }

    private static void orePocket(ServerLevel level, int cx, int y, int cz, BlockState state, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius + 1) {
                        set(level, cx + dx, y + dy, cz + dz, state);
                    }
                }
            }
        }
    }

    private static void clearBox(ServerLevel level, int minX, int minY, int minZ,
                                 int maxX, int maxY, int maxZ) {
        fill(level, minX, minY, minZ, maxX, maxY, maxZ, AIR());
    }

    private static void clearCylinder(ServerLevel level, int cx, int baseY, int cz,
                                      int radius, int height) {
        for (int y = baseY; y < baseY + height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) set(level, cx + dx, y, cz + dz, AIR());
                }
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        if (y >= level.getMinBuildHeight() && y < level.getMaxBuildHeight()) {
            level.setBlock(new BlockPos(x, y, z), state, 3);
        }
    }

    private static BlockState AIR() {
        return Blocks.AIR.defaultBlockState();
    }
}
