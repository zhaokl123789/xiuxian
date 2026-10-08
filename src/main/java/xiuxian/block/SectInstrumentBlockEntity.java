package xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Short original pentatonic phrases played through Minecraft's bundled instrument samples. */
public final class SectInstrumentBlockEntity extends BlockEntity {
    private static final int[][] TUNES = {
        {0, 4, 7, 9, 7, 4, 2, 0, -1, 2, 4, 7, 12, 9, 7, 4},
        {12, 9, 7, 4, 7, 9, 12, -1, 7, 4, 2, 0, 2, 4, 7, 0},
        {0, 0, 7, -1, 4, 4, 9, -1, 7, 9, 12, 9, 7, 4, 2, 0}
    };
    private int tune;
    private int note;
    private int notesPlayed;

    public SectInstrumentBlockEntity(BlockPos pos, BlockState state) {
        super(SectBlocks.INSTRUMENT_ENTITY.get(), pos, state);
    }

    public int tune() { return tune; }
    public int note() { return note; }
    public int notesPlayed() { return notesPlayed; }

    public void toggle() {
        if (!(level instanceof ServerLevel server)) return;
        boolean start = !getBlockState().getValue(SectInstrumentBlock.PLAYING);
        note = 0;
        server.setBlock(worldPosition, getBlockState().setValue(SectInstrumentBlock.PLAYING, start), Block.UPDATE_ALL);
        setChanged();
        if (start) server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    public void nextTune() {
        tune = (tune + 1) % TUNES.length;
        note = 0;
        setChanged();
    }

    public void playNextNote() {
        if (!(level instanceof ServerLevel server) || !getBlockState().getValue(SectInstrumentBlock.PLAYING)
                || !(getBlockState().getBlock() instanceof SectInstrumentBlock block)) return;
        int pitch = TUNES[tune][note];
        if (pitch >= 0) {
            server.playSound(null, worldPosition, block.voice(), SoundSource.RECORDS, 0.8F,
                    (float) Math.pow(2, (pitch - 6) / 12.0));
            server.sendParticles(ParticleTypes.NOTE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.15,
                    worldPosition.getZ() + 0.5, 0, pitch / 24.0, 0, 0, 1);
            notesPlayed++;
        }
        note++;
        setChanged();
        if (note >= TUNES[tune].length) {
            note = 0;
            server.setBlock(worldPosition, getBlockState().setValue(SectInstrumentBlock.PLAYING, false), Block.UPDATE_ALL);
        } else server.scheduleTick(worldPosition, block, block.tempo());
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && getBlockState().getValue(SectInstrumentBlock.PLAYING))
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Tune", tune);
        tag.putInt("Note", note);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tune = Math.floorMod(tag.getInt("Tune"), TUNES.length);
        note = Math.floorMod(tag.getInt("Note"), TUNES[tune].length);
    }
}
