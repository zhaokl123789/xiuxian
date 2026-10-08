package xiuxian.sect;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Final-state geometry checks and diagnostic previews of the ordered residence blueprint. */
final class DaotaiGeometryVerification {
    private static final int X0 = DaotaiResidenceGenerator.MIN_X, X1 = DaotaiResidenceGenerator.MAX_X;
    private static final int Z0 = DaotaiResidenceGenerator.MIN_Z, Z1 = DaotaiResidenceGenerator.MAX_Z;
    private static final int Y0 = DaotaiResidenceGenerator.MIN_Y, Y1 = DaotaiResidenceGenerator.MAX_Y;
    private static final int DX = X1 - X0 + 1, DY = Y1 - Y0 + 1, DZ = Z1 - Z0 + 1;
    private final short[] voxels = new short[DX * DY * DZ];
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Short> ids = new HashMap<>();

    private DaotaiGeometryVerification(DaotaiResidenceGenerator.Plan plan) {
        palette.add(Blocks.AIR.defaultBlockState());
        ids.put(palette.get(0), (short) 0);
        for (var op : plan.placements) {
            short id = op.state().isAir() ? 0 : ids.computeIfAbsent(op.state(), state -> {
                palette.add(state);
                return (short) (palette.size() - 1);
            });
            for (int z = op.minZ(); z <= op.maxZ(); z++) for (int x = op.minX(); x <= op.maxX(); x++) {
                Arrays.fill(voxels, index(x, op.minY(), z), index(x, op.maxY(), z) + 1, id);
            }
        }
    }

