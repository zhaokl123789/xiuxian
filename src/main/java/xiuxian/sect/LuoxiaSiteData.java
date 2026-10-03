package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

final class LuoxiaSiteData extends SavedData {
    enum Phase { PLANNED, SURVEY, TERRAIN, BUILDINGS, WATER, COMPLETE }
    BlockPos origin;
    Phase phase = Phase.PLANNED;
    boolean paused;
    int version = LuoxiaBlueprint.VERSION;
    int chunkIndex;
    int operationIndex;
    long cellIndex;
    long changedBlocks;
    String problem = "";

    static LuoxiaSiteData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LuoxiaSiteData::load, LuoxiaSiteData::new, "xiuxian_luoxia");
    }

    static LuoxiaSiteData load(CompoundTag tag) {
        LuoxiaSiteData data = new LuoxiaSiteData();
        if (tag.contains("Origin")) data.origin = BlockPos.of(tag.getLong("Origin"));
        try {
            data.phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException invalidPhase) {
            data.paused = true;
            data.problem = "施工记录阶段无效，请检查存档备份。";
        }
        data.paused |= tag.getBoolean("Paused");
        data.version = tag.getInt("Version");
        data.chunkIndex = tag.getInt("Chunk");
        data.operationIndex = tag.getInt("Operation");
        data.cellIndex = tag.getLong("Cell");
        data.changedBlocks = tag.getLong("Changed");
        if (data.problem.isEmpty()) data.problem = tag.getString("Problem");
        if (data.origin != null && !data.validCursor()
                || data.origin == null && (data.chunkIndex != 0 || data.operationIndex != 0 || data.cellIndex != 0
                || data.changedBlocks < 0 || data.phase != Phase.PLANNED)) {
            data.paused = true;
            data.problem = "施工游标无效，已暂停，不能直接续建；请检查存档备份。";
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (origin != null) tag.putLong("Origin", origin.asLong());
        tag.putString("Phase", phase.name());
        tag.putBoolean("Paused", paused);
        tag.putInt("Version", version);
        tag.putInt("Chunk", chunkIndex);
        tag.putInt("Operation", operationIndex);
        tag.putLong("Cell", cellIndex);
        tag.putLong("Changed", changedBlocks);
        tag.putString("Problem", problem);
        return tag;
    }

    boolean active() {
        return origin != null && !paused && phase != Phase.PLANNED && phase != Phase.COMPLETE;
    }

    boolean validCursor() {
        return LuoxiaConstruction.validCursor(this);
    }

    void nextPhase() {
        phase = Phase.values()[phase.ordinal() + 1];
        chunkIndex = 0;
        operationIndex = 0;
        cellIndex = 0;
        setDirty();
    }
}
