package com.otectus.plantfiber.network;

import com.otectus.plantfiber.config.EffectivePlantFiberConfig;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * The one gameplay value this mod puts on the wire: the server's grass-armor durability multiplier.
 * It exists so a joined client shows the same maximum durability the server enforces; nothing else
 * from the COMMON config is synchronized.
 */
public record ArmorConfigSnapshot(double armorDurabilityMultiplier) {
    public static void encode(ArmorConfigSnapshot message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.armorDurabilityMultiplier());
    }

    public static ArmorConfigSnapshot decode(FriendlyByteBuf buffer) {
        // Bound whatever arrives to the config's validated range before it can reach item code.
        return new ArmorConfigSnapshot(EffectivePlantFiberConfig.clamp(buffer.readDouble()));
    }

    public static void handle(ArmorConfigSnapshot message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                EffectivePlantFiberConfig.setRemoteArmorDurabilityMultiplier(message.armorDurabilityMultiplier()));
        ctx.setPacketHandled(true);
    }
}
