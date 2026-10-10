package xiuxian.vein;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Room ownership persists independently of block or entity unloading. */
public final class VeinEncounterData extends SavedData {
    public static final class Room {
        public final BlockPos center;
        public UUID boss;
        public boolean completed, rewarded;
        Room(BlockPos center) { this.center=center; }
    }
    public final Map<Long,Room> rooms=new HashMap<>();
    public static VeinEncounterData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(VeinEncounterData::load,VeinEncounterData::new,"xiuxian_vein_encounters");
    }
    public Room room(BlockPos center) { return rooms.computeIfAbsent(center.asLong(),key -> new Room(center.immutable())); }
    public static VeinEncounterData load(CompoundTag tag) {
        var data=new VeinEncounterData();
        var entries=tag.getCompound("Rooms");
        for(String id:entries.getAllKeys()) {
            var entry=entries.getCompound(id);var room=data.room(BlockPos.of(entry.getLong("Center")));
            room.boss=entry.hasUUID("Boss")?entry.getUUID("Boss"):null;room.completed=entry.getBoolean("Completed");room.rewarded=entry.getBoolean("Rewarded");
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var entries=new CompoundTag();
        rooms.forEach((key,room) -> {
            var entry=new CompoundTag();entry.putLong("Center",room.center.asLong());
            if(room.boss!=null) entry.putUUID("Boss",room.boss);
            entry.putBoolean("Completed",room.completed);entry.putBoolean("Rewarded",room.rewarded);entries.put(Long.toString(key),entry);
        });
        tag.put("Rooms",entries);return tag;
    }
}
