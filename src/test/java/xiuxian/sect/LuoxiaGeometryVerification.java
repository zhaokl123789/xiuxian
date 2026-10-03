package xiuxian.sect;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Verifies and previews the exact terrain and ordered block operations used by construction. */
public final class LuoxiaGeometryVerification {
    private static final int DX = LuoxiaBlueprint.MAX_X - LuoxiaBlueprint.MIN_X + 1;
    private static final int DY = LuoxiaBlueprint.MAX_Y - LuoxiaBlueprint.MIN_Y + 1;
    private static final int DZ = LuoxiaBlueprint.MAX_Z - LuoxiaBlueprint.MIN_Z + 1;
    private final short[] voxels = new short[DX * DY * DZ];
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Short> paletteIds = new HashMap<>();
    private long occupied;

    private LuoxiaGeometryVerification() {
        id(Blocks.AIR.defaultBlockState());
    }

    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        bootstrapGeometry();
        long start = System.nanoTime();
        LuoxiaBlueprint blueprint = LuoxiaBlueprint.create();
        LuoxiaGeometryVerification model = new LuoxiaGeometryVerification();
        model.assemble(blueprint);
        model.verify(blueprint);
        verifyPersistence();
        Path output = Path.of("build", "luoxia-preview");
        Files.createDirectories(output);
        model.render(output.resolve("luoxia-isometric.png"), 2240, 1920, 3.35);
        model.renderAxis(output.resolve("luoxia-central-axis.png"));
        System.out.printf("Luoxia geometry PASS: %,d occupied blocks, %d block states, %d operations; %.2f s%n",
                model.occupied, model.palette.size(), blueprint.placements().size(),
                (System.nanoTime() - start) / 1_000_000_000.0);
        System.out.println("Generated-block previews: " + output.toAbsolutePath());
    }

    private static void bootstrapGeometry() {
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            // The plain verification JVM has no ModLauncher event transformations. This last
            // networking hook runs after vanilla block/registry bootstrap and is unused here.
            if (!(cause instanceof NoSuchMethodException)
                    || !"net.minecraftforge.network.NetworkEvent.<init>()".equals(cause.getMessage())) {
                throw error;
            }
            require(BuiltInRegistries.BLOCK.size() > 500, "Vanilla block bootstrap did not finish");
            System.out.println("NOTE: geometry-only JVM omits Forge network hooks; no event/server integration is tested here.");
        }
    }

    private short id(BlockState state) {
        if (state.isAir()) {
            state = Blocks.AIR.defaultBlockState();
        }
        Short existing = paletteIds.get(state);
        if (existing != null) {
            return existing;
        }
        require(palette.size() < Short.MAX_VALUE, "Too many block states");
        short next = (short) palette.size();
        palette.add(state);
        paletteIds.put(state, next);
        return next;
    }

    private static int index(int x, int y, int z) {
        return ((z - LuoxiaBlueprint.MIN_Z) * DX + x - LuoxiaBlueprint.MIN_X) * DY
                + y - LuoxiaBlueprint.MIN_Y;
    }

    private void assemble(LuoxiaBlueprint blueprint) {
        for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
            for (int x = LuoxiaBlueprint.MIN_X; x <= LuoxiaBlueprint.MAX_X; x++) {
                int terrainTop = LuoxiaTerrain.heightAt(x, z);
                for (int y = LuoxiaBlueprint.MIN_Y; y <= terrainTop; y++) {
                    voxels[index(x, y, z)] = id(LuoxiaTerrain.blockAt(x, y, z, terrainTop));
                }
            }
        }
        List<LuoxiaBlueprint.Placement> ordered = new ArrayList<>();
        ordered.addAll(blueprint.placements().stream().filter(p -> !p.state().is(Blocks.WATER)).toList());
        ordered.addAll(blueprint.placements().stream().filter(p -> p.state().is(Blocks.WATER)).toList());
        for (LuoxiaBlueprint.Placement operation : ordered) {
            require(operation.minX() >= LuoxiaBlueprint.MIN_X && operation.maxX() <= LuoxiaBlueprint.MAX_X
                            && operation.minY() >= LuoxiaBlueprint.MIN_Y && operation.maxY() <= LuoxiaBlueprint.MAX_Y
                            && operation.minZ() >= LuoxiaBlueprint.MIN_Z && operation.maxZ() <= LuoxiaBlueprint.MAX_Z,
                    "Operation outside construction bounds: " + operation);
            require(operation.minX() <= operation.maxX() && operation.minY() <= operation.maxY()
                            && operation.minZ() <= operation.maxZ(), "Inverted operation: " + operation);
            short stateId = id(operation.state());
            for (int z = operation.minZ(); z <= operation.maxZ(); z++) {
                for (int x = operation.minX(); x <= operation.maxX(); x++) {
                    Arrays.fill(voxels, index(x, operation.minY(), z), index(x, operation.maxY(), z) + 1, stateId);
                }
            }
        }
        for (short voxel : voxels) {
            if (voxel != 0) {
                occupied++;
            }
        }
    }

    private void verify(LuoxiaBlueprint blueprint) {
        require(occupied > 300_000, "Mountain and buildings are unexpectedly empty");
        require(blueprint.placements().size() > 50, "Architectural model is unexpectedly empty");
        for (int z = 112; z >= -162; z--) {
            int floor = LuoxiaTerrain.pathHeight(z);
            require(!state(0, floor, z).isAir(), "Ascent lacks a floor at z=" + z);
            require(state(0, floor + 2, z).isAir() && state(0, floor + 3, z).isAir(),
                    "Ascent headroom is blocked at z=" + z);
            BlockState step = state(0, floor + 1, z);
            if (!step.isAir()) {
                require(step.getBlock() instanceof StairBlock && step.getValue(StairBlock.FACING) == Direction.NORTH,
                        "Ascent requires a jump over a solid obstruction at z=" + z);
            }
            if (z > -162) {
                int change = LuoxiaTerrain.pathHeight(z - 1) - floor;
                require(change >= 0 && change <= 1, "Ascent has a non-walkable height change at z=" + z);
            }
        }
        require(state(0, 133, -163).getBlock() instanceof StairBlock, "Hall podium lacks its joining step");
        require(state(0, 134, -163).isAir() && state(0, 135, -163).isAir(), "Hall podium step is obstructed");
        for (int[] door : new int[][]{{0, 19, 64}, {0, 49, -13}, {0, 85, -91},
                {0, 134, -169}, {0, 134, -209}, {32, 134, -189}, {-32, 134, -189}}) {
            walkable(door[0], door[1], door[2], "door passage");
        }
        for (int z = -208; z <= -170; z++) {
            walkable(0, 134, z, "main hall central aisle");
        }
        for (int x = -112; x <= -63; x++) {
            walkable(x, 85, -103, "western covered bridge");
        }
        for (int x = 70; x <= 120; x++) {
            walkable(x, 133, -180, "eastern covered bridge");
        }
        for (int y = -4; y <= -1; y++) {
            for (int x = LuoxiaBlueprint.MIN_X; x <= LuoxiaBlueprint.MAX_X; x++) {
                require(state(x, y, LuoxiaBlueprint.MIN_Z).is(Blocks.GRAVEL)
                                && state(x, y, LuoxiaBlueprint.MAX_Z).is(Blocks.GRAVEL),
                        "Lake shoreline is open on a north/south construction boundary");
            }
            for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
                require(state(LuoxiaBlueprint.MIN_X, y, z).is(Blocks.GRAVEL)
                                && state(LuoxiaBlueprint.MAX_X, y, z).is(Blocks.GRAVEL),
                        "Lake shoreline is open on an east/west construction boundary");
            }
        }
        int roofBlocks = 0;
        for (int z = -217; z <= -157; z++) {
            for (int x = -42; x <= 42; x++) {
                for (int y = 149; y <= 188; y++) {
                    String name = blockName(state(x, y, z));
                    if (name.contains("deepslate") || name.contains("blackstone")) {
                        roofBlocks++;
                    }
                }
            }
        }
        require(roofBlocks > 1000, "Main hall lacks its tiled roof silhouette");
        Random random = new Random(0x10A51A);
        for (int i = 0; i < 2048; i++) {
            int x = LuoxiaBlueprint.MIN_X + random.nextInt(DX);
            int z = LuoxiaBlueprint.MIN_Z + random.nextInt(DZ);
            int y = LuoxiaBlueprint.MIN_Y + random.nextInt(DY);
            BlockState architecture = blueprint.sampleArchitecture(x, y, z);
            BlockState expected = architecture != null ? architecture : LuoxiaTerrain.blockAt(x, y, z);
            require(state(x, y, z).equals(expected), "Ordered block assembly disagrees at " + x + "," + y + "," + z);
            int height = LuoxiaTerrain.heightAt(x, z);
            require(height == LuoxiaTerrain.heightAt(x, z), "Terrain is nondeterministic");
            require(height <= LuoxiaBlueprint.MAX_Y, "Terrain exceeds build height");
        }
        System.out.println("PASS: construction bounds, ordered operation sampling, continuous ascent, gate passages, hall aisle, roof mass");
    }

    private void walkable(int x, int feetY, int z, String label) {
        require(!state(x, feetY - 1, z).isAir(), label + " lacks a floor at " + x + "," + feetY + "," + z);
        require(state(x, feetY, z).isAir() && state(x, feetY + 1, z).isAir(),
                label + " is blocked at " + x + "," + feetY + "," + z + ": "
                        + state(x, feetY, z) + " / " + state(x, feetY + 1, z));
    }

    private static void verifyPersistence() {
        BlockPos origin = new BlockPos(-480_007, 72, 345_619);
        for (LuoxiaSiteData.Phase phase : LuoxiaSiteData.Phase.values()) {
            for (boolean paused : new boolean[]{false, true}) {
                for (boolean forceClearing : new boolean[]{false, true}) {
                    LuoxiaSiteData original = new LuoxiaSiteData();
                    original.origin = origin;
                    original.phase = phase;
                    original.paused = paused;
                    original.forceClearing = forceClearing;
                    original.chunkIndex = phase == LuoxiaSiteData.Phase.PLANNED || phase == LuoxiaSiteData.Phase.COMPLETE ? 0 : 307;
                    original.operationIndex = 0;
                    original.cellIndex = 0;
                    original.changedBlocks = 5_600_000_031L;
                    original.problem = "preserved reason";
                    LuoxiaSiteData restored = LuoxiaSiteData.load(original.save(new CompoundTag()));
                    require(origin.equals(restored.origin) && restored.phase == phase && restored.paused == paused,
                            "Saved site identity/phase/pause state did not survive reload");
                    require(restored.chunkIndex == original.chunkIndex && restored.operationIndex == 0
                                    && restored.cellIndex == 0 && restored.changedBlocks == 5_600_000_031L && restored.validCursor(),
                            "Saved construction cursor did not survive reload");
                    require(restored.version == original.version && restored.problem.equals(original.problem),
                            "Saved version/pause reason did not survive reload");
                    require(restored.forceClearing == forceClearing,
                            "Saved clearing mode did not survive reload");
                    boolean active = !paused && phase != LuoxiaSiteData.Phase.PLANNED && phase != LuoxiaSiteData.Phase.COMPLETE;
                    require(restored.active() == active, "Restored site incorrectly resumes a planned/completed/paused job");
                }
            }
        }
        LuoxiaSiteData data = new LuoxiaSiteData();
        data.origin = origin;
        data.phase = LuoxiaSiteData.Phase.SURVEY;
        data.forceClearing = true;
        data.chunkIndex = 77;
        data.operationIndex = 11;
        data.cellIndex = 12003;
        data.changedBlocks = 700;
        data.nextPhase();
        require(data.phase == LuoxiaSiteData.Phase.TERRAIN && data.chunkIndex == 0 && data.operationIndex == 0
                        && data.cellIndex == 0 && data.changedBlocks == 700 && origin.equals(data.origin) && data.forceClearing,
                "Phase transition failed to reset work cursor while preserving site/progress");
        CompoundTag legacy = data.save(new CompoundTag());
        legacy.remove("ForceClearing");
        LuoxiaSiteData restoredLegacy = LuoxiaSiteData.load(legacy);
        require(!restoredLegacy.forceClearing && restoredLegacy.validCursor() && !restoredLegacy.paused,
                "A legacy record without a clearing-mode key should remain protected and resumable");
        CompoundTag unknownPhase = data.save(new CompoundTag());
        unknownPhase.putString("Phase", "unrecognized-phase");
        LuoxiaSiteData corrupt = LuoxiaSiteData.load(unknownPhase);
        require(corrupt.paused && !corrupt.active() && !corrupt.problem.isEmpty(), "Unknown phase should stop construction");
        for (String field : new String[]{"Chunk", "Operation", "Cell", "Changed"}) {
            CompoundTag invalidCursor = data.save(new CompoundTag());
            if (field.equals("Cell") || field.equals("Changed")) invalidCursor.putLong(field, -1);
            else invalidCursor.putInt(field, -1);
            LuoxiaSiteData invalid = LuoxiaSiteData.load(invalidCursor);
            require(invalid.paused && !invalid.active() && !invalid.validCursor() && !invalid.problem.isEmpty(),
                    "Negative saved cursor was accepted: " + field);
        }
        CompoundTag invalidChunk = data.save(new CompoundTag());
        invalidChunk.putInt("Chunk", LuoxiaConstruction.chunkCount(origin) + 1);
        LuoxiaSiteData outside = LuoxiaSiteData.load(invalidChunk);
        require(outside.paused && !outside.active() && !outside.validCursor(), "Out-of-range chunk cursor was accepted");
        verifyCursorLimits();
        require(LuoxiaSiteData.load(new LuoxiaSiteData().save(new CompoundTag())).origin == null,
                "Empty plan acquired a construction origin during reload");
        for (int offset = -31; offset <= 31; offset++) {
            BlockPos shifted = origin.offset(offset, 0, -offset);
            int expected = 0;
            for (int z = (shifted.getZ() + LuoxiaBlueprint.MIN_Z) >> 4;
                 z <= (shifted.getZ() + LuoxiaBlueprint.MAX_Z) >> 4; z++) {
                for (int x = (shifted.getX() + LuoxiaBlueprint.MIN_X) >> 4;
                     x <= (shifted.getX() + LuoxiaBlueprint.MAX_X) >> 4; x++) expected++;
            }
            require(LuoxiaConstruction.chunkCount(shifted) == expected, "Chunk coverage is incorrect at a shifted origin");
        }
        System.out.println("PASS: NBT phase/cursor/pause/clearing-mode persistence, legacy-safe default, cursor bounds, clipped volumes, chunk coverage");
    }

    private static void verifyCursorLimits() {
        for (LuoxiaSiteData.Phase phase : LuoxiaSiteData.Phase.values()) {
            LuoxiaSiteData data = new LuoxiaSiteData();
            data.origin = new BlockPos(0, 64, 0);
            data.phase = phase;
            require(data.validCursor(), "A newly entered phase rejected its empty cursor: " + phase);
            for (String field : new String[]{"Operation", "Cell"}) {
                CompoundTag corrupt = data.save(new CompoundTag());
                if (field.equals("Cell")) corrupt.putLong(field, Long.MAX_VALUE);
                else corrupt.putInt(field, Integer.MAX_VALUE);
                LuoxiaSiteData invalid = LuoxiaSiteData.load(corrupt);
                require(invalid.paused && !invalid.validCursor() && !invalid.active(),
                        "A positive out-of-range " + field + " cursor was accepted in " + phase);
            }
            if (phase == LuoxiaSiteData.Phase.PLANNED || phase == LuoxiaSiteData.Phase.COMPLETE) {
                data.chunkIndex = 1;
                require(!data.validCursor(), "A planned/completed phase accepted a work chunk");
                continue;
            }
            data.chunkIndex = LuoxiaConstruction.chunkCount(data.origin);
            require(data.validCursor(), "A just-completed phase rejected its terminal chunk cursor");
            data.cellIndex = 1;
            require(!data.validCursor(), "A terminal chunk accepted a nonempty cell cursor");
        }
        LuoxiaSiteData terrain = new LuoxiaSiteData();
        terrain.origin = new BlockPos(0, 64, 0);
        terrain.phase = LuoxiaSiteData.Phase.TERRAIN;
        terrain.chunkIndex = LuoxiaConstruction.chunkCount(terrain.origin) - 1;
        // The aligned northeast corner has exactly one x/z column, not a full 16 x 16 chunk.
        terrain.cellIndex = LuoxiaBlueprint.MAX_Y - LuoxiaBlueprint.MIN_Y + 1;
        require(terrain.validCursor(), "The edge column's exact end cursor was rejected");
        terrain.cellIndex++;
        require(!terrain.validCursor(), "The edge column accepted a cursor beyond its clipped volume");

        LuoxiaSiteData hall = new LuoxiaSiteData();
        hall.origin = terrain.origin;
        hall.phase = LuoxiaSiteData.Phase.BUILDINGS;
        hall.chunkIndex = 157;
        // This chunk starts with the summit courtyard's 16 x 11 x 4 stone foundation slice.
        hall.cellIndex = 16L * 11 * 4;
        require(hall.validCursor(), "A clipped summit-foundation end cursor was rejected");
        hall.cellIndex++;
        require(!hall.validCursor(), "A building cursor exceeded its chunk-clipped foundation slice");
    }

    private BlockState state(int x, int y, int z) {
        if (x < LuoxiaBlueprint.MIN_X || x > LuoxiaBlueprint.MAX_X
                || y < LuoxiaBlueprint.MIN_Y || y > LuoxiaBlueprint.MAX_Y
                || z < LuoxiaBlueprint.MIN_Z || z > LuoxiaBlueprint.MAX_Z) {
            return Blocks.AIR.defaultBlockState();
        }
        return palette.get(voxels[index(x, y, z)]);
    }

    private static String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    private static int color(BlockState state) {
        String name = blockName(state);
        if (name.contains("water")) return 0x398fba;
        if (name.contains("leaves")) return name.contains("cherry") ? 0xbf725d : 0x456b42;
        if (name.contains("gold") || name.contains("yellow")) return 0xcbb158;
        if (name.contains("deepslate") || name.contains("blackstone")) return 0x465764;
        if (name.contains("red_terracotta") || name.contains("red_nether")) return 0x91483a;
        if (name.contains("spruce") || name.contains("dark_oak")) return 0x6b493b;
        if (name.contains("log") || name.contains("wood")) return 0x7e6551;
        if (name.contains("quartz") || name.contains("calcite") || name.contains("white")) return 0xdddccb;
        if (name.contains("lantern") || name.contains("glowstone") || name.contains("shroomlight")) return 0xe8be76;
        if (name.contains("grass") || name.contains("moss")) return 0x628257;
        if (name.contains("dirt") || name.contains("podzol")) return 0x797260;
        if (name.contains("sand")) return 0xc2b897;
        if (name.contains("glass")) return 0xb1c7cb;
        if (name.contains("tuff")) return 0x848880;
        if (name.contains("andesite")) return 0x9b9e98;
        if (name.contains("smooth_stone") || name.contains("stone_brick")) return 0xa9b0ae;
        return 0xaaa99f;
    }

    private void render(Path path, int width, int height, double scale) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int background = 0xe9eef0;
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, background);
        float[] depth = new float[pixels.length];
        Arrays.fill(depth, Float.NEGATIVE_INFINITY);
        double minU = (LuoxiaBlueprint.MIN_X - LuoxiaBlueprint.MAX_Z - 1) * 0.8660254;
        double maxU = (LuoxiaBlueprint.MAX_X + 1 - LuoxiaBlueprint.MIN_Z) * 0.8660254;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
            for (int x = LuoxiaBlueprint.MIN_X; x <= LuoxiaBlueprint.MAX_X; x++) {
                int bottom = LuoxiaBlueprint.MIN_Y;
                while (bottom <= LuoxiaBlueprint.MAX_Y && state(x, bottom, z).isAir()) bottom++;
                if (bottom > LuoxiaBlueprint.MAX_Y) continue;
                int top = LuoxiaBlueprint.MAX_Y;
                while (top >= bottom && state(x, top, z).isAir()) top--;
                minV = Math.min(minV, (x + z) * 0.5 - top - 1);
                maxV = Math.max(maxV, (x + z + 2) * 0.5 - bottom);
            }
        }
        scale = Math.min(scale, Math.min((width - 96) / (maxU - minU), (height - 150) / (maxV - minV)));
        double originX = (width - (maxU - minU) * scale) / 2.0 - minU * scale;
        double originY = 114 - minV * scale;
        int[] colors = palette.stream().mapToInt(LuoxiaGeometryVerification::color).toArray();
        for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
            for (int x = LuoxiaBlueprint.MIN_X; x <= LuoxiaBlueprint.MAX_X; x++) {
                for (int y = LuoxiaBlueprint.MIN_Y; y <= LuoxiaBlueprint.MAX_Y; y++) {
                    int stateId = voxels[index(x, y, z)];
                    if (stateId == 0) continue;
                    if (state(x, y + 1, z).isAir()) {
                        face(pixels, depth, width, height, originX, originY, scale, colors[stateId],
                                new int[][]{{x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}});
                    }
                    if (state(x + 1, y, z).isAir()) {
                        face(pixels, depth, width, height, originX, originY, scale, shade(colors[stateId], 0.74),
                                new int[][]{{x + 1, y, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x + 1, y, z + 1}});
                    }
                    if (state(x, y, z + 1).isAir()) {
                        face(pixels, depth, width, height, originX, originY, scale, shade(colors[stateId], 0.9),
                                new int[][]{{x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}});
                    }
                }
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(0x374751));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.drawString("Luoxia Sect | Actual generated block geometry", 48, 54);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        graphics.drawString("Diagnostic voxel preview; simplified block shapes and colors, not an in-game screenshot", 48, 86);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static int shade(int rgb, double factor) {
        return ((int) (((rgb >> 16) & 255) * factor) << 16)
                | ((int) (((rgb >> 8) & 255) * factor) << 8) | (int) ((rgb & 255) * factor);
    }

    private static void face(int[] pixels, float[] depth, int width, int height, double ox, double oy,
                             double scale, int rgb, int[][] corners) {
        double[][] vertices = new double[4][3];
        for (int i = 0; i < 4; i++) {
            int[] p = corners[i];
            vertices[i][0] = ox + (p[0] - p[2]) * scale * 0.8660254;
            vertices[i][1] = oy + (p[0] + p[2]) * scale * 0.5 - p[1] * scale;
            vertices[i][2] = p[0] + p[2] + p[1];
        }
        triangle(pixels, depth, width, height, rgb, vertices[0], vertices[1], vertices[2]);
        triangle(pixels, depth, width, height, rgb, vertices[0], vertices[2], vertices[3]);
    }

    private static void triangle(int[] pixels, float[] depth, int width, int height, int rgb,
                                 double[] a, double[] b, double[] c) {
        int left = Math.max(0, (int) Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int right = Math.min(width - 1, (int) Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int top = Math.max(0, (int) Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int bottom = Math.min(height - 1, (int) Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        double denominator = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
        if (Math.abs(denominator) < 0.00001) return;
        for (int py = top; py <= bottom; py++) {
            for (int px = left; px <= right; px++) {
                double wa = ((b[1] - c[1]) * (px + 0.5 - c[0]) + (c[0] - b[0]) * (py + 0.5 - c[1])) / denominator;
                double wb = ((c[1] - a[1]) * (px + 0.5 - c[0]) + (a[0] - c[0]) * (py + 0.5 - c[1])) / denominator;
                double wc = 1 - wa - wb;
                if (wa < -0.00001 || wb < -0.00001 || wc < -0.00001) continue;
                float distance = (float) (wa * a[2] + wb * b[2] + wc * c[2]);
                int index = py * width + px;
                if (distance > depth[index]) {
                    pixels[index] = rgb;
                    depth[index] = distance;
                }
            }
        }
    }

    private void renderAxis(Path path) throws IOException {
        int cell = 4;
        BufferedImage image = new BufferedImage(DZ * cell, DY * cell + 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0xe9eef0));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int z = LuoxiaBlueprint.MIN_Z; z <= LuoxiaBlueprint.MAX_Z; z++) {
            for (int y = LuoxiaBlueprint.MIN_Y; y <= LuoxiaBlueprint.MAX_Y; y++) {
                BlockState state = state(0, y, z);
                if (!state.isAir()) {
                    graphics.setColor(new Color(color(state)));
                    graphics.fillRect((z - LuoxiaBlueprint.MIN_Z) * cell,
                            80 + (LuoxiaBlueprint.MAX_Y - y) * cell, cell, cell);
                }
            }
        }
        graphics.setColor(new Color(0x374751));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.drawString("Luoxia Sect | Central-axis block section (x = 0)", 24, 36);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        graphics.drawString("Summit / rear on the left; approach / front on the right", 24, 62);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
