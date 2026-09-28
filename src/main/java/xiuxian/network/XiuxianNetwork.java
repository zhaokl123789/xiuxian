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

public final class XiuxianNetwork {
    private static final String PROTOCOL_VERSION = "1";
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

    private static final class IdentityScreenPacket {}

    private static final class IdentitySelectedPacket {}

    private static final class SelectIdentityPacket {
        private final String familyId;
        private final String pathId;

        private SelectIdentityPacket(String familyId, String pathId) {
            this.familyId = familyId;
            this.pathId = pathId;
        }
    }
}
