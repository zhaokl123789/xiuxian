package xiuxian.network;

import java.util.Optional;
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
    private static final String PROTOCOL_VERSION = "6";
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
        CHANNEL.registerMessage(nextMessageId++, JumpEnhancementPacket.class,
                (message, buffer) -> {}, buffer -> new JumpEnhancementPacket(),
                XiuxianNetwork::handleJumpEnhancement,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
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
                },
                buffer -> new CultivationSyncPacket(buffer.readBoolean(), buffer.readUtf(32), buffer.readUtf(32),
                        buffer.readUtf(32), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readUtf(64), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(64), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(16),
                        buffer.readVarInt()),
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
                data.immortalFoundation(), data.majorBreakthroughFailures()));
    }

    public static void requestChannelInterrupt() {
        CHANNEL.sendToServer(new ChannelInterruptPacket());
    }

    public static void requestVoidWalk() {
        CHANNEL.sendToServer(new VoidWalkPacket());
    }

    public static void requestJumpEnhancement() {
        CHANNEL.sendToServer(new JumpEnhancementPacket());
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

    private static void handleJumpEnhancement(JumpEnhancementPacket message,
                                               Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) CultivationEvents.performJumpEnhancement(player);
        });
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
                        message.majorBreakthroughFailures)));
        context.setPacketHandled(true);
    }

    private static final class IdentityScreenPacket {}

    private static final class IdentitySelectedPacket {}

    private static final class ChannelInterruptPacket {}

    private static final class VoidWalkPacket {}

    private static final class JumpEnhancementPacket {}

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

        private CultivationSyncPacket(boolean initialized, String familyId, String pathId, String realmId,
                                      int realmLevel, int qi, int breakthroughCost,
                                      String techniqueId, boolean meditating, int spiritualRoot, int constitution,
                                      int comprehension, int fortune, String studyingTechniqueId,
                                      int techniqueStudyTicks, int techniqueStudyDuration,
                                      int techniqueStudyChance, int trueQi, int trueQiMaximum,
                                      int alchemyLevel, int alchemyExperience,
                                      int alchemyExperienceToNextLevel,
                                      String immortalFoundation, int majorBreakthroughFailures) {
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
        }
    }
}
