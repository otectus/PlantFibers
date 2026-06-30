package com.otectus.plantfiber.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PlantFiberConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        SPEC = builder.build();
    }

    private PlantFiberConfig() {
    }

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue enablePlantFiberDrops;
        public final ForgeConfigSpec.DoubleValue plantFiberDropChance;

        public final ForgeConfigSpec.BooleanValue enableBandage;
        public final ForgeConfigSpec.IntValue bandageRegenDurationTicks;
        public final ForgeConfigSpec.IntValue bandageRegenAmplifier;
        public final ForgeConfigSpec.IntValue bandageCooldownTicks;

        public final ForgeConfigSpec.BooleanValue enablePlaster;
        public final ForgeConfigSpec.IntValue plasterRegenDurationTicks;
        public final ForgeConfigSpec.IntValue plasterRegenAmplifier;
        public final ForgeConfigSpec.IntValue plasterCooldownTicks;

        public final ForgeConfigSpec.BooleanValue enableGrassBed;
        public final ForgeConfigSpec.BooleanValue enableGrassArmor;
        public final ForgeConfigSpec.DoubleValue grassArmorDurabilityMultiplier;

        public final ForgeConfigSpec.BooleanValue enablePlantMeshPaperRecipe;
        public final ForgeConfigSpec.BooleanValue enableReinforcedFibers;

        public final ForgeConfigSpec.BooleanValue enableRope;
        public final ForgeConfigSpec.BooleanValue enableFenceRopeAnchors;
        public final ForgeConfigSpec.BooleanValue ropeBreaksWhenUnsupported;
        public final ForgeConfigSpec.BooleanValue unsupportedRopeDropsItems;
        public final ForgeConfigSpec.BooleanValue ropeCanBeWaterlogged;
        public final ForgeConfigSpec.BooleanValue ropeCanReplacePlants;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("drops");
            enablePlantFiberDrops = builder
                    .comment("Enable Plant Fiber drops from vegetation blocks")
                    .define("enablePlantFiberDrops", true);
            plantFiberDropChance = builder
                    .comment("Chance per vegetation block break to drop Plant Fiber")
                    .defineInRange("plantFiberDropChance", 0.25d, 0.0d, 1.0d);
            builder.pop();

            builder.push("bandage");
            enableBandage = builder
                    .comment("Enable Grass Bandage usage")
                    .define("enableBandage", true);
            bandageRegenDurationTicks = builder
                    .comment("Bandage regeneration duration in ticks")
                    .defineInRange("bandageRegenDurationTicks", 120, 0, 20 * 60);
            bandageRegenAmplifier = builder
                    .comment("Bandage regeneration amplifier (0 = Regen I)")
                    .defineInRange("bandageRegenAmplifier", 0, 0, 4);
            bandageCooldownTicks = builder
                    .comment("Bandage cooldown in ticks")
                    .defineInRange("bandageCooldownTicks", 120, 0, 20 * 60);
            builder.pop();

            builder.push("plaster");
            enablePlaster = builder
                    .comment("Enable Grass Plaster usage")
                    .define("enablePlaster", true);
            plasterRegenDurationTicks = builder
                    .comment("Plaster regeneration duration in ticks")
                    .defineInRange("plasterRegenDurationTicks", 160, 0, 20 * 60);
            plasterRegenAmplifier = builder
                    .comment("Plaster regeneration amplifier (1 = Regen II)")
                    .defineInRange("plasterRegenAmplifier", 1, 0, 4);
            plasterCooldownTicks = builder
                    .comment("Plaster cooldown in ticks")
                    .defineInRange("plasterCooldownTicks", 200, 0, 20 * 60);
            builder.pop();

            builder.push("grass");
            enableGrassBed = builder
                    .comment("Enable Grass Bed placement")
                    .define("enableGrassBed", true);
            enableGrassArmor = builder
                    .comment("Enable Grass Armor usage")
                    .define("enableGrassArmor", true);
            grassArmorDurabilityMultiplier = builder
                    .comment("Durability multiplier relative to leather (0.5 = half leather)")
                    .defineInRange("grassArmorDurabilityMultiplier", 0.5d, 0.1d, 1.0d);
            builder.pop();

            builder.push("crafting");
            enablePlantMeshPaperRecipe = builder
                    .comment("Enable cooking Plant Mesh into Paper (furnace and campfire)")
                    .define("enablePlantMeshPaperRecipe", true);
            enableReinforcedFibers = builder
                    .comment("Enable crafting Reinforced Fibers from Plant Fiber and String")
                    .define("enableReinforcedFibers", true);
            builder.pop();

            builder.push("rope");
            enableRope = builder
                    .comment("Enable Rope crafting and placement")
                    .define("enableRope", true);
            enableFenceRopeAnchors = builder
                    .comment("Allow Rope to be tied to fences (side-click a fence to start an outward hanging rope)")
                    .define("enableFenceRopeAnchors", true);
            ropeBreaksWhenUnsupported = builder
                    .comment("Rope breaks when it loses its anchor or connected rope above")
                    .define("ropeBreaksWhenUnsupported", true);
            unsupportedRopeDropsItems = builder
                    .comment("Rope that breaks due to losing support drops itself")
                    .define("unsupportedRopeDropsItems", true);
            ropeCanBeWaterlogged = builder
                    .comment("Allow Rope to be waterlogged when placed in water")
                    .define("ropeCanBeWaterlogged", false);
            ropeCanReplacePlants = builder
                    .comment("Allow Rope extension to replace plant blocks (tagged vegetation)")
                    .define("ropeCanReplacePlants", true);
            builder.pop();
        }
    }
}


