package com.otectus.plantfiber.event;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.EffectivePlantFiberConfig;
import com.otectus.plantfiber.network.ArmorConfigNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Keeps {@link EffectivePlantFiberConfig} in step with this installation's config file. */
@Mod.EventBusSubscriber(modid = PlantFiberMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigSnapshotEvents {
    private ConfigSnapshotEvents() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (isOwnCommonConfig(event.getConfig())) {
            EffectivePlantFiberConfig.refreshLocalFromSpec();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (!isOwnCommonConfig(event.getConfig())) {
            return;
        }
        EffectivePlantFiberConfig.refreshLocalFromSpec();
        // A reload on a running server changes the maximum it enforces, so re-send the snapshot.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> ArmorConfigNetwork.sendToAll(server));
        }
    }

    private static boolean isOwnCommonConfig(ModConfig config) {
        return PlantFiberMod.MODID.equals(config.getModId()) && config.getType() == ModConfig.Type.COMMON;
    }
}