    static void verify(DaotaiResidenceGenerator.Plan plan) throws IOException {
        var model = new DaotaiGeometryVerification(plan);
        model.verifyRoutes();
        int[] counts = new int[model.palette.size()];
        long occupied = 0;
        for (short id : model.voxels) if (id != 0) { counts[id]++; occupied++; }
        int unique = 0, lamps = 0, furnishings = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] == 0) continue;
            String name = name(model.palette.get(i));
            if (name.startsWith("daotai_")) unique++;
            if (name.startsWith("daotai_lamp_")) lamps += counts[i];
            if (name.startsWith("daotai_furniture_")) furnishings += counts[i];
        }
        require(unique >= 100, "Fewer than 100 exclusive decorations survive assembly: " + unique);
        require(lamps >= 100 && furnishings >= 40, "Decorations were erased by later construction");
        require(occupied > 250_000, "Residence lacks estate-scale architectural mass");
        Path output = Path.of(System.getProperty("xiuxian.verificationRoot", "."), "build", "daotai-preview");
        Files.createDirectories(output);
        model.render(output.resolve("daotai-isometric.png"));
        model.renderSection(output.resolve("daotai-axis.png"));
        Files.writeString(output.resolve("verification.txt"), "Footprint: 441 x 421; height: 225\n"
                + "Occupied blocks: " + occupied + "\nExclusive decoration states surviving: " + unique
                + "\nExclusive lamps: " + lamps + "\nExclusive furnishings: " + furnishings
                + "\nMain ascent, upper-floor approaches, palace aisles: PASS\n");
        System.out.printf("Dao-Tai geometry PASS: %,d blocks, %d exclusive states, %d lamps, %d furnishings%n",
                occupied, unique, lamps, furnishings);
        System.out.println("Dao-Tai diagnostic previews: " + output.toAbsolutePath());
    }

    private void verifyRoutes() {
        for (int z = 210; z >= -121; z--) {
            int y = DaotaiResidenceGenerator.axisHeight(z);
            walkable(0, y, z);
            require(Math.abs(y - DaotaiResidenceGenerator.axisHeight(z - 1)) <= 1, "Main stair is too steep");
        }
        for (int side : new int[] {-1, 1}) {
            checkAscent(side * 54, 74, -14, 113, -92);
            checkAscent(side * 34, 113, -32, 143, -92);
            for (int x = 13; x <= 34; x++) walkable(side * x, 143, -92);
            for (int x = 23; x <= 54; x++) walkable(side * x, 113, -92);
            for (int z = -32; z >= -92; z--) walkable(side * 42, 113, z);
            for (int x = 34; x <= 42; x++) walkable(side * x, 113, -32);
            for (int x = 0; x <= 54; x++) walkable(side * x, 74, -14);
        }
        // Each hall's full central aisle must remain usable after furniture and bridge placement.
        for (int[] hall : new int[][] {{0, 74, -92, 30}, {-112, 74, -86, 19},
                {112, 74, -86, 19}, {0, 113, -92, 19}, {0, 143, -92, 12}}) {
            for (int z = hall[2] - hall[3]; z <= hall[2] + hall[3]; z++) walkable(hall[0], hall[1], z);
        }
        require(state(48, 160, -150).is(Blocks.GOLD_BLOCK), "Golden halo rim was erased");
        require(state(42, 160, -150).getLightEmission() == 15, "Halo lighting was erased");
        require(state(182, 20, 20).isAir(), "Outer ring has become a solid ground platform");
    }

    private void checkAscent(int x, int low, int startZ, int high, int endZ) {
        int length = Math.abs(endZ - startZ);
        for (int i = 0; i <= length; i++) {
            int z = startZ + Integer.signum(endZ - startZ) * i;
            int y = low + i * (high - low) / length;
            walkable(x, y, z);
            if (i < length) require((i + 1) * (high - low) / length - i * (high - low) / length <= 1,
                    "Upper staircase requires jumping");
        }
    }

    private void walkable(int x, int y, int z) {
        String pos = x + "," + y + "," + z;
        require(!state(x, y, z).isAir(), "Route floor missing at " + pos);
        require(state(x, y + 2, z).isAir() && state(x, y + 3, z).isAir(), "Route headroom blocked at " + pos);
        BlockState step = state(x, y + 1, z);
        require(step.isAir() || step.getBlock() instanceof StairBlock, "Route blocked at " + pos + " by " + step);
    }

    private static int index(int x, int y, int z) { return ((z - Z0) * DX + x - X0) * DY + y - Y0; }
    private BlockState state(int x, int y, int z) {
        return x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > Y1
                ? palette.get(0) : palette.get(voxels[index(x, y, z)]);
    }
    private static String name(BlockState state) { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(); }
    private static int color(BlockState state) {
        String name = name(state);
        if (name.contains("gold") || name.contains("daotai_gate") || name.contains("daotai_roof")) return 0xd7b759;
        if (state.getLightEmission() > 0) return 0xebdcaa;
        if (name.contains("water") || name.contains("blue_stained")) return 0x639db8;
        if (name.contains("prismarine") || name.contains("formation") || name.contains("sky_floor")) return 0x315b72;
        if (name.contains("leaves") || name.contains("moss") || name.contains("garden")) return 0x507856;
        if (name.contains("log") || name.contains("furniture")) return 0x775644;
        if (name.contains("deepslate") || name.contains("tuff")) return 0x697078;
        return 0xe3e7e4;
    }

    private void render(Path path) throws IOException {
        int width = 1800, height = 1500;
        double scale = 2.25, ox = 900, oy = 820;
        int[] pixels = new int[width * height];
        float[] depth = new float[pixels.length];
        Arrays.fill(pixels, 0xe9eef0);
        Arrays.fill(depth, Float.NEGATIVE_INFINITY);
        int[] colors = palette.stream().mapToInt(DaotaiGeometryVerification::color).toArray();
        for (int z = Z0; z <= Z1; z++) for (int x = X0; x <= X1; x++) for (int y = Y0; y <= Y1; y++) {
            int id = voxels[index(x, y, z)];
            if (id == 0) continue;
            if (state(x, y + 1, z).isAir()) LuoxiaGeometryVerification.face(pixels, depth, width, height, ox, oy,
                    scale, colors[id], new int[][]{{x,y+1,z},{x+1,y+1,z},{x+1,y+1,z+1},{x,y+1,z+1}});
            if (state(x + 1, y, z).isAir()) LuoxiaGeometryVerification.face(pixels, depth, width, height, ox, oy,
                    scale, LuoxiaGeometryVerification.shade(colors[id], 0.74),
                    new int[][]{{x+1,y,z},{x+1,y+1,z},{x+1,y+1,z+1},{x+1,y,z+1}});
            if (state(x, y, z + 1).isAir()) LuoxiaGeometryVerification.face(pixels, depth, width, height, ox, oy,
                    scale, LuoxiaGeometryVerification.shade(colors[id], 0.9),
                    new int[][]{{x,y,z+1},{x+1,y,z+1},{x+1,y+1,z+1},{x,y+1,z+1}});
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Graphics2D g = image.createGraphics();
        caption(g, "Dao-Tai Cloud Palace | Ordered block geometry", "Simplified shapes and colors; not an in-game screenshot");
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private void renderSection(Path path) throws IOException {
        BufferedImage image = new BufferedImage(DZ * 3 + 60, DY * 3 + 110, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xe9eef0));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int z = Z0; z <= Z1; z++) for (int y = Y0; y <= Y1; y++) {
            if (state(0, y, z).isAir()) continue;
            g.setColor(new Color(color(state(0, y, z))));
            g.fillRect(30 + (z - Z0) * 3, 100 + (Y1 - y) * 3, 3, 3);
        }
        caption(g, "Dao-Tai | Central-axis section", "Rear / halo on the left; entrance / ascent on the right");
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void caption(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(0x374751));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString(title, 30, 40);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        g.drawString(subtitle, 30, 70);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
