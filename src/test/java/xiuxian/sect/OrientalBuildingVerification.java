package xiuxian.sect;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Check final ordered voxels, including details that might be erased by later passes. */
final class OrientalBuildingVerification {
    static void verify(Map<String, Integer> sect, Path root) throws IOException {
        var plan = MortalTownGenerator.createPlan();
        int dx = plan.maxX - plan.minX + 1, dy = plan.maxY - plan.minY + 1;
        short[] townVoxels = new short[dx * dy * (plan.maxZ - plan.minZ + 1)];
        var palette = new ArrayList<BlockState>();
        palette.add(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        Map<BlockState, Short> ids = new java.util.HashMap<>();
        ids.put(palette.get(0), (short) 0);
        for (var op : plan.placements) {
            short id = ids.computeIfAbsent(op.state(), state -> {
                short next = (short) palette.size();
                palette.add(state);
                return next;
            });
            for (int z = op.minZ(); z <= op.maxZ(); z++) for (int x = op.minX(); x <= op.maxX(); x++) {
                int column = ((z - plan.minZ) * dx + x - plan.minX) * dy - plan.minY;
                Arrays.fill(townVoxels, column + op.minY(), column + op.maxY() + 1, id);
            }
        }
        Map<String, Integer> town = new TreeMap<>();
        long[] counts = new long[palette.size()];
        for (short id : townVoxels) counts[id]++;
        for (int id = 1; id < palette.size(); id++) {
            String name = BuiltInRegistries.BLOCK.getKey(palette.get(id).getBlock()).getPath();
            if (counts[id] > 0 && (name.startsWith("xian_") || name.startsWith("town_")))
                town.merge(name, (int) counts[id], Integer::sum);
        }
        int newBlocks = 0;
        int daotaiBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!key.getNamespace().equals("xiuxian")) continue;
            if (key.getPath().startsWith("xian_")) {
                newBlocks++;
                require(sect.containsKey(key.getPath()) || town.containsKey(key.getPath()),
                        "Registered decoration erased or unused in both buildings: " + key);
            } else if (key.getPath().startsWith("daotai_")) {
                daotaiBlocks++;
            }
        }
        require(newBlocks == 56, "Expected 56 new blocks, found " + newBlocks);
        require(daotaiBlocks >= 100, "Expected at least 100 Dao-Tai blocks, found " + daotaiBlocks);
        require(sect.size() >= 35 && town.size() >= 40, "Building integration is too sparse");
        Path output = root.resolve("build/oriental-preview/building-usage.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                "newBlockCount", newBlocks, "daotaiBlockCount", daotaiBlocks,
                "luoxiaSurvivingBlocks", sect, "townSurvivingBlocks", town)));
        System.out.println("PASS: all 56 Luoxia blocks survive; Dao-Tai library=" + daotaiBlocks
                + "; sect=" + sect.size() + ", town=" + town.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
