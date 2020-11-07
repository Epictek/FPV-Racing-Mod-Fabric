package dev.lazurite.fpvracing.network.packet;

import dev.lazurite.fpvracing.network.tracker.Config;
import dev.lazurite.fpvracing.util.PacketHelper;
import dev.lazurite.fpvracing.server.ServerInitializer;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class ConfigC2S {
    public static final Identifier PACKET_ID = new Identifier(ServerInitializer.MODID, "config_c2s");

    public static void accept(PacketContext context, PacketByteBuf buf) {
        UUID uuid = context.getPlayer().getUuid();
        Config config = PacketHelper.deserializeConfig(buf);
        context.getTaskQueue().execute(() -> ServerInitializer.SERVER_PLAYER_CONFIGS.put(uuid, config));
    }

    public static void send(Config config) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        PacketHelper.serializeConfig(buf, config);
        ClientSidePacketRegistry.INSTANCE.sendToServer(PACKET_ID, buf);
    }

    public static void register() {
        ServerSidePacketRegistry.INSTANCE.register(PACKET_ID, ConfigC2S::accept);
    }
}
