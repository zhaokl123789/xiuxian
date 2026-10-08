package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class SectInstrumentBlock extends OrientalDecorationBlock implements EntityBlock {
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
    private final int voice;
    private final int tempo;

    public SectInstrumentBlock(Properties properties, double[][] boxes, int voice, int tempo) {
        super(properties, boxes);
        this.voice = voice;
        this.tempo = tempo;
        registerDefaultState(defaultBlockState().setValue(PLAYING, false));
    }

    public SoundEvent voice() {
        return switch (voice) {
            case 1 -> SoundEvents.NOTE_BLOCK_FLUTE.get();
            case 2 -> SoundEvents.NOTE_BLOCK_CHIME.get();
            case 3 -> SoundEvents.NOTE_BLOCK_BASEDRUM.get();
            case 4 -> SoundEvents.NOTE_BLOCK_BELL.get();
            case 5 -> SoundEvents.NOTE_BLOCK_HARP.get();
            default -> SoundEvents.NOTE_BLOCK_GUITAR.get();
        };
    }

    int tempo() { return tempo; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLAYING);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SectInstrumentBlockEntity(pos, state);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SectInstrumentBlockEntity instrument) {
            if (player.isShiftKeyDown()) instrument.nextTune();
            else instrument.toggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SectInstrumentBlockEntity instrument) instrument.playNextNote();
    }
}
