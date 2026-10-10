package xiuxian.vein;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.block.VeinBlocks;
import xiuxian.item.XiuxianItems;
import xiuxian.sect.LuoxiaInnerDimension;

/** Three elite trials using Minecraft's existing AI; no destructive Wither or dragon behavior. */
@Mod.EventBusSubscriber(modid="xiuxian")
public final class VeinEncounters {
    private static final String ROOM_TAG="XiuxianVeinRoom";
    private static final Map<ServerLevel,Map<Long,ServerBossEvent>> BARS=new WeakHashMap<>();
    private VeinEncounters() {}
    public static boolean toggleEntrance(ServerLevel level,BlockPos gate,boolean open) {
        if(!LuoxiaInnerDimension.isInner(level)) return false;
        int layer=VeinLayers.atY(gate.getY());if(layer%3!=0 || layer==0) return false;
        BlockPos center=VeinTerrain.room(gate.getX(),gate.getZ(),layer);
        if(layer==9 && Math.abs(gate.getX())<=27 && Math.abs(gate.getZ()-160)<=27) center=new BlockPos(0,VeinTerrain.floorY(9)+1,160);
        int radius=VeinTerrain.roomRadius(layer),dy=gate.getY()-center.getY();
        if(Math.abs(gate.getX()-center.getX())>2 || gate.getZ()!=center.getZ()-radius || dy<0 || dy>2) return false;
        BlockPos base=center.offset(0,0,-radius);
        if(!open && !level.getEntities(null,new AABB(base.offset(-2,0,0),base.offset(3,3,1))).isEmpty()) return true;
        for(int dx=-2;dx<=2;dx++)for(int y=0;y<3;y++) {
            BlockPos pos=base.offset(dx,y,0);if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)continue;
            var state=level.getBlockState(pos);
            if(state.getBlock() instanceof VeinSealBlock && !state.getValue(VeinSealBlock.POWERED)) level.setBlock(pos,state.setValue(VeinSealBlock.OPEN,open),3);
        }
        return true;
    }
    public static BlockPos centerForControl(BlockPos pos) {
        int layer=VeinLayers.atY(pos.getY());
        if(layer!=3 && layer!=6 && layer!=9) return null;
        BlockPos center=VeinTerrain.room(pos.getX(),pos.getZ(),layer);
        if(layer==9 && Math.abs(pos.getX())<=27 && Math.abs(pos.getZ()-160)<=27) center=new BlockPos(0,VeinTerrain.floorY(9)+1,160);
        return pos.equals(VeinTerrain.control(center))?center:null;
    }
    public static boolean activate(ServerLevel level,BlockPos pos,Player player) {
        if(!LuoxiaInnerDimension.isInner(level)) return false;
        BlockPos center=centerForControl(pos);if(center==null) return false;
        int layer=VeinLayers.atY(center.getY());
        if(!player.mayBuild() || player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>36) return true;
        var data=VeinEncounterData.get(level);var room=data.room(center);
        if(room.completed) { player.displayClientMessage(Component.literal("此处灵脉试炼已完成。"),true);deliver(level,data,room);return true; }
        if(room.boss!=null) { player.displayClientMessage(Component.literal("此处守脉者尚未被击败。"),true);return true; }
        if(level.getDifficulty()==Difficulty.PEACEFUL) { player.displayClientMessage(Component.literal("和平难度下无法开启守脉试炼。"),true);return true; }
        // Check the entire room is already loaded. Activation never generates or tickets extra chunks.
        if(!roomLoaded(level,center,layer)) { player.displayClientMessage(Component.literal("场域尚未完整显现。"),true);return true; }
        Mob boss=(layer==3?EntityType.IRON_GOLEM:layer==6?EntityType.BLAZE:EntityType.WITHER_SKELETON).create(level);
        if(boss==null) return true;
        boss.moveTo(center.getX()+3.5,center.getY()+1,center.getZ()+.5,180,0);
        boss.setCustomName(Component.literal(layer==3?"苍蓝晶卫":layer==6?"地火脉灵":"地心封印卫"));boss.setCustomNameVisible(true);boss.setPersistenceRequired();
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(layer==3?160:layer==6?220:320);
        if(boss.getAttribute(Attributes.ATTACK_DAMAGE)!=null) boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(layer==3?10:layer==6?12:15);
        if(boss.getAttribute(Attributes.MOVEMENT_SPEED)!=null) boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(layer==3?.24:layer==6?.24:.28);
        boss.setHealth(boss.getMaxHealth());boss.setTarget(player);
        if(boss instanceof NeutralMob neutral) { neutral.setPersistentAngerTarget(player.getUUID());neutral.setRemainingPersistentAngerTime(24000); }
        if(layer==9) { boss.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));boss.setDropChance(EquipmentSlot.MAINHAND,0); }
        boss.getPersistentData().putLong(ROOM_TAG,center.asLong());
        if(!level.addFreshEntity(boss)) return true;
        room.boss=boss.getUUID();data.setDirty();
        player.displayClientMessage(boss.getCustomName().copy().append(" · 试炼开始"),true);
        return true;
    }
    public static boolean roomLoaded(ServerLevel level,BlockPos center,int layer) {
        int radius=VeinTerrain.roomRadius(layer)+2;
        for(int cx=Math.floorDiv(center.getX()-radius,16);cx<=Math.floorDiv(center.getX()+radius,16);cx++)
            for(int cz=Math.floorDiv(center.getZ()-radius,16);cz<=Math.floorDiv(center.getZ()+radius,16);cz++)
                if(level.getChunkSource().getChunkNow(cx,cz)==null) return false;
        return true;
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(!(event.getEntity().level() instanceof ServerLevel level) || !LuoxiaInnerDimension.isInner(level)
                || !event.getEntity().getPersistentData().contains(ROOM_TAG)) return;
        var data=VeinEncounterData.get(level);BlockPos center=BlockPos.of(event.getEntity().getPersistentData().getLong(ROOM_TAG));
        var room=data.rooms.get(center.asLong());
        if(room==null || room.completed || !event.getEntity().getUUID().equals(room.boss)) return;
        room.completed=true;data.setDirty();deliver(level,data,room);
    }
    public static boolean deliver(ServerLevel level,VeinEncounterData data,VeinEncounterData.Room room) {
        if(!room.completed || room.rewarded) return false;
        BlockPos pos=VeinTerrain.cache(room.center);
        if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) return false;
        int layer=VeinLayers.atY(room.center.getY());
        ItemStack stones=new ItemStack(layer==3?XiuxianItems.MID_SPIRIT_STONE.get():layer==6?XiuxianItems.HIGH_SPIRIT_STONE.get():XiuxianItems.SUPREME_SPIRIT_STONE.get(),layer==9?4:8);
        ItemStack concentrate=new ItemStack(VeinBlocks.material("vein_"+VeinLayers.id(layer)+"_concentrate"),16);
        ItemStack tonic=new ItemStack(VeinBlocks.material("vein_spring_tonic"),2);
        room.rewarded=true;data.setDirty();
        // Preserve player storage; place into empty slots or drop the earned reward beside the cache.
        for(ItemStack reward:List.of(stones,concentrate,tonic)) {
            boolean placed=false;
            if(level.getBlockEntity(pos) instanceof VeinStationBlockEntity container && container.getContainerSize()==27) {
                for(int i=0;i<27;i++) if(container.getItem(i).isEmpty()) { container.setItem(i,reward);placed=true;break; }
            }
            if(!placed) net.minecraft.world.level.block.Block.popResource(level,pos,reward);
        }
        return true;
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if(event.getEntity().getPersistentData().contains(ROOM_TAG)) event.getDrops().clear();
    }
    @SubscribeEvent public static void removed(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        var entity=event.getEntity();
        if(!(event.getLevel() instanceof ServerLevel level) || !LuoxiaInnerDimension.isInner(level)
                || !entity.getPersistentData().contains(ROOM_TAG) || entity.getRemovalReason()==null
                || !entity.getRemovalReason().shouldDestroy()) return;
        var data=VeinEncounterData.get(level);var room=data.rooms.get(entity.getPersistentData().getLong(ROOM_TAG));
        if(room!=null && !room.completed && entity.getUUID().equals(room.boss)) { room.boss=null;data.setDirty(); }
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !LuoxiaInnerDimension.isInner(level) || level.getGameTime()%20!=0) return;
        var data=VeinEncounterData.get(level);
        var bars=BARS.computeIfAbsent(level,key -> new HashMap<>());
        for(var room:data.rooms.values()) {
            if(room.completed) { deliver(level,data,room);var bar=bars.remove(room.center.asLong());if(bar!=null) bar.removeAllPlayers();continue; }
            if(room.boss==null) continue;
            Entity entity=level.getEntity(room.boss);
            if(!(entity instanceof Mob boss) || !boss.isAlive()) { var bar=bars.remove(room.center.asLong());if(bar!=null) bar.removeAllPlayers();continue; }
            var bar=bars.computeIfAbsent(room.center.asLong(),key -> new ServerBossEvent(boss.getDisplayName(),BossEvent.BossBarColor.GREEN,BossEvent.BossBarOverlay.PROGRESS));
            bar.setProgress(boss.getHealth()/boss.getMaxHealth());
            var players=level.players().stream().filter(p -> p.isAlive() && p.distanceToSqr(boss)<4096 && !p.isSpectator()).toList();
            for(ServerPlayer p:new ArrayList<>(bar.getPlayers())) if(!players.contains(p)) bar.removePlayer(p);
            players.forEach(bar::addPlayer);
            if(boss.getTarget()==null || !boss.getTarget().isAlive()) players.stream().filter(p -> !p.isCreative()).findFirst().ifPresent(boss::setTarget);
            int radius=VeinTerrain.roomRadius(VeinLayers.atY(room.center.getY()))-2;
            if(Math.abs(boss.getX()-room.center.getX())>radius || Math.abs(boss.getZ()-room.center.getZ())>radius || Math.abs(boss.getY()-room.center.getY())>12)
                boss.teleportTo(room.center.getX()+3.5,room.center.getY()+1,room.center.getZ()+.5);
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level) { var bars=BARS.remove(level);if(bars!=null) bars.values().forEach(ServerBossEvent::removeAllPlayers); }
    }
}
