package com.otectus.plantfiber.dev;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import com.otectus.plantfiber.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Development-only verification fixture for rope placement. It is excluded from the production JAR by
 * the {@code jar} task and additionally refuses to do anything in a production environment; no other
 * class references it.
 *
 * <p>Driven by system properties on the development run:
 * <ul>
 *   <li>{@code -Dplantfiber.dev.cancelPlaceAt=x,y,z} — cancel the placement event at exactly that
 *       block, so a single target in a column can be denied while its neighbours succeed.</li>
 *   <li>{@code -Dplantfiber.dev.cancelKnots=true} — cancel rope-knot entity insertion, exercising
 *       mutation failure separately from block-placement cancellation.</li>
 * </ul>
 * Every placement event is counted and logged with the replaced block and the player's rope count so
 * before/after world state and inventory can be compared.
 */
@Mod.EventBusSubscriber(modid = PlantFiberMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RopePlacementFixture {
    private static final String CANCEL_PLACE_AT = "plantfiber.dev.cancelPlaceAt";
    private static final String CANCEL_KNOTS = "plantfiber.dev.cancelKnots";

    private static int placementEvents;

    private RopePlacementFixture() {
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
        placementEvents++;
        BlockPos pos = event.getPos();
        PlantFiberMod.LOGGER.info("[dev fixture] placement #{} at {}: {} -> {} (rope in inventory: {})",
                placementEvents, pos, event.getBlockSnapshot().getReplacedBlock(), event.getPlacedBlock(),
                ropeCount(event.getEntity() instanceof Player player ? player : null));
        if (pos.equals(cancelTarget())) {
            PlantFiberMod.LOGGER.info("[dev fixture] cancelling placement at {}", pos);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (FMLEnvironment.production || !(event.getEntity() instanceof RopeKnotEntity)) {
            return;
        }
        if (Boolean.getBoolean(CANCEL_KNOTS)) {
            PlantFiberMod.LOGGER.info("[dev fixture] cancelling rope knot insertion at {}",
                    event.getEntity().blockPosition());
            event.setCanceled(true);
        }
    }

    private static int ropeCount(Player player) {
        return player == null ? -1 : player.getInventory().countItem(ModItems.ROPE_ITEM.get());
    }

    private static BlockPos cancelTarget() {
        String value = System.getProperty(CANCEL_PLACE_AT);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
