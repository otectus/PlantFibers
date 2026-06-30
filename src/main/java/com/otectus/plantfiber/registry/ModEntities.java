package com.otectus.plantfiber.registry;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PlantFiberMod.MODID);

    /**
     * The rope knot tied around a fence post. Modelled on the vanilla leash knot
     * ({@code LeashFenceKnotEntity} / {@code HangingEntity}): a small decorative attachment that
     * lives at the fence's block position, never replacing the fence block itself.
     */
    public static final RegistryObject<EntityType<RopeKnotEntity>> ROPE_KNOT =
            ENTITY_TYPES.register("rope_knot",
                    () -> EntityType.Builder.<RopeKnotEntity>of(RopeKnotEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(10)
                            .build("rope_knot"));

    private ModEntities() {
    }
}
