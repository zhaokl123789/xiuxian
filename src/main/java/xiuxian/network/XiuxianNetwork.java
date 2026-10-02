package xiuxian.network;

import java.util.Optional;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import xiuxian.client.ClientScreens;
import xiuxian.cultivation.CultivationEvents;
import xiuxian.cultivation.CultivationData;

public final class XiuxianNetwork {
    private static final String PROTOCOL_VERSION = "8";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("xiuxian", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int nextMessageId;

    private XiuxianNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(nextMessageId++, IdentityScreenPacket.class,
                (message, buffer) -> {}, buffer -> new IdentityScreenPacket(),
                XiuxianNetwork::handleOpenIdentityScreen,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextMessageId++, IdentitySelectedPacket.class,
                (message, buffer) -> {}, buffer -> new IdentitySelectedPacket(),
                XiuxianNetwork::handleIdentitySelected,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextMessageId++, SelectIdentityPacket.class,
                (message, buffer) -> {
                    buffer.writeUtf(message.familyId);
                    buffer.writeUtf(message.pathId);
                },
                buffer -> new SelectIdentityPacket(buffer.readUtf(32), buffer.readUtf(32)),
                XiuxianNetwork::handleSelectIdentity,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextMessageId++, ChannelInterruptPacket.class,
                (message, buffer) -> {}, buffer -> new ChannelInterruptPacket(),
                XiuxianNetwork::handleChannelInterrupt,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextMessageId++, VoidWalkPacket.class,
                (message, buffer) -> {}, buffer -> new VoidWalkPacket(),
                XiuxianNetwork::handleVoidWalk,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextMessageId++, EquipSpellPacket.class,
                (message, buffer) -> {
                    buffer.writeVarInt(message.slot);
                    buffer.writeUtf(message.spellId, 64);
                }, buffer -> new EquipSpellPacket(buffer.readVarInt(), buffer.readUtf(64)),
                XiuxianNetwork::handleEquipSpell, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextMessageId++, CastSpellPacket.class,
                (message, buffer) -> buffer.writeUtf(message.spellId, 64),
                buffer -> new CastSpellPacket(buffer.readUtf(64)),
                XiuxianNetwork::handleCastSpell, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextMessageId++, TrueQiSyncPacket.class,
                (message, buffer) -> buffer.writeVarInt(message.trueQi),
                buffer -> new TrueQiSyncPacket(buffer.readVarInt()),
                XiuxianNetwork::handleTrueQiSync,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextMessageId++, CultivationSyncPacket.class,
                (message, buffer) -> {
                    buffer.writeBoolean(message.initialized);
                    buffer.writeUtf(message.familyId);
                    buffer.writeUtf(message.pathId);
                    buffer.writeUtf(message.realmId);
                    buffer.writeVarInt(message.realmLevel);
                    buffer.writeVarInt(message.qi);
                    buffer.writeVarInt(message.breakthroughCost);
                    buffer.writeUtf(message.techniqueId);
                    buffer.writeBoolean(message.meditating);
                    buffer.writeVarInt(message.spiritualRoot);
                    buffer.writeVarInt(message.constitution);
                    buffer.writeVarInt(message.comprehension);
                    buffer.writeVarInt(message.fortune);
                    buffer.writeUtf(message.studyingTechniqueId);
                    buffer.writeVarInt(message.techniqueStudyTicks);
                    buffer.writeVarInt(message.techniqueStudyDuration);
                    buffer.writeVarInt(message.techniqueStudyChance);
                    buffer.writeVarInt(message.trueQi);
                    buffer.writeVarInt(message.trueQiMaximum);
                    buffer.writeVarInt(message.alchemyLevel);
                    buffer.writeVarInt(message.alchemyExperience);
                    buffer.writeVarInt(message.alchemyExperienceToNextLevel);
                    buffer.writeUtf(message.immortalFoundation);
                    buffer.writeVarInt(message.majorBreakthroughFailures);
                    buffer.writeVarInt(message.spellLoadout.size());
                    message.spellLoadout.forEach(id -> buffer.writeUtf(id, 64));
                    buffer.writeVarInt(message.learnedSpellIds.size());
                    message.learnedSpellIds.forEach(id -> buffer.writeUtf(id, 64));
                },
                buffer -> new CultivationSyncPacket(buffer.readBoolean(), buffer.readUtf(32), buffer.readUtf(32),
                        buffer.readUtf(32), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readUtf(64), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(64), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(16),
                        buffer.readVarInt(), readStrings(buffer), readStrings(buffer)),
                XiuxianNetwork::handleCultivationSync,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void openIdentityScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityScreenPacket());
    }

