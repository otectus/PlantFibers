package com.otectus.plantfiber.network;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.EffectivePlantFiberConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Play-phase channel carrying only {@link ArmorConfigSnapshot}, registered from the mod constructor. */
public final class ArmorConfigNetwork {
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(PlantFiberMod.MODID, "armor_config");
    private static final int SNAPSHOT_MESSAGE_ID = 0;

    private static SimpleChannel channel;

    private ArmorConfigNetwork() {
    }

    public static void register() {
        // Protocol compatibility follows the expanded mod metadata rather than a second version literal.
        String protocolVersion = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
        channel = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> protocolVersion,
                protocolVersion::equals, protocolVersion::equals);
        channel.messageBuilder(ArmorConfigSnapshot.class, SNAPSHOT_MESSAGE_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ArmorConfigSnapshot::encode)
                .decoder(ArmorConfigSnapshot::decode)
                .consumerMainThread(ArmorConfigSnapshot::handle)
                .add();
    }

    /** Send this server's own armor multiplier; the local value is authoritative here. */
    public static void sendTo(ServerPlayer player) {
        if (channel == null) {
            return;
        }
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                new ArmorConfigSnapshot(EffectivePlantFiberConfig.localArmorDurabilityMultiplier()));
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }
}
