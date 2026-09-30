package xiuxian.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class TaixuIslandFeature extends Feature<NoneFeatureConfiguration> {
    public TaixuIslandFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int radiusX = 9 + random.nextInt(7);
        int radiusZ = 8 + random.nextInt(7);
        int depth = 5 + random.nextInt(5);
        int topY = Math.min(origin.getY(), level.getMaxBuildHeight() - 8);
        List<BlockPos> surface = new ArrayList<>();
        BlockPos islandCenter = null;

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                double nx = (double) dx / radiusX;
                double nz = (double) dz / radiusZ;
                double distance = nx * nx + nz * nz;
                if (distance > 1.0D) continue;

                int columnDepth = Math.max(2, (int) Math.ceil(depth * (1.08D - distance * 0.72D)));
                int columnTop = topY + (distance < 0.58D ? random.nextInt(3) - 1 : random.nextInt(2) - 1);
                BlockPos top = new BlockPos(origin.getX() + dx, columnTop, origin.getZ() + dz);
                for (int dy = 0; dy <= columnDepth; dy++) {
                    BlockPos pos = top.below(dy);
                    if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;
                    BlockState state;
                    if (dy == 0) {
                        state = random.nextInt(5) == 0 ? Blocks.GRASS_BLOCK.defaultBlockState()
                                : Blocks.MOSS_BLOCK.defaultBlockState();
                    } else if (dy == 1) {
                        state = random.nextInt(4) == 0 ? Blocks.ROOTED_DIRT.defaultBlockState()
                                : Blocks.DIRT.defaultBlockState();
                    } else if (dy == columnDepth) {
                        state = random.nextBoolean() ? Blocks.TUFF.defaultBlockState()
                                : Blocks.CALCITE.defaultBlockState();
                    } else {
                        state = random.nextInt(7) == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                                : Blocks.CALCITE.defaultBlockState();
                    }
                    if (level.isEmptyBlock(pos)) {
                        level.setBlock(pos, state, 2);
                    }
                }
                surface.add(top);
                if (dx == 0 && dz == 0) islandCenter = top;
            }
        }

        for (BlockPos top : surface) {
            if (random.nextInt(19) == 0 && level.isEmptyBlock(top.above())) {
                BlockState plant = switch (random.nextInt(3)) {
                    case 0 -> Blocks.ALLIUM.defaultBlockState();
                    case 1 -> Blocks.AZURE_BLUET.defaultBlockState();
                    default -> Blocks.PINK_PETALS.defaultBlockState();
                };
                level.setBlock(top.above(), plant, 2);
            }
        }

        if (radiusX >= 13 && radiusZ >= 12 && random.nextInt(3) == 0) {
            buildSkyRuin(level, islandCenter);
        } else if (random.nextBoolean()) {
            buildCrystalOutcrop(level, surface.get(random.nextInt(surface.size())), random);
        }
        return !surface.isEmpty();
    }

    private static void buildCrystalOutcrop(WorldGenLevel level, BlockPos surface, RandomSource random) {
        BlockPos base = surface.above();
        if (!level.isEmptyBlock(base)) return;
        int height = 2 + random.nextInt(3);
        for (int dy = 0; dy < height; dy++) {
            BlockPos pos = base.above(dy);
            if (!level.isEmptyBlock(pos)) break;
            level.setBlock(pos, dy == height - 1 ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                    : Blocks.CALCITE.defaultBlockState(), 2);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextBoolean()) {
                BlockPos crystal = base.above().relative(direction);
                if (level.isEmptyBlock(crystal)) level.setBlock(crystal, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
            }
        }
    }

    private static void buildSkyRuin(WorldGenLevel level, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = center.offset(dx, 1, dz);
                if (!level.isEmptyBlock(floor)) continue;
                BlockState state = dx == 0 && dz == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                        : (Math.abs(dx) == 2 || Math.abs(dz) == 2)
                        ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
                        : Blocks.QUARTZ_BRICKS.defaultBlockState();
                level.setBlock(floor, state, 2);
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                for (int dy = 2; dy <= 4; dy++) {
                    BlockPos pillar = center.offset(dx, dy, dz);
                    if (level.isEmptyBlock(pillar)) {
                        level.setBlock(pillar, dy == 4 ? Blocks.SEA_LANTERN.defaultBlockState()
                                : Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