    public static void closeIdentityScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentitySelectedPacket());
    }

    public static void selectIdentity(String familyId, String pathId) {
        CHANNEL.sendToServer(new SelectIdentityPacket(familyId, pathId));
    }

    public static void syncCultivation(ServerPlayer player, CultivationData data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CultivationSyncPacket(
                data.isInitialized(), data.familyOrigin().id(), data.cultivationPath().id(), data.realm().id(),
                data.realmLevel(), data.qi(), data.breakthroughCost(), data.techniqueId(), data.isMeditating(),
                data.spiritualRoot(), data.constitution(), data.comprehension(), data.fortune(),
                data.studyingTechniqueId(), data.techniqueStudyTicks(), data.techniqueStudyDuration(),
                data.techniqueStudyChance(), data.trueQi(), data.trueQiMaximum(), data.alchemyLevel(),
                data.alchemyExperience(), data.alchemyExperienceToNextLevel(),
                data.immortalFoundation(), data.majorBreakthroughFailures(),
                data.spellLoadout(), data.learnedSpellIds()));
    }

    public static void requestChannelInterrupt() {
        CHANNEL.sendToServer(new ChannelInterruptPacket());
    }

    public static void requestVoidWalk() {
        CHANNEL.sendToServer(new VoidWalkPacket());
    }

    public static void requestEquipSpell(int slot, String spellId) {
        CHANNEL.sendToServer(new EquipSpellPacket(slot, spellId == null ? "" : spellId));
    }

    public static void requestCastSpell(String spellId) {
        CHANNEL.sendToServer(new CastSpellPacket(spellId == null ? "" : spellId));
    }

    public static void syncTrueQi(ServerPlayer player, int trueQi) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TrueQiSyncPacket(trueQi));
    }

    private static void handleOpenIdentityScreen(IdentityScreenPacket message,
                                                  Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientScreens.openIdentityScreen()));
        context.setPacketHandled(true);
    }

    private static void handleIdentitySelected(IdentitySelectedPacket message,
                                                Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientScreens.closeIdentityScreen()));
        context.setPacketHandled(true);
    }

    private static void handleSelectIdentity(SelectIdentityPacket message,
                                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !CultivationEvents.selectIdentity(player, message.familyId, message.pathId)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("身份选择无效，请重新选择。"));
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleChannelInterrupt(ChannelInterruptPacket message,
                                                Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) CultivationEvents.interruptChannel(player);
        });
        context.setPacketHandled(true);
    }

    private static void handleVoidWalk(VoidWalkPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) CultivationEvents.performVoidWalk(player);
        });
        context.setPacketHandled(true);
    }

    private static void handleEquipSpell(EquipSpellPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            CultivationData data = xiuxian.cultivation.TaixuDimension.recoverTripData(player);
            if (data != null && data.equipSpell(message.slot, message.spellId)) {
                syncCultivation(player, data);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u672f\u6cd5\u88c5\u914d\u5df2\u66f4\u65b0\u3002"));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u65e0\u6cd5\u88c5\u914d\u8fd9\u95e8\u672f\u6cd5\u3002"));
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleCastSpell(CastSpellPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            xiuxian.cultivation.CultivationSpells.CastResult result =
                    xiuxian.cultivation.CultivationSpells.castEquipped(player, message.spellId);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(result.message()));
        });
        context.setPacketHandled(true);
    }

    private static void handleTrueQiSync(TrueQiSyncPacket message,
                                         Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> xiuxian.client.CultivationClientState.updateTrueQi(message.trueQi)));
        context.setPacketHandled(true);
    }

    private static void handleCultivationSync(CultivationSyncPacket message,
                                               Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientScreens.updateCultivation(message.initialized, message.familyId, message.pathId,
                        message.realmId, message.realmLevel, message.qi, message.breakthroughCost,
                        message.techniqueId, message.meditating, message.spiritualRoot, message.constitution,
                        message.comprehension, message.fortune, message.studyingTechniqueId,
                        message.techniqueStudyTicks, message.techniqueStudyDuration,
                        message.techniqueStudyChance, message.trueQi, message.trueQiMaximum,
                        message.alchemyLevel, message.alchemyExperience,
                        message.alchemyExperienceToNextLevel, message.immortalFoundation,
                        message.majorBreakthroughFailures, message.spellLoadout,
                        message.learnedSpellIds)));
        context.setPacketHandled(true);
    }

    private static final class IdentityScreenPacket {}

    private static final class IdentitySelectedPacket {}

    private static final class ChannelInterruptPacket {}

    private static final class VoidWalkPacket {}

    private static final class EquipSpellPacket {
        private final int slot;
        private final String spellId;

        private EquipSpellPacket(int slot, String spellId) {
            this.slot = slot;
            this.spellId = spellId;
        }
    }

    private static final class CastSpellPacket {
        private final String spellId;

        private CastSpellPacket(String spellId) {
            this.spellId = spellId;
        }
    }

    private static final class TrueQiSyncPacket {
        private final int trueQi;

        private TrueQiSyncPacket(int trueQi) {
            this.trueQi = Math.max(0, trueQi);
        }
    }

    private static final class SelectIdentityPacket {
        private final String familyId;
        private final String pathId;

        private SelectIdentityPacket(String familyId, String pathId) {
            this.familyId = familyId;
            this.pathId = pathId;
        }
    }

    private static final class CultivationSyncPacket {
        private final boolean initialized;
        private final String familyId;
        private final String pathId;
        private final String realmId;
        private final int realmLevel;
        private final int qi;
        private final int breakthroughCost;
        private final String techniqueId;
        private final boolean meditating;
        private final int spiritualRoot;
        private final int constitution;
        private final int comprehension;
        private final int fortune;
        private final String studyingTechniqueId;
        private final int techniqueStudyTicks;
        private final int techniqueStudyDuration;
        private final int techniqueStudyChance;
        private final int trueQi;
        private final int trueQiMaximum;
        private final int alchemyLevel;
        private final int alchemyExperience;
        private final int alchemyExperienceToNextLevel;
        private final String immortalFoundation;
        private final int majorBreakthroughFailures;
        private final List<String> spellLoadout;
        private final List<String> learnedSpellIds;

        private CultivationSyncPacket(boolean initialized, String familyId, String pathId, String realmId,
                                      int realmLevel, int qi, int breakthroughCost,
                                      String techniqueId, boolean meditating, int spiritualRoot, int constitution,
                                      int comprehension, int fortune, String studyingTechniqueId,
                                      int techniqueStudyTicks, int techniqueStudyDuration,
                                      int techniqueStudyChance, int trueQi, int trueQiMaximum,
                                      int alchemyLevel, int alchemyExperience,
                                      int alchemyExperienceToNextLevel,
                                      String immortalFoundation, int majorBreakthroughFailures,
                                      List<String> spellLoadout, java.util.Collection<String> learnedSpellIds) {
            this.initialized = initialized;
            this.familyId = familyId;
            this.pathId = pathId;
            this.realmId = realmId;
            this.realmLevel = realmLevel;
            this.qi = qi;
            this.breakthroughCost = breakthroughCost;
            this.techniqueId = techniqueId;
            this.meditating = meditating;
            this.spiritualRoot = spiritualRoot;
            this.constitution = constitution;
            this.comprehension = comprehension;
            this.fortune = fortune;
            this.studyingTechniqueId = studyingTechniqueId;
            this.techniqueStudyTicks = techniqueStudyTicks;
            this.techniqueStudyDuration = techniqueStudyDuration;
            this.techniqueStudyChance = techniqueStudyChance;
            this.trueQi = trueQi;
            this.trueQiMaximum = trueQiMaximum;
            this.alchemyLevel = alchemyLevel;
            this.alchemyExperience = alchemyExperience;
            this.alchemyExperienceToNextLevel = alchemyExperienceToNextLevel;
            this.immortalFoundation = immortalFoundation;
            this.majorBreakthroughFailures = majorBreakthroughFailures;
            this.spellLoadout = List.copyOf(spellLoadout);
            this.learnedSpellIds = List.copyOf(learnedSpellIds);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int size = Math.min(16, Math.max(0, buffer.readVarInt()));
        java.util.ArrayList<String> values = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add(buffer.readUtf(64));
        return values;
    }
}
