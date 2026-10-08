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
    boolean forceClearing;
    int version = LuoxiaBlueprint.VERSION;
    int detailsVersion;
    int detailsPlacement;
    long detailsCell;
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
        data.forceClearing = tag.getBoolean("ForceClearing");
        data.version = tag.getInt("Version");
        // Revision 3 changes ordered placements. Never migrate an old operation
        // cursor to the new blueprint, even if its numerical values are valid.
        data.detailsVersion = tag.getInt("DetailsVersion");
        data.detailsPlacement = tag.getInt("DetailsPlacement");
        data.detailsCell = tag.getLong("DetailsCell");
        if (data.version != LuoxiaBlueprint.VERSION && data.phase == Phase.COMPLETE
                && data.detailsVersion < LuoxiaBlueprint.DETAILS_VERSION) {
            // A pending legacy additive upgrade can safely restart: it only fills
            // air. Its old cursor cannot index the revised detail placements.
            data.detailsPlacement = 0;
            data.detailsCell = 0;
        }
        data.chunkIndex = tag.getInt("Chunk");
        data.operationIndex = tag.getInt("Operation");
        data.cellIndex = tag.getLong("Cell");
        data.changedBlocks = tag.getLong("Changed");
        if (data.problem.isEmpty()) data.problem = tag.getString("Problem");
        if (data.origin != null && (!data.validCursor() || data.detailsPlacement < 0 || data.detailsCell < 0)
                || data.origin == null && (data.chunkIndex != 0 || data.operationIndex != 0 || data.cellIndex != 0
                || data.changedBlocks < 0 || data.detailsPlacement < 0 || data.detailsCell < 0
                || data.phase != Phase.PLANNED)) {
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
        tag.putBoolean("ForceClearing", forceClearing);
        tag.putInt("Version", version);
        tag.putInt("DetailsVersion", detailsVersion);
        tag.putInt("DetailsPlacement", detailsPlacement);
        tag.putLong("DetailsCell", detailsCell);
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

    /**
     * Returns whether ordinary natural spawning should be suppressed at this position.
     * The protection starts once a site leaves the planning-only phase, so mobs cannot
     * accumulate while the mountain is being built or while a completed site is visited.
     */
    boolean protectsNaturalSpawnsAt(BlockPos pos) {
        if (origin == null || phase == null || phase == Phase.PLANNED) return false;
        return pos.getX() >= origin.getX() + LuoxiaBlueprint.MIN_X
                && pos.getX() <= origin.getX() + LuoxiaBlueprint.MAX_X
                && pos.getY() >= origin.getY() + LuoxiaBlueprint.MIN_Y
                && pos.getY() <= origin.getY() + LuoxiaBlueprint.MAX_Y
                && pos.getZ() >= origin.getZ() + LuoxiaBlueprint.MIN_Z
                && pos.getZ() <= origin.getZ() + LuoxiaBlueprint.MAX_Z;
    }

    boolean validCursor() {
        return LuoxiaConstruction.validCursor(this);
    }

    void nextPhase() {
        phase = Phase.values()[phase.ordinal() + 1];
        chunkIndex = 0;
        operationIndex = 0;
        cellIndex = 0;
        if (phase == Phase.COMPLETE) {
            detailsVersion = LuoxiaBlueprint.DETAILS_VERSION;
            detailsPlacement = 0;
            detailsCell = 0;
        }
        setDirty();
    }
}
