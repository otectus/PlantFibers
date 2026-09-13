# Plant Fibers — Full Review and Coding-Agent Roadmap

## 1. Reviewed target, scope, and validation

**Target confirmed: Minecraft 1.20.1, Forge 47.x (build pins 47.2.0), Java 17, ForgeGradle 6.0.16. This is not a NeoForge or 1.21 port.** The mod's registry namespace is the singular `plantfiber`, despite the repository name `PlantFibers`.

| Field | Reviewed value |
|---|---|
| Mod version | 1.2.0 |
| Repository | https://github.com/otectus/PlantFibers |
| Commit | `e9a275cbb633e4f02156d589d6f6399372dd7c73` on `master` |
| Commit subject | Release 1.2.0: Rope, material progression, and CurseForge docs |
| Commit timestamp | 2026-06-30 18:48:35 -0400 |
| Review date | 2026-09-10 UTC |
| Accessible checkout | `/workspace/scratch/5377f6e05505/PlantFibers` |
| Author's Windows checkout | `C:\Projects\PlantFibers` was not accessible; this report reviews the public commit above, not unpublished local changes |
| Version evidence | `build.gradle:8–23,61–63`; `gradle.properties:1–2`; `src/main/resources/META-INF/mods.toml:1–28` |
| Build tooling | Gradle wrapper 8.4; Java 17.0.20 available in review environment |

This is a review and planning deliverable. No source, asset, recipe, configuration, or build file was changed. No issues, PRs, or messages were published. The only new project file is this document.

### Validation actually performed

| Action | Result and limits |
|---|---|
| Read `MODMAP.md` first if present | Absent. No hand-maintained map was available. No `AGENTS.md` was present in the checkout. |
| Minecraft modding skill; `modmap.py`, `check_mod.py`, `find_api.py` | Not available in the exposed skill catalog or searched local tool locations. None was run; no substitute is represented as their output. |
| Full tracked-file audit | Read all 24 Java classes, all 50 JSON files, metadata, Gradle files, wrapper configuration/scripts, README, CHANGELOG, and CURSEFORGE text. Inspected all 17 PNGs. Checked the wrapper JAR as a binary artifact, not by decompiling its implementation. Total: 104 tracked files before this report. |
| JSON and asset checks | All JSON parsed. Local model/texture references resolved. Dimensions: item/block textures 16×16, armor layers 64×32, knot texture 32×32. No duplicate hand-written/generated resource paths exist at this commit. |
| `./gradlew build` | **BLOCKED before Gradle tasks:** wrapper download of `gradle-8.4-bin.zip` failed with `java.net.UnknownHostException: services.gradle.org`. No compile result, test result, compiler warnings, or usable JAR were produced. |
| `./gradlew runData` | Same wrapper DNS failure. `src/generated/resources` is absent; no providers are present in Java. No generated diff appeared, but this does **not** establish that generation is current. |
| `./gradlew runServer` | Same wrapper DNS failure, before FML, EULA processing, or server startup. No `Done` message, world, or dedicated-server side-safety result. No EULA file was changed. |
| Git status | Initially clean. At completion, only this report should be untracked/changed. |
| API research | Used GitHub plugin reads, official Forge 1.20.1 docs/source, version-filtered Modrinth release metadata, CurseForge metadata, Maven POMs, and selected release-JAR resource/metadata inspection. |
| Pinned Forge source limitation | Exact 47.2.0 source artifacts were not obtainable. Forge's **1.20.1** branch was inspected; findings depending on startup ordering remain `LIKELY` until checked against the pinned runtime. The unrelated `1.20.x` branch must not be assumed to mean 1.20.1. |

The build failures are environmental evidence, not evidence of a broken project build. There were no build warnings to enumerate because the wrapper never reached Gradle. Release availability and readable POMs establish candidate dependencies; they do not establish that a combined modpack launches.

## 2. Executive summary

The mod has a coherent small survival identity, consistent registration, complete local assets, and a sensible split between common and client code. Its highest-risk code is concentrated in the custom two-block bed, config-dependent armor initialization, and the second rope-placement entry point through a knot entity. Fix these before adding processing machines or integrations.

There are **21 findings: 15 defects and 6 inconsistencies/maintenance issues**. Confidence describes the stated evidence, not a claim that an unrun game test passed.

| Severity | Count | Meaning in this review |
|---|---:|---|
| Critical | 1 | Likely development startup crash from reading an unloaded config during registration |
| High | 3 | Bed item loss, unsafe respawn selection, and likely placement-protection bypass through knots |
| Medium | 12 | Incorrect state/config behavior, compatibility hygiene, missing recipe unlocks, and progression balance |
| Low | 5 | Toggle/documentation semantics, metadata maintenance, and semantic tags |

The five most important fixes are PF-BUG-001 (armor config lifecycle), PF-BUG-002 (bed foot break loses the bed), PF-BUG-003 (safe respawn), PF-BUG-004 (knot placement authorization), and PF-BUG-006 (server-authoritative effective config).

The three strongest expansion directions are:

1. **A useful fiber-processing chain:** consumptive plant processing, bucketless wetting, slow drying, and woven cloth with defined sinks.
2. **Practical rope improvements:** a spool and controlled retrieval, built on corrected placement, support, permissions, and item accounting.
3. **Survival integrations using existing materials:** Farmer's Delight and No Tree Punching first, followed by optional temperature profiles and Create 6 automation. Preserve overhaul progression and each rope block's distinct behavior.

Do not rename existing IDs, force a loader migration, make raw fiber equivalent to string/wool/hemp, or install hard dependencies to achieve these goals.

## 3. Project snapshot

### 3.1 Path conventions used throughout this document

These aliases are exact repository-relative path prefixes, not additional directories:

- **J/** = `src/main/java/com/otectus/plantfiber/`
- **R/** = `src/main/resources/`
- **D/** = `src/main/resources/data/plantfiber/`
- **A/** = `src/main/resources/assets/plantfiber/`

For example, `J/block/RopeBlock.java:187–197` means `src/main/java/com/otectus/plantfiber/block/RopeBlock.java`, lines 187–197 at the reviewed commit. JSON filenames identify the entire specific resource. All line citations in this document refer to the reviewed commit; implementation will move them.

### 3.2 Complete registration inventory

All IDs below are prefixed `plantfiber:` unless explicitly qualified. `PlantFiberMod.java:23–32` registers the six DeferredRegisters on the MOD bus and the COMMON config.

| Registry | IDs | Evidence |
|---|---|---|
| Items, 12 | `plant_fiber`, `plant_mesh`, `reinforced_fibers`, `grass_bandage`, `grass_plaster`, `grass_helmet`, `grass_chestplate`, `grass_leggings`, `grass_boots`, `grass_fiber_block`, `grass_bed`, `rope` | `J/registry/ModItems.java:21–68` |
| Blocks, 3 | `grass_fiber_block`, `grass_bed`, `rope` | `J/registry/ModBlocks.java:22–41` |
| Block entity type, 1 | `grass_bed` | `J/registry/ModBlockEntities.java:14–16` |
| Entity type, 1 | `rope_knot` (MISC; 0.5×0.5; tracking range 10) | `J/registry/ModEntities.java:20–25` |
| Creative tab, 1 | `plantfiber` | `J/registry/ModCreativeTabs.java:13–42` |
| Global loot modifier serializer, 1 | `plant_fiber_from_vegetation` | `J/registry/ModLootModifiers.java:13–20` |
| Crafting condition serializer, 1 | `plantfiber:config` (CraftingHelper condition, **not** a recipe type) | `J/crafting/ConfigCondition.java:22–29,54–70`; `J/event/ModSetup.java:10–17` |
| Armor material, non-registry constant | `GRASS`, texture name `plantfiber:grass` | `J/registry/ModArmorMaterials.java:12–67` |
| Absent registries/systems | No custom recipe types, recipe serializers, menus, screens, effects, sounds, fluids, particles, enchantments, worldgen, capabilities, or mixins | Full Java/resource inventory |

The three blocks all have corresponding items. The grass-bed block entity contains only its constructor; it has no inventory, progress, or custom NBT. Retain its existing ID even if a later implementation no longer needs it.

### 3.3 Java and event inventory

All 24 classes are accounted for:

| Package | Classes |
|---|---|
| root | `PlantFiberMod` |
| block | `GrassBedBlock`, `GrassBedBlockEntity`, `RopeBlock` |
| client | `ClientSetup`, `RopeKnotModel`, `RopeKnotRenderer` |
| config | `PlantFiberConfig` |
| crafting | `ConfigCondition` |
| entity | `RopeKnotEntity` |
| event | `ModSetup` |
| item | `ConfigurableBedItem`, `GrassArmorItem`, `HealingConsumableItem`, `RopeBlockItem` |
| loot | `PlantFiberLootModifier` |
| registry | `ModArmorMaterials`, `ModBlockEntities`, `ModBlocks`, `ModCreativeTabs`, `ModEntities`, `ModItems`, `ModLootModifiers`, `ModTags` |

| Handler/lifecycle hook | Bus / side / behavior |
|---|---|
| `ModSetup.onCommonSetup` | MOD bus; enqueues CraftingHelper condition registration (`J/event/ModSetup.java:10–17`) |
| `ClientSetup` client setup handler | MOD bus, `Dist.CLIENT`; enqueues cutout layers for bed and rope (`J/client/ClientSetup.java:14–23`) |
| `ClientSetup` renderer registration | MOD bus, `Dist.CLIENT`; knot entity renderer (`J/client/ClientSetup.java:25–29`) |
| `ClientSetup` layer-definition registration | MOD bus, `Dist.CLIENT`; knot model layer (`J/client/ClientSetup.java:31–34`) |
| Registry listeners | The six DeferredRegisters above, explicitly attached to the mod event bus |
| Gameplay events | No global player/block/tick event subscriber. Gameplay is implemented in block/item/entity overrides and a global loot modifier. |
| Scheduled work | Rope support uses scheduled block ticks; knots perform one support block lookup each server tick. No block-entity tickers. |
| Config lifecycle / networking | No config load/reload handler, no effective-config sync, no custom channel |

### 3.4 Complete config inventory

All 21 values are in `J/config/PlantFiberConfig.java:47–126`, registered as **COMMON** in `J/PlantFiberMod.java:32`. Every value has at least one reader; none is a wholly unused declaration. COMMON is not automatically synchronized to clients [S02].

| Full key | Default | Allowed values | Consumers |
|---|---|---|---|
| `drops.enablePlantFiberDrops` | true | boolean | vegetation GLM |
| `drops.plantFiberDropChance` | 0.25 | 0–1 | vegetation GLM |
| `bandage.enableBandage` | true | boolean | bandage use supplier |
| `bandage.bandageRegenDurationTicks` | 120 | 0–1200 | effect and tooltip |
| `bandage.bandageRegenAmplifier` | 0 | 0–4 | effect and tooltip |
| `bandage.bandageCooldownTicks` | 120 | 0–1200 | use cooldown and tooltip |
| `plaster.enablePlaster` | true | boolean | plaster use supplier |
| `plaster.plasterRegenDurationTicks` | 160 | 0–1200 | effect and tooltip |
| `plaster.plasterRegenAmplifier` | 1 | 0–4 | effect and tooltip |
| `plaster.plasterCooldownTicks` | 200 | 0–1200 | use cooldown and tooltip |
| `grass.enableGrassBed` | true | boolean | item placement, bed use, creative tab |
| `grass.enableGrassArmor` | true | boolean | armor use/equip checks, creative tab |
| `grass.grassArmorDurabilityMultiplier` | 0.5 | 0.1–1.0 | material durability during item construction |
| `crafting.enablePlantMeshPaperRecipe` | true | boolean | two cooking recipe conditions |
| `crafting.enableReinforcedFibers` | true | boolean | recipe condition and creative tab |
| `rope.enableRope` | true | boolean | recipe, placement, knot interaction, creative tab |
| `rope.enableFenceRopeAnchors` | true | boolean | fence placement and knot interaction |
| `rope.ropeBreaksWhenUnsupported` | true | boolean | rope block survival |
| `rope.unsupportedRopeDropsItems` | true | boolean | support-loss block/entity drops |
| `rope.ropeCanBeWaterlogged` | false | boolean | placement and liquid insertion |
| `rope.ropeCanReplacePlants` | true | boolean | extension replaceability, incompletely enforced |

### 3.5 Every recipe and the material economy

All 15 recipes are hand-written in `D/recipes/`. The following names are exact filenames without `.json`. There are 13 crafting recipes and two cooking recipes; no recipe-unlock advancements exist.

| Recipe | Input → output | Gate / observation |
|---|---|---|
| `grass_fiber_block` | 9 raw fiber → 1 fiber block | Unconditional |
| `plant_fiber_from_grass_fiber_block` | 1 fiber block → 9 raw fiber | Unconditional; exact reciprocal, no intrinsic dupe |
| `string_from_plant_fiber` | 3 raw fiber → 1 vanilla string | Unconditional |
| `plant_mesh` | 3 raw fiber + water bucket → 1 mesh | Empty bucket is vanilla crafting remainder; iron bucket is a progression bottleneck |
| `paper_from_plant_mesh` | 1 mesh → 1 paper, smelting, 200 ticks, JSON XP 0.1 | `enablePlantMeshPaperRecipe` |
| `paper_from_plant_mesh_campfire` | 1 mesh → 1 paper, campfire, 200 ticks, JSON XP 0.1 | Same gate; do not infer furnace-like XP payout from campfire JSON |
| `reinforced_fibers` | 1 raw fiber + 1 vanilla string → 1 reinforced fiber | `enableReinforcedFibers` |
| `rope` | 3 reinforced fibers vertically → 3 ropes | `enableRope` only |
| `grass_bandage` | 2 `#plantfiber:bandage_plants` + 1 raw fiber → 1 bandage | No recipe condition |
| `grass_plaster` | 3 bandage plants + 2 raw fiber → 1 plaster | No recipe condition |
| `grass_bed` | 3 fiber blocks + 3 `#minecraft:planks` → 1 bed | 27 raw fiber equivalent; no recipe condition |
| `grass_boots` | 4 fiber blocks → boots | 36 raw fiber |
| `grass_helmet` | 5 fiber blocks → helmet | 45 raw fiber |
| `grass_chestplate` | 8 fiber blocks → chestplate | 72 raw fiber |
| `grass_leggings` | 7 fiber blocks → leggings | 63 raw fiber |

At the default 25% chance, one raw fiber takes four eligible loot evaluations on average. String therefore averages 12; a bed 108; a full armor set **864**, excluding other drops and deterministic processing. These are expectations, not guaranteed break counts. Reinforced fiber costs four raw-fiber equivalents when its string comes from this mod; each rope costs four. A fence knot consumes one additional rope before the first rope block is placed.

Default armor defense is leather-like (boots/legs/chest/helmet: 1/2/3/1); nominal durability is 32/37/40/27 at multiplier 0.5. Bandages apply Regen I for 120 ticks with 120-tick item cooldown; plaster applies Regen II for 160 ticks with 200-tick item cooldown. Both have 24-tick use duration and BOW animation. Actual healing depends on vanilla effect timing; do not advertise a fixed heart restoration without testing.

### 3.6 Every loot table, modifier, and tag

Loot tables: `D/loot_tables/blocks/grass_fiber_block.json`, `grass_bed.json`, `rope.json`. They use explosion survival and self drops; bed drops require `part=head`.

Global modifier files: `R/data/forge/loot_modifiers/global_loot_modifiers.json` appends `plantfiber:plant_fiber_from_vegetation` with `replace:false`; `D/loot_modifiers/plant_fiber_from_vegetation.json` names the registered codec and has an empty conditions array. The Java modifier supplies the block-state/tag/config/random checks (`J/loot/PlantFiberLootModifier.java:18–47`). Empty JSON conditions are therefore not a broken registration. It appends one fiber at the configured chance; it does not inspect tool, maturity, explosion, silk touch, fortune, or double-plant half.

| Exact tag resource | Contents / role |
|---|---|
| `R/data/minecraft/tags/blocks/climbable.json` | `plantfiber:rope` |
| `R/data/minecraft/tags/blocks/mineable/hoe.json` | `plantfiber:grass_fiber_block` |
| `D/tags/blocks/rope_anchor_blocks.json` | Empty extension tag; sturdy downward faces also work |
| `D/tags/blocks/rope_fence_anchors.json` | `#minecraft:fences` |
| `D/tags/blocks/rope_replaceable_blocks.json` | `minecraft:cave_air` |
| `D/tags/blocks/vegetation.json` | 43 vanilla block entries, listed below |
| `D/tags/items/bandage_plants.json` | grass, tall_grass, fern, large_fern, vine, kelp (all `minecraft:`) |
| `D/tags/items/fibers.json` | `plantfiber:plant_fiber`; currently unused by recipes/repair |
| `D/tags/items/reinforced_fibers.json` | `plantfiber:reinforced_fibers`; currently unused by rope recipe |

Complete vegetation membership, all in `minecraft:`: `grass`, `tall_grass`, `fern`, `large_fern`, `dandelion`, `poppy`, `blue_orchid`, `allium`, `azure_bluet`, `red_tulip`, `orange_tulip`, `white_tulip`, `pink_tulip`, `oxeye_daisy`, `cornflower`, `lily_of_the_valley`, `wither_rose`, `sunflower`, `lilac`, `rose_bush`, `peony`, `vine`, `glow_lichen`, `sweet_berry_bush`, `cactus`, `sugar_cane`, `seagrass`, `tall_seagrass`, `kelp`, `kelp_plant`, `cave_vines`, `cave_vines_plant`, `dead_bush`, `lily_pad`, `spore_blossom`, `hanging_roots`, `moss_carpet`, `small_dripleaf`, `big_dripleaf`, `big_dripleaf_stem`, `pitcher_plant`, `torchflower`, `pink_petals`.

### 3.7 Asset/data completeness per ID

Every item has `A/models/item/<id>.json` and an English name. Generated item models resolve to matching item textures; block-item models resolve to the appropriate block/bed model. All three blocks have blockstates, models, item models, English names, recipes, and loot tables. Five block models are present: fiber block, bed head, bed foot, rope, and fence-anchored rope. Bed variants cover both parts, four facings, and occupancy; rope selectors legitimately omit properties that do not change a model. Missing redundant variants are not defects.

| IDs | Visual resources and intended omissions |
|---|---|
| `plant_fiber`, `plant_mesh`, `reinforced_fibers` | Item textures/models; source and sinks above. No blockstate, block loot, or mining tags needed. |
| `grass_bandage`, `grass_plaster` | Item textures/models plus two translated tooltip templates; recipes present. |
| Four grass armor items | Item textures/models and both armor layers; recipes and repair input present. |
| `grass_fiber_block` | Top/side block textures; model, blockstate, block item, hoe tag, self loot, reciprocal recipes. |
| `grass_bed` | Head/foot block models with grass textures; no dedicated renderer required by its MODEL render shape. Self loot has the break-half issue below. No correct-tool requirement; absent tool-level tags are intentional. |
| `rope` | Normal and fence-side models, block/item texture, climbable tag, self loot. No correct-tool requirement. |
| `rope_knot` | Java model/renderer, 32×32 entity texture, entity translation. No spawn egg or entity loot JSON; drop is implemented in Java. |
| Registry-only IDs | Creative tab, condition, and GLM serializer do not need item assets. Bed BE needs no separate texture. |

Only `en_us.json` exists; there are no stale alternate languages to reconcile. No broken local texture links or clearly orphaned resource files were found. Texture palette/style is an artistic review question, not a confirmed defect; inspect the bright green armor in daylight and shaded scenes before commissioning replacements.

### 3.8 Review-category coverage and clean checks

| Category | Checked clean portions | Findings / unresolved verification |
|---|---|---|
| Build & metadata | MC/Forge/Java/FG match expected era; plural folders; `pack_format:15`; namespace consistent; ARR declaration consistent; no mixin config needed | Build blocked; PF-INC-004; integration Forge floors differ |
| Registration | DeferredRegister wiring, block items, entity/BE factories, registry naming, no dangling internal IDs | Config timing PF-BUG-001; creative filtering PF-INC-002 |
| Assets/data | All local assets present, JSON parse, recipe result IDs, blockstate coverage, no generated shadows | Bed loot PF-BUG-002; unlocks PF-BUG-015; semantic tags PF-INC-005 |
| Events/lifecycle | Correct MOD/client buses; setup work enqueued; no unsafe mutation of composters, pots, or strippables | Config initialization PF-BUG-001 |
| Side safety | Renderer/model/client imports confined to `client/` with `Dist.CLIENT` subscriber | Dedicated-server and two-client tests not run |
| Gameplay | GLM registration is valid; fiber-block compression is exact; recipes do not intrinsically collide with vanilla | Bed/rope findings; renewable flower loop; armor cost |
| Config | All values read; numeric bounds and amplifier comments coherent | PF-BUG-001/006/008/009/012; toggle inconsistencies |
| Persistence/sync | Knot direction written/read; destroy-vs-unload distinction present; empty BE has no missing dirty mark | PF-BUG-005/006/013; late-tracking spawn test |
| Performance | No heavy global tick subscriber or random ticking; rope collapse scheduled per block; knot support check constant work | Long-column scan linear in height on interaction; profile dense knot builds before optimizing |
| Compatibility hygiene | Uses planks/fences tags; optional anchor extension tags exist; no hard dependencies | PF-BUG-004/007; selected shared tags below |
| Inconsistencies | Registry spelling consistently `fiber`; 1.20.1 folder names correct; no alternate lang drift | Six PF-INC tasks; one unused `OFFSET_Y` constant can be removed while editing knot code |

Do not create spurious fixes: a non-`BedBlock` class can be a valid Forge bed through `isBed`, `setBedOccupied`, and `getRespawnPosition` hooks [S05]; a vanilla-style knot spawn packet is not proven broken merely because it is not `NetworkHooks`; an empty GLM condition array can be intentional; trailing blank shaped-recipe rows are trimmed; and ForgeGradle's `assemble` already wires reobfuscation [S06]. Validate the built JAR, but do not label the absent explicit `jar.finalizedBy('reobfJar')` a confirmed broken build.

## 4. Conventions to follow

- Keep Java under `com.otectus.plantfiber`, with behavior in the existing `block`, `item`, `entity`, `loot`, and `crafting` packages and registrations in `registry`. New optional Java adapters belong under `compat/<actual_modid>/`; client integrations must remain client-isolated.
- Retain `PlantFiberMod.MODID = "plantfiber"`, snake_case resource IDs, static registry holders, `RegistryObject` references, and DeferredRegister wiring on the MOD bus. Do not conditionally register content based on config or another mod's presence.
- Continue hand-written resources for small additions. There is a runData profile but no datagen architecture. If adding datagen later, migrate a whole resource family atomically and remove its hand-written duplicates; never shadow the same path in both source sets.
- Minecraft 1.20.1 uses `recipes/`, `loot_tables/`, `tags/items/`, and `tags/blocks/`. Use `forge:` conventions only where semantic equivalence has been verified; do not substitute newer `c:` namespaces.
- Keep four-space Java formatting, explicit imports and override annotations, final helper classes/private constructors where already used, and descriptive English translation keys. Avoid unrelated formatting churn and texture changes during fixes.
- For vanilla Forge conditional recipes, use the existing `conditions` convention and verified `forge:mod_loaded` [S03]. That condition format is **not automatically supported by every other mod's custom data loader**.
- Append optional cross-mod tag entries with `replace:false` and `{ "id": "othermod:item", "required": false }` (or optional tag entries with `#`) [S04]. An existing namespace/tag name is not proof that generic Plant Fiber belongs in it.
- Persist all old registry IDs and NBT keys. No task in this roadmap requires renaming one. If a later design does, add and verify an explicit migration/remap before release, including a copied 1.2.0 save test. Never silently delete old beds, knots, rope, or inventory stacks.

## 5. Part A — Fixes

### 5.1 Summary

| ID | Title | Severity | Confidence | Primary location |
|---|---|---|---|---|
| PF-BUG-001 | Config read during armor construction | Critical | LIKELY | `J/registry/ModArmorMaterials.java:17–21` |
| PF-BUG-002 | Breaking bed foot loses the item | High | CONFIRMED | `J/block/GrassBedBlock.java:167–176` |
| PF-BUG-003 | Respawn candidate checks ignore hazards | High | CONFIRMED | `J/block/GrassBedBlock.java:220–266` |
| PF-BUG-004 | Knot interaction skips normal placement transaction | High | LIKELY | `J/entity/RopeKnotEntity.java:110–119` |
| PF-BUG-005 | Bed halves retain inconsistent occupied state | Medium | CONFIRMED | `J/block/GrassBedBlock.java:118–128,215–216` |
| PF-BUG-006 | Client and server read independent gameplay config | Medium | CONFIRMED | `J/PlantFiberMod.java:32` |
| PF-BUG-007 | Published fiber tags are not used by recipes/repair | Medium | CONFIRMED | `D/recipes/`; `J/registry/ModArmorMaterials.java:40–41` |
| PF-BUG-008 | Plant replacement disabled flag is bypassed | Medium | CONFIRMED | `J/block/RopeBlock.java:187–197` |
| PF-BUG-009 | Knot cleanup ignores unsupported-rope policy | Medium | CONFIRMED | `J/entity/RopeKnotEntity.java:126–140` |
| PF-BUG-010 | Failed world mutation can still consume rope | Medium | CONFIRMED | `J/block/RopeBlock.java:266–282,336–353` |
| PF-BUG-011 | Directional rope lacks rotation/mirror behavior | Medium | LIKELY | `J/block/RopeBlock.java:49–85` and class-wide absence |
| PF-BUG-012 | Unknown recipe condition flags enable content | Medium | CONFIRMED | `J/crafting/ConfigCondition.java:43–46,63–64` |
| PF-BUG-013 | Missing/vertical saved knot direction is accepted | Medium | CONFIRMED | `J/entity/RopeKnotEntity.java:68–69,178–181` |
| PF-BUG-014 | Replantable flowers repeatedly yield bonus fiber | Medium | LIKELY | `J/loot/PlantFiberLootModifier.java:26–46`; vegetation tag |
| PF-BUG-015 | No recipe-unlock advancements | Medium | CONFIRMED | All `D/recipes/*.json`; absent `D/advancements/` |
| PF-INC-001 | Disposable starter armor costs 216 raw fiber | Medium | CONFIRMED | Four `D/recipes/grass_<armor>.json` |
| PF-INC-002 | Disabled-use content remains craftable/visible | Low | CONFIRMED | `J/registry/ModCreativeTabs.java:18–40`; recipes |
| PF-INC-003 | Fence-rope instructions omit the knot-only first click | Low | CONFIRMED | `CURSEFORGE.md:30–35` |
| PF-INC-004 | Version metadata has two manual sources | Low | CONFIRMED | `build.gradle:9`; `R/META-INF/mods.toml:7` |
| PF-INC-005 | Grass bed omitted from vanilla semantic bed tags | Low | CONFIRMED | `R/data/minecraft/tags/` inventory |
| PF-INC-006 | Independent crafting flags leave dead-end materials | Low | CONFIRMED | `D/recipes/plant_mesh.json`, `rope.json` |

### PF-BUG-001 — Config read during armor construction

**Severity:** Critical. **Confidence:** LIKELY. **Location:** `J/registry/ModArmorMaterials.java:17–21`; `J/item/GrassArmorItem.java:14–17`; `J/registry/ModItems.java:50–59`; `J/PlantFiberMod.java:23–32`.

**Current vs. expected:** The armor material reads `.get()` on a gameplay config while ArmorItem construction determines durability. Item registration must succeed before configs are available, and configured durability must take effect after loading. Forge 1.20.1's config implementation checks premature reads in development and otherwise can return defaults; the likely outcomes are a development startup exception or production durability frozen to the default [S02,S05]. Neither was reproduced because Gradle could not start.

**Root cause:** Config-dependent material output is used as an item-construction property. Supplying a config reference lazily to the material does not defer ArmorItem's constructor call into that material.

**Fix:** Change `J/registry/ModArmorMaterials.java` to return a stable registration-time baseline. Change `J/item/GrassArmorItem.java` to calculate effective durability through the verified Forge `IForgeItem#getMaxDamage(ItemStack)` hook using a loaded/effective configuration snapshot; add `J/config/EffectivePlantFiberConfig.java` and lifecycle handling shared with PF-BUG-006. Use validated defaults before loading; never read an unloaded spec. Verify the constructor call against decompiled **1.20.1** before finalizing implementation.

**Acceptance criteria:** Client and dedicated development launches pass item registration with no config file. Multiplier 0.5 gives boots/legs/chest/helmet 32/37/40/27; 1.0 gives 65/75/80/55. The client shows the same maximum as the server. Existing damage and enchantment NBT survive.

**Verification:** Build and launch with a fresh config directory; repeat with multiplier 1.0 in a copied test instance. Compare new and pre-existing damaged stacks in dedicated multiplayer. Exercise config reload/disconnect behavior specified in PF-BUG-006.

**Save-compat risk / dependencies:** No ID change. Changing the maximum can make old damage exceed the new maximum: retain damage, clamp presentation safely, and apply normal break behavior on subsequent damage; never repair every stack as a side effect. Coordinate with PF-BUG-006.

### PF-BUG-002 — Breaking the bed foot loses the bed

**Severity:** High. **Confidence:** CONFIRMED. **Location:** `J/block/GrassBedBlock.java:167–176`; `D/loot_tables/blocks/grass_bed.json`.

**Current vs. expected:** Survival destruction removes the other half with `setBlock(AIR, 35)` before loot resolution, regardless of creative mode. Breaking the foot removes the head without loot, while the foot's table excludes itself. Either half should yield exactly one bed in survival, and neither should yield one in creative.

**Root cause:** Unconditional paired-half removal conflicts with a head-only loot owner.

**Fix:** Change `J/block/GrassBedBlock.java` destruction/neighbor handling so one explicitly defined half owns the single drop, including foot-first destruction. Retain or adjust `D/loot_tables/blocks/grass_bed.json` to match that ownership; do not merely remove the part condition and create a two-drop path. Mirror the verified vanilla 1.20.1 two-part destruction ordering after inspecting its decompiled source.

**Acceptance criteria:** Breaking head or foot in survival yields one item and no orphan half; creative yields zero. Support removal and explosion do not produce two beds. Both block entities disappear with their blocks.

**Verification:** Place four beds facing N/E/S/W. Break each half in survival and creative with empty inventory; count drops. Repeat on a copied old-world bed and with explosion/drop rules.

**Save-compat risk / dependencies:** Low if IDs and `part/facing/occupied` properties are retained. Depends on build baseline; coordinate with PF-BUG-003/005.

### PF-BUG-003 — Respawn selection can accept hazardous spaces

**Severity:** High. **Confidence:** CONFIRMED for the deficient predicate; hazard outcomes require in-game verification. **Location:** `J/block/GrassBedBlock.java:220–266`.

**Current vs. expected:** Candidate validation checks a sturdy floor and empty block collision at feet/head. It ignores dangerous fluids, entity-specific safe dismount rules, and border/dimension constraints. Fire or lava can satisfy an empty-collision test. A bed respawn should use safe vanilla-equivalent placement or fail cleanly.

**Root cause:** A bespoke collision-only substitute for bed stand-up/respawn logic.

**Fix:** Change `J/block/GrassBedBlock.java` to validate the paired bed and dimension, then use the verified `BedBlock.findStandUpPosition` helper with the actual entity type, bed facing, and orientation through Forge's respawn hook [S05]. Check the exact pinned signature before coding. Respect world border and an explicit disabled-bed respawn policy. Preserve custom block rendering and existing block/BE IDs.

**Acceptance criteria:** No candidate inside lava, fire, collision, or outside the border is selected. A blocked bed fails safely; valid beds still set and honor respawn. Nether/End use retains intended vanilla explosion behavior.

**Verification:** Set spawn using the bed in a disposable world. Surround candidate spaces with solid blocks except one lava/fire space; die and observe safe fallback. Repeat with one safe space, low ceiling, border edge, both bed halves, all directions, and disabled config.

**Save-compat risk / dependencies:** Low; existing spawn positions must remain usable when their beds are valid. Depends on PF-BUG-002 and config policy PF-BUG-006/PF-INC-002.

### PF-BUG-004 — Knot interaction bypasses normal item-placement handling

**Severity:** High. **Confidence:** LIKELY for protection bypass in a real server; the alternate call path is confirmed. **Location:** `J/entity/RopeKnotEntity.java:110–119`; `J/block/RopeBlock.java:298–353`; `J/item/RopeBlockItem.java:17–26`.

**Current vs. expected:** Clicking a knot directly calls rope extension and world mutation. The normal BlockItem interaction travels through Forge's item placement transaction, including snapshot capture and cancellable placement events; the entity interaction does not [S05]. A player may be allowed to interact with the knot while lacking permission at the distant bottom of its rope column.

**Root cause:** Two placement entry points share a mutation helper but do not share target authorization and event/accounting semantics. This finding is **not** a claim that every direct `setBlock` in the normal item route bypasses Forge.

**Fix:** Refactor `J/block/RopeBlock.java`, `J/entity/RopeKnotEntity.java`, and `J/item/RopeBlockItem.java` around a single placement operation that computes the actual target, checks build/adventure/world permissions there, and participates in one cancellable Forge placement transaction. Reuse verified 1.20.1 hooks from decompiled sources; do not double-post events for the already wrapped item route. Keep the server authoritative and return failure without consumption on denial.

**Acceptance criteria:** Adventure mode, spawn protection, and a cancelled placement at the target prevent both knot and block-item extension. A permission boundary partway down a column cannot be crossed. Exactly one placement event and one item charge occur on success.

**Verification:** Dedicated server with non-op player: knot outside protected spawn, extension target inside; then an event-cancelling test fixture targeting only the bottom block. Repeat via the normal rope item, all four fence sides, and creative mode. A chosen claims mod is an additional compatibility test, not assumed proof for all claims systems.

**Save-compat risk / dependencies:** None to IDs/NBT. Coordinate PF-BUG-008/009/010. Release blocker until reproduced or ruled out on the pinned Forge runtime.

### PF-BUG-005 — Bed occupied state diverges between halves

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/block/GrassBedBlock.java:118–128,180–191,215–216`.

**Current vs. expected:** Successful sleep marks both halves occupied, but `setBedOccupied` updates only the supplied position; the neighbor update does not copy occupancy. Wake-up can therefore clear the head while leaving the foot occupied. This is a state inconsistency, not proof that the usable head stays permanently locked.

**Root cause:** Initial sleep and lifecycle callbacks have different ownership rules for paired state.

**Fix:** In `J/block/GrassBedBlock.java`, normalize to a validated head/foot pair and update both through a bounded helper, avoiding neighbor recursion. Clear both on wake/cancel/removal when appropriate. Handle a missing partner without creating blocks.

**Acceptance criteria:** Both states agree after sleep, wake, interrupted sleep, reload, and counterpart removal; a second player can use an available bed.

**Verification:** Two-player night test; inspect each half's `occupied` value before, during, and after sleep, then reload the chunk. Repeat after breaking one half while occupied.

**Save-compat risk / dependencies:** Existing stale states can be normalized on interaction/neighbor validation without scanning every chunk. Depends on PF-BUG-002/003.

### PF-BUG-006 — COMMON gameplay config is not synchronized

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/PlantFiberMod.java:32`; `J/config/PlantFiberConfig.java:47–126`; `J/item/HealingConsumableItem.java:49–93`; `J/item/GrassArmorItem.java:20–33`; `J/block/RopeBlock.java:165–197`.

**Current vs. expected:** Client prediction, tooltips, visibility, and durability can use different local COMMON values from the server. Forge does not synchronize COMMON automatically [S02]. Connected players should see and predict the server's effective gameplay settings.

**Root cause:** One unsynchronized spec serves registration, recipe loading, client presentation, and runtime authority.

**Fix:** Keep existing COMMON keys for backward compatibility and early recipe conditions. Add `J/config/EffectivePlantFiberConfig.java`, `J/network/PlantFiberNetwork.java`, and a narrowly scoped config snapshot message using the documented 1.20.1 networking pattern [S07]. Populate after config load, send server→client on login and supported reload, and clear the remote snapshot on disconnect. Change gameplay readers in armor/healing/bed/rope/knot/tab code to the appropriate effective view. Do not allow a client packet to modify server settings. Keep recipe condition reads on a correctly loaded early spec; simply moving everything to SERVER can make recipe conditions run before that config is loaded.

**Acceptance criteria:** Deliberately mismatched client/server files still show identical active values while connected. Single-player and pre-world menus use safe defaults/local values. Reload behavior is documented: recipe flags require resource reload/restart; runtime values update consistently. No premature config reads remain.

**Verification:** Two clients with opposite rope/armor flags and different healing durations join one server; compare use results, tooltips, durability, reconnect, config reload, and return to single-player.

**Save-compat risk / dependencies:** Preserve key paths and old configuration files. Protocol changes require matching mod versions; no world content migration. Depends on PF-BUG-001; precedes integrations with temperature or healing.

### PF-BUG-007 — Fiber tags are published but bypassed

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `D/tags/items/fibers.json`, `reinforced_fibers.json`; `D/recipes/grass_bandage.json`, `grass_plaster.json`, `plant_mesh.json`, `reinforced_fibers.json`, `rope.json`, `grass_fiber_block.json`, `string_from_plant_fiber.json`; `J/registry/ModArmorMaterials.java:40–41`.

**Current vs. expected:** Adding equivalent fibers to the advertised tags does not make the main recipes or armor repair accept them. Reinforced fibers also require an exact vanilla string instead of the verified Forge string convention.

**Root cause:** Ingredient definitions use item IDs where semantic input tags already exist.

**Fix:** Change the listed raw-fiber recipe inputs and armor repair to `#plantfiber:fibers`; change rope input to `#plantfiber:reinforced_fibers`; change reinforced-fiber string input to `#forge:string` after verifying the loaded tag includes vanilla string (Comforts' Forge recipe uses it [S21]). Keep the block-decompression input exact and output canonical Plant Fiber. Update `J/registry/ModTags.java` if a string TagKey is needed; update README/CURSEFORGE with the precise equivalence policy.

**Acceptance criteria:** A test datapack adding another semantically equivalent item makes every intended recipe and repair accept it. Recipes still work without optional mods. A material graph check finds no profitable compression/conversion loop.

**Verification:** Add a disposable test item/tag entry, `/reload`, craft each affected recipe and repair armor. Verify 9→1→9 block roundtrip. Check all optional entries with the target mod absent.

**Save-compat risk / dependencies:** Existing item and recipe IDs retained. Balance risk from broad tag membership; never tag raw Plant Fiber as string, wool, a crop, or hemp. Precedes material integrations and PF-INC-001.

### PF-BUG-008 — Plant-replacement toggle falls through

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/block/RopeBlock.java:187–197`; `J/config/PlantFiberConfig.java:123–125`.

**Current vs. expected:** With replacement disabled, tagged vegetation that is normally replaceable can still pass the final `state.canBeReplaced()` branch. The flag should govern tagged plants consistently.

**Root cause:** A failed vegetation-specific allow check falls through into a broader allow rule.

**Fix:** Reorder `J/block/RopeBlock.java` checks: explicit `rope_replaceable_blocks` remains a documented pack override; otherwise, if the state is tagged vegetation, return the plant flag decision; only non-vegetation reaches the general replaceability fallback. Preserve fluid policy separately.

**Acceptance criteria:** With the flag false, ordinary grass/fern in a target cell prevents extension; true allows it. An explicit replacement-tag override works as documented. No rope is consumed on failure.

**Verification:** Place identical rope columns over grass, fern, cave air, water, and a solid block; test both config values through item and knot routes.

**Save-compat risk / dependencies:** None; affects future placement only. Depends on PF-BUG-004/006/010.

### PF-BUG-009 — Knot cleanup ignores the rope support policy

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/entity/RopeKnotEntity.java:97–105,126–140`; `J/block/RopeBlock.java:110–157`.

**Current vs. expected:** Destroying a knot unconditionally destroys the adjacent matching top rope block. This occurs even when `ropeBreaksWhenUnsupported=false`, and without considering a valid independent support above. The rope block's own survival logic says such a rope may remain.

**Root cause:** Entity cleanup directly destroys a dependent block instead of applying the shared survival policy.

**Fix:** Change `J/entity/RopeKnotEntity.java` to request bounded rope support re-evaluation after genuine knot destruction; change `J/block/RopeBlock.java` if necessary to distinguish a still-valid fence/upper support from an absent one. Honor the break and drop flags separately. Keep chunk unload exempt from destructive cleanup.

**Acceptance criteria:** With unsupported breaking disabled, knot/fence removal does not delete rope. With it enabled, unsupported rope collapses once; independently supported rope remains. Drop false produces no support-loss refunds.

**Verification:** Matrix of break flag × drop flag × upper support, removing knot and fence separately. Unload/reload and server-stop tests must leave valid rope intact.

**Save-compat risk / dependencies:** No ID change. Preserve existing fence-anchored state and direction. Depends on PF-BUG-004/006; coordinate PF-BUG-010.

### PF-BUG-010 — Rope is charged without confirming mutation success

**Severity:** Medium. **Confidence:** CONFIRMED for unchecked return values; natural failure frequency unmeasured. **Location:** `J/block/RopeBlock.java:266–282,336–353`.

**Current vs. expected:** `addFreshEntity` and `setBlock` results are ignored; sound and stack shrink follow regardless. A failed insertion/placement should not cost rope or claim success.

**Root cause:** Item accounting is not conditional on a committed world change.

**Fix:** In `J/block/RopeBlock.java`, propagate success/failure from entity/block mutation through the shared transaction from PF-BUG-004. Play sound and consume only after success. Roll back a partially performed combined operation, if any; do not charge twice when Forge restores captured stacks.

**Acceptance criteria:** Denied placement/entity insertion leaves inventory and world unchanged. Successful knot costs one; each successful block costs one; creative costs zero.

**Verification:** Cancel entity insertion/placement in a controlled Forge event fixture and compare stack counts. Repeat at height/border limits and with one remaining rope.

**Save-compat risk / dependencies:** None. Implement with PF-BUG-004 rather than adding a second transaction layer.

### PF-BUG-011 — Rope direction does not transform with structures

**Severity:** Medium. **Confidence:** LIKELY. **Location:** `J/block/RopeBlock.java:49–85` and absence of rotation/mirror overrides in the full class.

**Current vs. expected:** Fence-anchored rope stores `HORIZONTAL_FACING`, but the block does not implement direction transforms. Structure rotation/mirroring is expected to retain the attachment relationship.

**Root cause:** Directional state was added without corresponding structure transforms.

**Fix:** Add the pinned 1.20.1 block `rotate`/`mirror` overrides to `J/block/RopeBlock.java`, verified against decompiled vanilla directional blocks. Also test entity-containing structures; add appropriate knot direction transform handling to `J/entity/RopeKnotEntity.java` only if the runtime test shows that the base hanging-entity path does not cover it.

**Acceptance criteria:** A saved fence/knot/rope structure placed at 90°, 180°, 270°, and both mirror modes retains attachment and model orientation without spurious collapse.

**Verification:** Structure-block save with entities enabled; place each transform in a disposable world, wait several ticks, then extend the rope.

**Save-compat risk / dependencies:** No NBT key or blockstate rename. Existing untransformed saves unchanged. Depends on PF-BUG-009/013; exact knot transform API is UNVERIFIED until the structure test.

### PF-BUG-012 — Unknown config-condition flags fail open

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/crafting/ConfigCondition.java:25–29,43–46,63–64`.

**Current vs. expected:** A typo or unsupported flag enables a recipe. A pack author can believe a feature is gated while the recipe remains active.

**Root cause:** The implementation intentionally treats a missing map entry as true.

**Fix:** Validate flag names in `ConfigCondition.Serializer.read`, produce a clear recipe-loading error naming the unknown flag and supported keys, and never silently return true for unknown flags. Expand the recognized map only for deliberately supported flags needed by PF-INC-002/006. Update README/CURSEFORGE configuration examples.

**Acceptance criteria:** Known true/false flags behave correctly. Misspelled flags identify the exact bad recipe/key during reload and cannot enable the recipe.

**Verification:** Disposable recipe variants with a valid enabled flag, valid disabled flag, and `enableRpoe`; run `/reload` and inspect logs/recipe availability.

**Save-compat risk / dependencies:** No save risk; previously mistyped datapacks need correction. This is an intentional stricter validation change and needs a changelog entry.

### PF-BUG-013 — Knot direction NBT is not validated

**Severity:** Medium. **Confidence:** CONFIRMED. **Location:** `J/entity/RopeKnotEntity.java:68–69,172–181`; `J/block/RopeBlock.java:307–315`.

**Current vs. expected:** Missing `ExitDir` reads as byte 0 (DOWN); explicit UP/DOWN is accepted. Extension later tries to use a vertical direction in a horizontal-only blockstate property. Legacy or malformed saved entities should recover safely.

**Root cause:** General Direction decoding is used for a horizontal-only invariant without validation.

**Fix:** Validate both the public setter and NBT load in `J/entity/RopeKnotEntity.java`. Accept N/E/S/W, use NORTH as a documented fallback for absent/vertical values, preserve base hanging-position NBT, and recompute the box. Preserve the `ExitDir` key. Remove the unused `OFFSET_Y` constant while touching this class if no intended use is found.

**Acceptance criteria:** Valid old knots retain direction. Missing, vertical, and out-of-range byte inputs cannot crash extension or produce vertical exits.

**Verification:** In a disposable world, alter a knot's `ExitDir` with `/data` to missing/0/1 and a valid horizontal value, reload, and interact with rope. Also test a newly spawned knot and an old saved knot.

**Save-compat risk / dependencies:** This is a migration-safe repair, not entity deletion. Depends on PF-BUG-011 only for structure-transform follow-up.

### PF-BUG-014 — Replantable flowers offer a repeatable bonus-fiber loop

**Severity:** Medium. **Confidence:** LIKELY as an unintended economy issue. **Location:** `D/tags/blocks/vegetation.json`; `J/loot/PlantFiberLootModifier.java:26–46`; `D/recipes/string_from_plant_fiber.json`.

**Current vs. expected:** The modifier rewards tagged block loot without checking whether a plant was grown/consumed. A self-dropping flower can be placed and broken repeatedly, retaining the flower while accumulating fiber and then string. Renewable fiber is intended; requiring no plant growth or material consumption is the questionable part.

**Root cause:** One broad block tag conflates expendable wild vegetation, self-returning decorative plants, crops, and multipart plants.

**Fix:** Default to removing self-returning decorative flowers and comparable trivial loops from direct bonus drops in `D/tags/blocks/vegetation.json`, while retaining that public extension tag. Document the policy in README/CURSEFORGE. Add consumed-plant recipes through PF-FEAT-003 for plants that should remain sources. If preserving exact historical drops is preferred, provide an explicitly named legacy profile rather than tracking every placed plant in world NBT.

**Acceptance criteria:** Repeated place/break of one flower yields no net fiber in the default profile. Breaking intended expendable grass still yields fiber at the configured rate. Normal crop harvest and multipart drops are quantified before being added to new profiles.

**Verification:** Set chance to 1.0, start with one dandelion, repeat place/break 20 times, count retained flowers/fibers. Repeat with grass and each remaining source family. Restore 0.25 and run a larger sample; no statistical test is required to prove the deterministic loop removal.

**Save-compat risk / dependencies:** No ID migration; changes economy and datapack defaults. Depends on PF-BUG-007; coordinate PF-FEAT-002/003. Author default: remove the loop, keep renewability through growth or consumption.

### PF-BUG-015 — Crafting recipes lack recipe-book unlocks

**Severity:** Medium. **Confidence:** CONFIRMED for missing resources. **Location:** all 15 `D/recipes/*.json`; no `D/advancements/recipes/` exists.

**Current vs. expected:** Acquiring the relevant ingredients does not award the mod's recipes through advancements. This reduces discoverability and leaves the 13 crafting recipes unavailable under limited crafting until externally granted.

**Root cause:** Hand-written recipes were added without their unlock resources.

**Fix:** Create `D/advancements/recipes/<recipe_name>.json` for all 15 recipes, including cooking discovery, with ingredient acquisition triggers and recipe rewards appropriate to each input. Keep disabled recipes' unlock resources aligned with the supported recipe-condition approach; do not blindly copy third-party condition syntax into advancements. If advancement gating cannot be verified, use safe vanilla triggers and verify no invalid recipe reward errors when disabled.

**Acceptance criteria:** A fresh survival player unlocks each enabled recipe after its intended ingredient. With `doLimitedCrafting=true`, the 13 crafting recipes work after unlock. Disabled combinations load cleanly.

**Verification:** Fresh player, `/advancement revoke @s everything`, `/recipe take @s *`, enable limited crafting, acquire ingredients in progression order, then craft. Repeat with feature flags off and inspect reload logs.

**Save-compat risk / dependencies:** Additive advancements; existing players can unlock upon next qualifying inventory change. Depends on PF-BUG-012 and PF-INC-002/006.

### PF-INC-001 — Starter armor has compressed-block costs

**Severity:** Medium. **Confidence:** CONFIRMED for cost; the balance judgment is a recommendation. **Location:** `D/recipes/grass_boots.json`, `grass_helmet.json`, `grass_chestplate.json`, `grass_leggings.json`; `J/registry/ModArmorMaterials.java:13–21`; `CURSEFORGE.md:23–24`.

**Current vs. expected:** A leather-defense, half-durability set costs 24 fiber blocks = 216 fiber, around 864 default source breaks. A disposable early material should be practical before leather/iron.

**Root cause:** Vanilla armor shapes use compressed blocks instead of the raw tier material.

**Fix:** Change the four recipe inputs to `#plantfiber:fibers`, retaining vanilla shapes and outputs: 4/5/8/7 raw fiber, 24 total. Update README/CURSEFORGE costs. Keep a pack override option for the old expensive recipes; do not add a reversible armor-unpacking recipe. Keep base stats until playtesting demonstrates a separate problem.

**Acceptance criteria:** Default full set costs exactly 24 equivalent raw fibers, averages 96 eligible breaks at 25%, and remains lower-durability leather protection. No existing armor disappears or changes ID.

**Verification:** Count materials in a fresh survival craft; test durability after PF-BUG-001. Compare time to first full grass/leather/iron set in the same seed.

**Save-compat risk / dependencies:** Economy-only; already crafted armor is retained without refunds. Depends on PF-BUG-001/007.

### PF-INC-002 — Feature toggles have inconsistent presentation

**Severity:** Low. **Confidence:** CONFIRMED. **Location:** `J/registry/ModCreativeTabs.java:18–40`; `J/item/ConfigurableBedItem.java:16–20`; `J/item/GrassArmorItem.java:20–33`; `J/item/HealingConsumableItem.java:49–56`; recipes for bed, armor, bandage, plaster; `README.md:30–36`; `CURSEFORGE.md:39`.

**Current vs. expected:** Some disabled features disappear from the tab, some remain, and several still craft despite failing use. Config comments sometimes specify usage rather than crafting, so this is a semantics/UI inconsistency rather than a claim that all existing flags violate their literal comments.

**Root cause:** Feature gating evolved separately in items, recipes, and tab population.

**Fix:** Adopt a documented default: disabling a feature hides it from the tab and prevents new crafting/use where safe, but never unregisters or deletes existing content. Change `J/crafting/ConfigCondition.java`, affected recipes, `J/registry/ModCreativeTabs.java`, config comments, and docs together. Existing placed beds keep a safe respawn fallback; existing worn armor is not forcibly removed or silently destroyed. Specify whether existing armor protection persists until removed; recommended default is yes.

**Acceptance criteria:** A flag matrix matches documentation for recipe, tab, placement/use, existing placed blocks, and existing equipment. No disabled feature creates an unknown registry ID.

**Verification:** Restart/reload with each feature off individually and in combinations; inspect creative tab, crafting, existing save content, and client/server behavior.

**Save-compat risk / dependencies:** Preserve all IDs and config keys. Depends on PF-BUG-006/012 and PF-INC-006.

### PF-INC-003 — Fence-rope instructions omit the first-click cost

**Severity:** Low. **Confidence:** CONFIRMED. **Location:** `CURSEFORGE.md:30–35`; `CHANGELOG.md:7–8`; `J/block/RopeBlock.java:266–282,298–315`.

**Current vs. expected:** Instructions imply side-clicking a fence starts the outward rope column. The first click creates only a knot and consumes one rope; a later click creates the first block.

**Root cause:** User documentation describes the earlier combined interaction rather than the current entity-based behavior.

**Fix:** Update README/CURSEFORGE and add a corrective current changelog entry: first rope ties the knot, next rope starts the outward segment, later clicks extend downward. Explain anchor support, reclaim cost, and configuration. Preserve historical changelog entries; clarify their superseding behavior rather than rewriting history. Add a localized rope tooltip if PF-FEAT-001 is scheduled.

**Acceptance criteria:** Instructions let a new player build a fence rope with the correct inventory cost without experimentation.

**Verification:** Follow the text with exactly two ropes: one knot plus one rope block should result.

**Save-compat risk / dependencies:** None. Depends on final interaction/accounting decisions in PF-BUG-004/010.

### PF-INC-004 — Release version is duplicated manually

**Severity:** Low. **Confidence:** CONFIRMED as maintenance risk, not a current version mismatch. **Location:** `build.gradle:8–13,69–82`; `R/META-INF/mods.toml:7`.

**Current vs. expected:** Both files currently say 1.2.0. A future release can update one and ship mismatched metadata. Jar base name `plantfiber` is intentional and consistent with the namespace.

**Root cause:** Two manually maintained version literals; no metadata expansion task.

**Fix:** Make the Gradle project version the single source and expand a deliberate placeholder in `mods.toml` via `processResources`, following the verified Forge 1.20.1 MDK pattern [S06]. Change only `build.gradle` and `R/META-INF/mods.toml`; retain sensible development resource processing. Do not lower or arbitrarily raise the Forge minimum until the launch matrix establishes it.

**Acceptance criteria:** Built JAR name, manifest, and packaged TOML report the same release version, with no unexpanded placeholder. `build --dry-run` includes reobfuscation through assembly.

**Verification:** `./gradlew build`; inspect the built JAR's TOML/manifest and run that packaged JAR in a dedicated Forge instance.

**Save-compat risk / dependencies:** None; registry namespace unchanged. Build/network access must be restored in the execution environment.

### PF-INC-005 — Grass bed lacks semantic bed tags

**Severity:** Low. **Confidence:** CONFIRMED. **Location:** full `R/data/minecraft/tags/` inventory; `J/block/GrassBedBlock.java:205–216`.

**Current vs. expected:** The block declares itself a bed through Forge hooks but is absent from vanilla bed block/item tags. Tag-based tools or recipes cannot classify it as a bed.

**Root cause:** Java behavior was implemented without equivalent semantic data membership.

**Fix:** Add `R/data/minecraft/tags/blocks/beds.json` and `R/data/minecraft/tags/items/beds.json`, appending `plantfiber:grass_bed` with `replace:false`. Verify vanilla 1.20.1 tag contents before writing. Document that these tags do **not** automatically register villager HOME point-of-interest behavior.

**Acceptance criteria:** Bed block/item tag queries include the grass bed and all vanilla beds. No claim is made that this alone enables villagers or another sleep overhaul.

**Verification:** Inspect loaded tags and test a tag-based ingredient/query; repeat sleep/respawn tests after changes.

**Save-compat risk / dependencies:** Additive tag change; may expand recipes in packs using the bed tag. Depends on bed fixes; native tag contents are a short pinned-resource verification task.

### PF-INC-006 — Crafting toggles leave dead-end progression

**Severity:** Low. **Confidence:** CONFIRMED. **Location:** `D/recipes/plant_mesh.json`, both paper recipes, `reinforced_fibers.json`, `rope.json`; `J/config/PlantFiberConfig.java:98–110`.

**Current vs. expected:** Disabling both paper routes leaves mesh craftable with no remaining default sink. Disabling reinforced-fiber crafting leaves rope crafting enabled but without its normal newly obtainable ingredient. Existing stock or other mods can make this intentional, so it is not automatically an unobtainable registry defect.

**Root cause:** Independent flags lack a documented dependency policy.

**Fix:** Define default effective recipe gates in `J/crafting/ConfigCondition.java` and the named recipes: hide mesh crafting when its only enabled sink is absent; hide the default rope recipe when its only default material route is disabled. Permit an explicit pack profile to supply replacement routes. Update config comments, tab presentation, and docs without deleting old stacks. Revisit mesh gating once drying/other sinks are actually shipped.

**Acceptance criteria:** Every advertised default craftable material has an enabled sink and every advertised default recipe has an intended obtainable input. Alternate pack recipes remain possible.

**Verification:** Exercise all combinations of paper/reinforced/rope flags with no optional mods, then with an explicit replacement-recipe datapack.

**Save-compat risk / dependencies:** Existing items remain; no refunds or deletion. Depends on PF-BUG-007/012 and PF-INC-002; later processing features extend the dependency graph.

## 6. Part B — Expansions

### 6.1 Design rules and scoring

These are proposed designs, not claims that features already exist. **PROPOSED** marks new Plant Fiber IDs, configuration keys, or formats. **UNVERIFIED —** marks an external API/detail not established during this review. A new design does not become a verified external API merely by appearing in this document.

For every expansion block, severity is **N/A: enhancement**; priority is P1 (strong first choice), P2 (useful follow-on), or P3 (optional/experimental). Confidence in player-facing outcomes is **NEEDS IN-GAME TEST**, with an exact acceptance test below. Existing-code and external-hook evidence is separately cited. Value and identity fit score 1–5 (higher is better); effort S/M/L is relative engineering scope, not a delivery-date estimate. Risk combines balance, overlap, persistence, and maintenance. Ship optional integrations in independently removable groups; none justifies a hard dependency.

| Feature | Value | Fit | Effort | Risk | Priority | Dependencies |
|---|---:|---:|---|---|---|---|
| PF-FEAT-001 Guidance and progression milestones | 5 | 5 | S | Low | P1 | BUG-015, INC-003 |
| PF-FEAT-002 Data-driven harvest profiles | 5 | 5 | M | Medium | P1 | BUG-006/007/014 |
| PF-FEAT-003 Consumed-plant processing/fallback sources | 5 | 5 | S | Medium | P1 | BUG-007/014 |
| PF-FEAT-004 Wetting and slow drying frame | 5 | 5 | L | Medium | P1 | BUG-006, FEAT-003 |
| PF-FEAT-005 Woven cloth with defined uses | 4 | 5 | M | Medium | P1 | BUG-007, FEAT-004 |
| PF-FEAT-006 Rope spool | 5 | 5 | M | Medium | P1 | BUG-004/009/010/013, FEAT-005 |
| PF-FEAT-007 Controlled rope retrieval | 4 | 5 | M | Medium | P2 | BUG-004/009/010, FEAT-006 |
| PF-FEAT-008 Healing-use quality of life | 4 | 4 | S | Medium | P1 | BUG-006, INC-002 |
| PF-FEAT-009 Woven decoration | 3 | 5 | M | Low | P2 | FEAT-005 |
| PF-FEAT-010 Modest trades and structure loot | 3 | 4 | S | Medium | P2 | BUG-014, FEAT-001 |
| PF-FEAT-011 Compost and small fuel sink | 3 | 4 | S | Low | P2 | BUG-007 |
| PF-FEAT-012 One optional wetland fiber plant | 3 | 4 | L | High | P3 | FEAT-002/003/010 |
| PF-FEAT-013 Pack profiles and diagnostics | 5 | 5 | M | Medium | P1 | BUG-006/012, INC-006 |

### PF-FEAT-001 — Explain the existing survival progression

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort S; risk Low.

**Priority:** P1. **Evidence/location:** all recipes; `A/lang/en_us.json`; `J/item/HealingConsumableItem.java:80–93`; PF-BUG-015.

**Current vs. expected:** Players must infer plant sources, mesh drying, knot cost, and material progression. Add concise guidance without requiring a guidebook mod.

**Design:** Milestones for first fiber, first string, wet mesh/paper, reinforced fiber, usable rope, bed, and first bandage. Rope tooltip explains the two-click fence workflow; material tooltips name enabled sinks. A small root advancement branch is narrative guidance, separate from recipe-book unlocks.

**Implementation outline:** Add `D/advancements/progression/{root,first_fiber,first_string,wet_mesh,first_paper,reinforced,first_rope,first_bed,first_bandage}.json` (PROPOSED paths), translations in `A/lang/en_us.json`, and tooltip helpers in `J/item/` only where needed. No new items or sync protocol beyond effective config. Do not print a hardcoded 25% chance if a pack changes it.

**Acceptance/verification:** A new player follows the branch from one grass break to rope/paper without external instructions; disabled branches do not advertise unavailable recipes. Test with recipe viewers absent and with non-default drop chance. Check all visible text at normal GUI scale.

**Save-compat/dependencies:** Additive advancements, no ID changes; PF-BUG-015, PF-INC-003, and PF-BUG-006 for dynamic text.

### PF-FEAT-002 — Explicit harvest rules instead of one broad lottery

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort M; risk Medium.

**Priority:** P1. **Evidence/location:** `J/loot/PlantFiberLootModifier.java:26–46`; `D/tags/blocks/vegetation.json`.

**Current vs. expected:** Every tagged block gets the same roll regardless of growth or tool. Packs need controlled source families without editing Java or multiplying several GLMs.

**Design:** PROPOSED `data/<namespace>/plantfiber/fiber_sources/<id>.json` rules define a block/tag selector, optional state-property constraints, an optional tool tag, chance, and bounded count. The default wild-grass rule remains one fiber at 0.25. Use deterministic rule priority and **first matching rule**, so overlapping tags do not stack rolls. Decorative replantable plants remain outside default bonus drops.

**Implementation outline:** Add `J/loot/FiberSourceRule.java` and `J/loot/FiberSourceReloadListener.java`, registration in `J/event/ModSetup.java` or an appropriately scoped Forge resource-reload handler verified against 1.20.1, and integrate with the existing GLM rather than adding a second payout. Validate unknown properties, invalid ranges, and empty selectors on reload; atomically replace the immutable rule list. Publish the new format and all defaults in README/CURSEFORGE. Keep the existing vegetation tag as a documented fallback/extension point. No per-block placed-plant NBT or global ticking.

**Acceptance/verification:** A datapack can make mature configured plants yield exactly one and immature ones zero; overlapping rules still yield at most one selected payout. Chance 0 and 1 are deterministic. Test hand/shears/other tools, both halves of double plants, water break, and explosion contexts with explicit expected policy. A malformed rule reports its resource ID and cannot leave a partially loaded rule set.

**Save-compat/dependencies:** No world data migration. New format is a public maintenance commitment; version it/document reload semantics. PF-BUG-006/007/014.

### PF-FEAT-003 — Consume plant material to make reliable fiber

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort S; risk Medium.

**Priority:** P1. **Evidence/location:** current GLM and raw-fiber sinks; mesh's bucket requirement in `D/recipes/plant_mesh.json`.

**Current vs. expected:** Sources rely on random block loot and can be scarce in unusual survival worlds. Add deterministic processing that consumes the plant instead of returning it for repeated bonus rolls.

**Design:** PROPOSED `plantfiber:processable_plants` item tag; start with explicitly chosen dead/leaf/kelp material families. A default shapeless recipe consumes three eligible items for one fiber. Keep food-bearing kelp membership optional so food/compost economies remain a pack decision. Flowers removed from the bonus-drop tag may be deliberately admitted here. Never include fiber itself or a container-returning item.

**Implementation outline:** Add `D/tags/items/processable_plants.json`, `D/recipes/fiber_from_processed_plants.json`, and its unlock advancement; use `J/registry/ModTags.java` only if Java needs the tag. Hand-write resources. Prefer separate named source tags if families need different ratios. Use optional mod entries only after concrete native material verification.

**Acceptance/verification:** Three source items are consumed, one fiber produced, no replant/drop loop remains, and no source can be reconstructed for less than three material units. Test a treeless/coastal seed and a Primal Winter world for first fiber without importing items.

**Save-compat/dependencies:** Additive recipes; economy risk is in membership, not registry migration. PF-BUG-007/014; feeds FD/Pam/NTP/Primal Winter integrations.

### PF-FEAT-004 — A small wetting and drying frame

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort L; risk Medium.

**Priority:** P1. **Evidence/location:** mesh and paper recipes; `J/block/GrassBedBlockEntity.java` shows there is currently no inventory/progress infrastructure.

**Current vs. expected:** Making wet mesh requires an iron bucket, then dries in furnace/campfire in ten seconds. Offer a slower pre-iron route with a visible survival action.

**Design:** PROPOSED `plantfiber:drying_frame` block/item/BE, four batch slots. Craft from four sticks and two fibers in a documented shaped recipe. Insert three fibers into an empty slot and wet with a vanilla water bottle; consume water and return one empty bottle to produce one mesh. One mesh dries to one paper in 1,200 loaded server ticks (60 seconds), without fuel. Keep current furnace/campfire routes. A frame works under a roof; rain/weather modifiers are off by default so climate does not create a progression lock.

**Implementation outline:** Add registrations in `ModBlocks`, `ModItems`, `ModBlockEntities`; `J/block/DryingFrameBlock.java` and `DryingFrameBlockEntity.java`; blockstate, block/item models, textures, lang, self/inventory loot behavior, recipe, mining tag, and unlocks. Store item counts/progress with symmetric NBT and `setChanged`; tick only on the server, at coarse intervals while active, pausing when unloaded. Sync changed visible slots/progress; no menu is necessary for four hand interactions. No hopper support in the first release. **UNVERIFIED —** inspect the pinned PotionUtils/water-bottle check and bottle remainder behavior before coding; ordinary healing potions must not count as water.

**Acceptance/verification:** One batch consumes three fibers and one bottle of water, returns the bottle, and eventually gives one paper; no output on client-only prediction. Break, unload, restart, and two-player simultaneous interaction preserve exact materials. Test four batches, full inventory bottle return, blocked placement, and frame breaking mid-process.

**Save-compat/dependencies:** New BE persistence requires migration discipline from its first release. No old ID changes. PF-BUG-006 and PF-FEAT-003; do not build on the bed BE class.

### PF-FEAT-005 — Woven cloth with immediate sinks

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 4/5, fit 5/5; effort M; risk Medium.

**Priority:** P1. **Evidence/location:** `reinforced_fibers` currently feeds only rope; healing and armor are existing fiber sinks.

**Current vs. expected:** Add a useful textile tier without making a redundant generic material or bypassing wool-dependent technologies.

**Design:** PROPOSED item/tag `plantfiber:woven_cloth`; a 2×2 of reinforced fibers yields one cloth (16 raw-fiber equivalents through the default string route). No reverse recipe. Immediate sinks: rope spool, woven mats, and optional native First Aid/Comforts recipes. Keep starter armor on raw fiber after PF-INC-001; cloth is not a mandatory new armor bottleneck.

**Implementation outline:** Register in `ModItems` and tab; add item model/texture/lang, tag, crafting recipe and advancement. Add cloth sink recipes in the same release. Accept specific verified native fabrics into the **Plant Fiber** cloth tag where equivalent; do not insert generic cloth into `minecraft:wool` or `forge:fabric_hemp`. Initial hand crafting keeps the mod small; frame/loom animation can come later without a second cloth item.

**Acceptance/verification:** Every enabled profile supplies at least one attainable cloth sink; no reverse conversion increases fiber/string. Craft each sink and test with optional native fabric members. Compare cloth cost with wool and hemp fabric before enabling replacement recipes.

**Save-compat/dependencies:** Additive ID; never later remove it merely because an integration is installed. PF-BUG-007, PF-FEAT-004 for the overall processing release; cloth recipe itself does not require wetting.

### PF-FEAT-006 — Rope spool

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort M; risk Medium.

**Priority:** P1. **Evidence/location:** `J/block/RopeBlock.java:207–353`; `J/entity/RopeKnotEntity.java:110–119`.

**Current vs. expected:** Long climbs consume inventory slots and repeated item handling. A spool should store rope and invoke the same verified placement logic.

**Design:** PROPOSED `plantfiber:rope_spool`, stack size one, capacity 128 **Plant Fiber ropes**. Craft empty from two cloth and two sticks. Filling transfers existing rope from inventory; emptying returns only the stored amount. Spool placement follows the same knot/segment costs as loose rope. No free initial rope and no universal storage of other mods' rope blocks.

**Implementation outline:** Add `J/item/RopeSpoolItem.java`, registry/tab/model/texture/lang/recipe/advancement, and bounded integer NBT count. Reuse the placement service from PF-BUG-004; server validates counts and permissions. Define sneak-use fill/empty behavior explicitly and handle full inventory without deleting items. Use existing item-stack sync; do not trust client-supplied stored counts or arbitrary embedded ItemStacks.

**Acceptance/verification:** Loading 128 ropes, placing knot plus 20 blocks, and emptying yields exactly 107 loose ropes. Creative placement does not inflate stored rope. Test reconnect, death drops, full inventory, offhand use, two clients, and denied placement.

**Save-compat/dependencies:** New NBT schema with bounded default zero; preserve existing rope/knot IDs. PF-BUG-004/009/010/013 and PF-FEAT-005. Curios integration is optional later.

### PF-FEAT-007 — Controlled rope retrieval

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 4/5, fit 5/5; effort M; risk Medium.

**Priority:** P2. **Evidence/location:** rope support cascade and knot ownership in `J/block/RopeBlock.java:110–157`; `J/entity/RopeKnotEntity.java:126–152`.

**Current vs. expected:** Reclaiming a column by destroying its support produces falling item stacks and can cross protected areas. Offer deliberate short-range retrieval.

**Design:** Sneak-use a spool on rope to retrieve at most 16 connected segments per action, stopping at a denied/foreign/unsupported boundary. Refund only successfully removed Plant Fiber rope. A knot is a distinct final reclaim action. Do not automatically collect another mod's rope.

**Implementation outline:** Extend spool and shared rope service; verify permission at every affected position, avoid duplicate loot from support cascades, and stop cleanly when capacity is full. No new global ticker; bounded work per action. Document whether ordinary shears remain normal block breaking; default does not add a new mandatory tool.

**Acceptance/verification:** Reclaim a 40-block column over multiple actions, crossing a permission boundary and a full-spool boundary. Total loose + stored + world rope never increases. A refused segment remains intact and no protected lower segment is removed indirectly.

**Save-compat/dependencies:** No new registry beyond spool. PF-BUG-004/009/010 and PF-FEAT-006; protection/cascade tests are essential.

### PF-FEAT-008 — Predictable healing use and shared cooldown option

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 4/5, fit 4/5; effort S; risk Medium.

**Priority:** P1. **Evidence/location:** `J/item/HealingConsumableItem.java:49–102`; `J/registry/ModItems.java:30–48`.

**Current vs. expected:** Separate item cooldowns permit alternating bandage/plaster; tooltips use local config. This is not automatically a vanilla cooldown bypass, but a pack may prefer one dressing cooldown.

**Design:** PROPOSED `healing.sharedDressingCooldown`, default false to preserve current behavior; when enabled, either dressing applies its cooldown to both. Keep 24-tick use and existing default effects. Display effective-server duration/strength/cooldown. Keep pre-buff use allowed by default; an optional prevent-waste setting can reject a use that cannot improve health/effect state, with a clear message.

**Implementation outline:** Change `HealingConsumableItem`, `PlantFiberConfig`, effective config snapshot, and lang. Re-check enabled state at completion so a mid-use disable cannot grant a now-disabled effect. Use native cooldown/effect systems; no custom health capability. Do not promise First Aid or LSO limb treatment from vanilla regeneration.

**Acceptance/verification:** Both configurations behave as documented when alternating dressings, in creative, at full health, under stronger regeneration, and when interrupted. Count exactly one consumed item per completed use; disconnect/reload does not duplicate effects.

**Save-compat/dependencies:** No item renames; new flags default to current behavior. PF-BUG-006 and PF-INC-002. Native injury integration is a separate optional recipe task.

### PF-FEAT-009 — A restrained woven decoration set

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 3/5, fit 5/5; effort M; risk Low.

**Current vs. expected:** The only current bulk decorative material is the full grass fiber block; add a small coordinated set of thin building forms.

**Priority:** P2. **Evidence/location:** existing fiber block and grass/rope textures under `A/textures/`.

**Design:** PROPOSED `woven_mat`, `thatch_slab`, and `woven_panel` blocks/items, using cloth/fiber blocks with three distinct building uses. Start with undyed neutral plant colors; do not create sixteen colors and multiple fiber species in the first release. Mats are decorative, not sleeping bags or insulation devices by themselves.

**Implementation outline:** Register three blocks/items using ordinary 1.20.1 block bases after checking their constructors. Add complete blockstates/models/textures/lang/loot/recipes/advancements and appropriate hoe tags. Keep flammability consistent with raw plant material. Recipe counts must conserve material if any reverse recipe is offered; recommended default is no reverse for thin decorative products. Slab waterlogging uses the native slab behavior.

**Acceptance/verification:** All faces/rotations/slab halves render correctly; pick-block and survival drops match IDs; explosion/fire/water behavior is intentional. Build a small vanilla-material house and inspect palette at daylight/night.

**Save-compat/dependencies:** Additive blocks become permanent save commitments. PF-FEAT-005; no new crafting station or sync.

### PF-FEAT-010 — Limited world and trading presence

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 3/5, fit 4/5; effort S; risk Medium.

**Current vs. expected:** The mod currently has no structure-loot or trading entry point; add limited optional world distribution and an excess-fiber trade.

**Priority:** P2. **Evidence/location:** no current structure loot or villager events; fiber is solely a harvest/craft economy.

**Design:** Optional small village-house chest bundles (one 15% roll for 4–8 fiber) and a farmer purchase offer of 32 fiber for one emerald, max eight uses before native restock. A wandering trader can sell future cattail seeds; do not add that offer before seeds exist. Avoid a guaranteed reciprocal buy/sell emerald loop.

**Implementation outline:** Add a separate loot modifier with an exact allowed chest-table list and its registered codec, gated independently from block drops. Add a correctly bus-scoped trade handler in `J/event/FiberTrades.java` using verified 1.20.1 trade events; enqueue shared-state work only if required by the chosen lifecycle. Add config and guidance text. **UNVERIFIED —** inspect exact village chest IDs and trade constructor/event signatures in pinned sources before coding; do not guess them from newer tutorials.

**Acceptance/verification:** Loot modification occurs only in chosen chest tables, never plant or entity loot. Existing unopened vs already generated chests follow vanilla generation timing. Trade counts, prices, and restock behavior match the design; automation does not create a faster emerald loop than intended.

**Save-compat/dependencies:** Existing generated chests are not rewritten. Trading can persist on existing villagers; disabling future additions must not corrupt offers. PF-BUG-014 and PF-FEAT-001.

### PF-FEAT-011 — Compost and small fuel uses

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 3/5, fit 4/5; effort S; risk Low.

**Current vs. expected:** Excess raw fiber currently lacks compost/fuel use; add two modest sinks with conserved compressed-block fuel value.

**Priority:** P2. **Evidence/location:** raw fiber/block have no compost or fuel hooks; `J/event/ModSetup.java:16–17` already demonstrates `enqueueWork`.

**Design:** Add modest excess-material sinks: raw fiber 50 furnace fuel ticks, fiber block 450 ticks, so compression does not multiply energy; raw-fiber composter chance 0.15. Leave reinforced fiber, rope, and cloth out initially. Do not label fiber edible or a crop.

**Implementation outline:** Add fuel behavior through the pinned Forge item fuel hook after verifying its exact signature; add raw fiber to vanilla compost chances inside enqueued common setup work. Update item helpers/registration only as needed and document numbers. New settings are optional, default enabled, with no mass recipe rewrites.

**Acceptance/verification:** Nine raw fibers and one block provide equal total burn ticks; neither gives more outputs than the other. A large composter sample is consistent with the configured chance. Dedicated startup has no unsafe parallel shared-map mutation.

**Save-compat/dependencies:** No migration. PF-BUG-007; fuel hook signature is UNVERIFIED until pinned source lookup.

### PF-FEAT-012 — One optional wetland plant, only if native integrations leave a gap

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 3/5, fit 4/5; effort L; risk High.

**Current vs. expected:** The mod currently supplies no crop or worldgen; add one optional cultivated source only if existing farming integrations leave a real gap.

**Priority:** P3. **Evidence/location:** no current crops/worldgen; optional mod research supplies many native fiber crops already.

**Design:** PROPOSED `cattail` crop block and `cattail_seeds` item, grown beside water. Mature harvest gives two fiber plus one seed; immature harvest returns only the planted seed. First seed comes from a wandering trader for two emeralds. Optional wetland worldgen is off by default. Keep one fiber output; no five near-identical new fiber items.

**Implementation outline:** Add block/item registration, growth and water checks, age/half-aware loot, seeds/appropriate crop tags, models/textures/lang/advancements, and the enabled trader offer. Verify two-high growth/loot behavior against pinned plants before choosing that shape; one-high reed is the simpler fallback. Add worldgen resources only in a later opt-in profile after biome modifier formats are verified. Serene Seasons integration applies to the crop and seed, not raw fiber.

**Acceptance/verification:** Seed is obtainable with worldgen disabled. Immature break/replant cannot generate fiber or extra seeds; mature upper/lower-half harvest pays once. Test bone meal, water loss, piston/water destruction, crop automation, and all enabled seasons.

**Save-compat/dependencies:** New crop/worldgen IDs are permanent; disable growth/worldgen without unregistering existing plants. PF-FEAT-002/003/010; reconsider if Pam/FD/TFC already supplies the desired progression.

### PF-FEAT-013 — Pack profiles and a small diagnostics command

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Score:** value 5/5, fit 5/5; effort M; risk Medium.

**Current vs. expected:** Config flags and tags currently require manual pack reasoning; provide explicit recipe profiles and concise active-state diagnostics.

**Priority:** P1. **Evidence/location:** existing config condition and public tags; interactions documented in PF-INC-006.

**Design:** Ship documented optional profiles: vanilla survival (default), primitive No Tree Punching, experimental TFC, and Create 6 automation. Profiles replace recipes/rules, not registry entries. PROPOSED `/plantfiber diagnose`, permission level 2, reports effective flags, loaded source-rule count, active supported integrations, and incompatible profile combinations; it exposes no filesystem paths or player data.

**Implementation outline:** Add profile datapack examples and precise installation/override documentation; resource packs/data packs must have 1.20.1 metadata. Add `J/command/PlantFiberCommands.java` and appropriate command registration after verifying the 1.20.1 event/API. Validate dependency chains once on reload, not each tick. Avoid automatically rewriting users' files or activating a total-conversion profile solely because a mod is installed.

**Acceptance/verification:** Each profile has a reproducible fresh-world path to fiber and an intended sink, no unknown condition keys, and no conversion loop. Removing a profile restores baseline recipes on reload without missing IDs. Non-op players cannot run diagnostics if its output is administrative.

**Save-compat/dependencies:** Profile switches change economy but retain content. PF-BUG-006/012 and PF-INC-006; integration tasks define their own profile recipes. Exact command API is UNVERIFIED until pinned-source inspection.

### 6.2 Integration release and dependency ledger

**Availability:** All rows below have a verified Forge release advertised for Minecraft 1.20.1. These are explicit research pins, not a promise of being the best future release. Pam's selected file uses a `1.20` filename but its CurseForge listing includes 1.20.1; its permissive internal metadata is not used alone as proof. Several other release branches advance independently of their 1.20.1 artifacts, so native release-JAR resources take precedence over default-branch examples.

**Dependency convention:** For every Modrinth row, the literal compile/dev-runtime coordinate is `maven.modrinth:<slug>:<version-ID>` and the repository is `https://api.modrinth.com/maven`. The artifact POM at `https://api.modrinth.com/maven/maven/modrinth/<slug>/<version-ID>/<slug>-<version-ID>.pom` was retrieved. This coordinate is an exact release pin; it does not rely on resolving a mutable version label. For a data-only integration, **production compile dependency is none**; the coordinate is for testing. For soft Java adapters, use ForgeGradle `compileOnly fg.deobf(...)` and a separate development runtime dependency, isolate loading behind `ModList.get().isLoaded("<actual_modid>")`, and declare the relationship non-mandatory in `mods.toml`. Verify target API signatures after remapping before implementation [S06]. Never bundle the other mod's JAR or infer its transitive runtime requirements from an empty Modrinth dependency list.

| Compat ID | Target / actual mod ID | Verified Forge release | Maven slug : version-ID | License metadata / important floor |
|---|---|---|---|---|
| 001 | Farmer's Delight / `farmersdelight` | 1.20.1-1.3.4 | `farmers-delight:SiIpcZzM` | MIT; pin the 1.3 cutting format [S08] |
| 002 | Pam's HarvestCraft 2 Crops / `pamhc2crops` | 1.20-1.0.3, CF file 4687624 | `curse.maven:pams-harvestcraft-2-crops-361385:4687624` via `https://www.cursemaven.com` | All Rights Reserved [S09] |
| 003 | Serene Seasons / `sereneseasons` | 9.1.0.3 | `serene-seasons:WMrhrybF` | ARR; **Forge ≥47.3.0**, GlitchCore ≥0.0.1.1 [S10] |
| 004 | Tough As Nails / `toughasnails` | 9.2.0.171 | `tough-as-nails:kWVOI0iS` | ARR; **Forge ≥47.3.0**, GlitchCore ≥0.0.1.1 [S11] |
| 005 | Cold Sweat / `cold_sweat` | 2.4.2 | `cold-sweat:arQLKRjC` | Project metadata GPL-3.0-or-later; release TOML has an inconsistent “v3.1” label; do not copy code/assets [S12] |
| 006 | Legendary Survival Overhaul / `legendarysurvivaloverhaul` | 1.20.1-2.4.7 | `legendary-survival-overhaul:BsBOQNzX` | ARR; current release source is Alex-Hashtag's fork [S13] |
| 007 | Thirst Was Taken / `thirst` | 1.20.1-1.4.0 | `thirst-was-taken:v0d2UK5F` | MIT [S14] |
| 008 | Diet / `diet` | 2.1.1+1.20.1 | `diet:oHxMouff` | LGPL-3.0-or-later; coexistence check, no nutrition feature [S15] |
| 009 | No Tree Punching / `notreepunching` | 7.1.0 | `no-tree-punching:aPNOyI3O` | MIT [S16] |
| 010 | TerraFirmaCraft / `tfc` | 3.2.25 | `terrafirmacraft:F35Y7gTr` | Custom license; required Patchouli must be installed per release metadata [S17] |
| 011 | Primal Winter / `primalwinter` | 5.0.0 | `primal-winter:Q6znQM53` | MIT [S18] |
| 012 | Supplementaries / `supplementaries` | 1.20-3.1.43-forge | `supplementaries:S0TIJ1hU` | Custom Team license; Moonlight ≥1.20-2.16.26, Forge ≥47.1; optional Create must be ≥6.0 [S19] |
| 013 | Immersive Engineering / `immersiveengineering` | 10.2.0-183 | `immersiveengineering:WUzj4tgJ` | Blu's custom license; **Forge ≥47.3** [S20] |
| 014 | Comforts / `comforts` | 6.4.0+1.20.1 | `comforts:gBDsc134` | LGPL-3.0-or-later [S21] |
| 015 | Create / `create` | mc1.20.1-6.0.8 | `create:8amzvn9x` | Check Create's code/assets license distinction; Forge ≥47.1.3; release declares Flywheel [1,2) client and Ponder [0.8,) both, potentially embedded [S22] |
| 016 | JEI / `jei` | 15.59.0.210 | `jei:4vbuxppk` | MIT [S23] |
| 017 | EMI / `emi` | 1.1.24+1.20.1+forge | `emi:Axuu9I9R` | MIT; source uses Yarn names, Forge artifact is remapped [S24] |
| 018 | Jade / `jade` | 11.13.3+forge | `jade:xJQHCmWJ` | CC-BY-NC-SA-4.0 project metadata; use public API, no copied assets [S25] |
| 019 | Curios API / `curios` | 5.14.1+1.20.1 | `curios:IPQlZkz1` | LGPL-3.0-or-later [S26] |
| 020 | Patchouli / `patchouli` | 1.20.1-85-forge | `patchouli:94dtOLgZ` | CC-BY-NC-SA-3.0 project metadata; author original book content [S27] |
| 021 | First Aid / `firstaid` | 1.20.1-1.1 | `firstaid:JzYydXeE` | Mod GPL-3.0-only; inspected healing API file separately declares LGPL-2.1 [S28] |

The existing **47.2.0 test instance cannot satisfy selected Serene Seasons, Tough As Nails, or IE requirements**. Maintain a baseline test on the project's pin and a separate integration test on a verified 47.3+ patch. If shipping those combinations as supported, either raise the tested/reported minimum deliberately or document that the optional target mod imposes the higher floor. Do not silently change the Minecraft/loader target.

License labels above are source metadata and constraints to check before copying/distribution, not a legal compatibility opinion. Data-only files authored for Plant Fiber are the default; runtime JARs belong in development/test instances, not inside the Plant Fiber artifact.

### 6.3 Integration ranking

| ID | Player benefit | Value | Fit | Effort | Risk | Priority | Prerequisites |
|---|---|---:|---:|---|---|---|---|
| PF-COMPAT-001 | Farm waste to usable fiber | 5 | 5 | S | Medium | P1 | BUG-007/014, FEAT-003 |
| PF-COMPAT-002 | Five native fiber crops feed one chain | 4 | 5 | S | Medium | P1 | BUG-007 |
| PF-COMPAT-003 | Seasonal growth for optional crop | 3 | 4 | S/M | Medium | P2 | FEAT-012; higher Forge instance |
| PF-COMPAT-004 | Optional light cooling clothing | 4 | 4 | S | Medium | P2 | BUG-006, INC-001; higher Forge |
| PF-COMPAT-005 | Small heat insulation through native data | 4 | 4 | S | Medium | P1 | BUG-006, INC-001 |
| PF-COMPAT-006 | LSO temperature recognition | 4 | 4 | S | Medium | P2 | BUG-006, INC-001 |
| PF-COMPAT-007 | Mesh route to native filter | 2 | 4 | S | Medium | P3 | FEAT-004, Create/Thirst |
| PF-COMPAT-008 | Non-food coexistence validation only | 2 | 3 | S | Low | P2 check | FEAT-008 |
| PF-COMPAT-009 | Shared fiber without primitive-tier bypass | 5 | 5 | M | High | P1 | BUG-007/014, FEAT-013 |
| PF-COMPAT-010 | Explicit TFC progression profile | 3 | 3 | L | High | P3 | FEAT-013, cloth; overhaul tests |
| PF-COMPAT-011 | Winter-world source fallback | 4 | 4 | S | Medium | P2 | FEAT-003 |
| PF-COMPAT-012 | Native flax accepted, distinct ropes retained | 4 | 5 | S | Medium | P1 | BUG-007 |
| PF-COMPAT-013 | Hemp input bridge to Plant Fiber sinks | 4 | 5 | S | Medium | P2 | BUG-007, FEAT-005; higher Forge |
| PF-COMPAT-014 | Cloth in native camping recipes | 3 | 4 | S | Medium | P2 | FEAT-005 |
| PF-COMPAT-015 | Bulk wetting with Create 6 | 5 | 5 | S/M | Medium | P1 | BUG-007, FEAT-004 |
| PF-COMPAT-016 | JEI source/use information | 5 | 5 | S | Low | P1 | FEAT-001, BUG-006 |
| PF-COMPAT-017 | EMI source/use information | 4 | 5 | S | Medium | P2 | FEAT-001, BUG-006 |
| PF-COMPAT-018 | Rope/frame state at a glance | 4 | 5 | S/M | Low | P2 | BUG-006/009; FEAT-004 for progress |
| PF-COMPAT-019 | Equip an actual rope spool | 2 | 3 | M | Medium | P3 | FEAT-006 |
| PF-COMPAT-020 | Optional field guide | 3 | 5 | S/M | Low | P2 | FEAT-001/004/005 |
| PF-COMPAT-021 | Cloth crafts native limb dressings | 4 | 5 | S | Medium | P1 after cloth | FEAT-005 |

### 6.4 Integration implementation blocks

For each block, the target version, exact dependency coordinate, license constraints, and priority/score are the corresponding row of sections 6.2–6.3. They are part of the task specification. All blocks inherit **Severity: N/A; Confidence: NEEDS IN-GAME TEST** for the proposed integration's behavior. Hooks explicitly described as verified were read in the cited source or release JAR. Each source entry includes a release-availability URL and concrete hook evidence. Test each target both installed and absent.

### PF-COMPAT-001 — Farmer's Delight

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 5/5, fit 5/5; effort S; risk Medium.

**Verified target:** Farmer's Delight / `farmersdelight`, Forge / Minecraft 1.20.1, release 1.20.1-1.3.4. **Exact dependency pin:** `maven.modrinth:farmers-delight:SiIpcZzM`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** `D/recipes/` and `D/tags/items/fibers.json` currently know only Plant Fiber materials. Native FD straw and rope overlap with the source/sink chain.

**Design:** Let consumed FD straw yield Plant Fiber through a cutting-board recipe, giving harvested farm waste a useful choice. Start at one straw → one fiber; review against native straw uses before changing yield. Preserve FD rope for its crop/support behavior and Plant Fiber rope for its knot/climbing behavior; do not alias their block IDs or provide automatic reciprocal conversion.

**Verified hooks:** In 1.3.4's 1.20 source, `farmersdelight:cutting` uses an `ingredients` array, `result` array, and a tool alternatives array including the `farmersdelight:tool_action` ingredient with action `knife_dig`, or `#forge:tools/knives`. Native `rice_panicle` cutting yields rice/straw, and native rope uses straw. The 1.3 tool format must not be assumed compatible with older 1.2 examples [S08].

**Implementation outline:** Data only: add `D/recipes/compat/farmersdelight/fiber_from_straw_cutting.json`, gated by `forge:mod_loaded` for `farmersdelight`; mirror the pinned native cutting schema. No compile dependency in production. Keep Plant Fiber bonus drops from double-paying the same knife harvest; consume an existing straw item in this recipe. Add source/use information and an optional tag bridge only after a mass-balance check.

**Acceptance/verification:** Cutting consumes one straw and produces one fiber; wrong tool fails; harvest itself does not receive two newly added bonus payouts. Native FD rice/rope recipes remain unchanged. Test FD 1.3.4, FD absent, and FD with the primitive profile.

**Save-compat/dependencies:** Additive recipe, no block conversion; PF-BUG-007/014 and PF-FEAT-003. Dependency pin and MIT constraints: ledger 001; sources [S08].

### PF-COMPAT-002 — Pam's HarvestCraft 2 Crops

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 4/5, fit 5/5; effort S; risk Medium.

**Verified target:** Pam's HarvestCraft 2 Crops / `pamhc2crops`, Forge / Minecraft 1.20.1, release 1.20-1.0.3, CF file 4687624. **Exact dependency pin:** `curse.maven:pams-harvestcraft-2-crops-361385:4687624` via `https://www.cursemaven.com`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** `D/tags/items/fibers.json` lacks native agricultural fibers. Pam already supplies cotton, flax, jute, kenaf, and sisal, so another five Plant Fiber items would be redundant.

**Design:** Accept the five harvested fibers in Plant Fiber's general raw-fiber recipes. Keep native crop/seed identities and native recipes. Do not make immature crop-block breaking a bonus-fiber source.

**Verified hooks:** The selected release JAR contains `pamhc2crops:cottonitem`, `flaxitem`, `juteitem`, `kenafitem`, `sisalitem`, corresponding `forge:fiber/<plant>`, `forge:crops/<plant>`, and seed tags. The inspected cotton/flax tags name those concrete items. Native `data/pamhc2crops/recipes/string_fiber.json` consumes two `#forge:fiber` for string [S09]. This is cheaper than Plant Fiber's three-to-one string recipe.

**Implementation outline:** Data only: append the five exact optional items to `D/tags/items/fibers.json` with `required:false`. Do **not** append Plant Fiber raw material to `forge:fiber`, because that would silently enable Pam's cheaper string recipe for all Plant Fiber. Add compatibility notes explaining the directional bridge. No crop replacement or source-code dependency.

**Acceptance/verification:** Each harvested fiber works in intended Plant Fiber recipes; every seed/crop remains native. With Pam absent, tags load without errors. Audit compressed-block roundtrips and string outputs so the bridge cannot recreate more native crop material.

**Save-compat/dependencies:** No ID changes; PF-BUG-007. Exact CurseMaven coordinate and ARR constraint: ledger 002. Use the 1.20.1-listed release JAR; the public older 1.18.2 source branch is not sufficient evidence [S09].

### PF-COMPAT-003 — Serene Seasons

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 3/5, fit 4/5; effort S/M; risk Medium.

**Verified target:** Serene Seasons / `sereneseasons`, Forge / Minecraft 1.20.1, release 9.1.0.3. **Exact dependency pin:** `maven.modrinth:serene-seasons:WMrhrybF`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** No crop exists in 1.2.0; seasonal tags cannot usefully be attached to raw fiber or armor.

**Design:** If cattails ship, make their growth fertile in spring/summer/autumn, with winter handled by the target mod's native rules. Preserve a year-round fallback through consumed plants. A later optional seasonal yield modifier may use the public season API, but growth tags are the first implementation.

**Verified hooks:** Release tags exist in **both block and item registries**: `sereneseasons:spring_crops`, `summer_crops`, `autumn_crops`, `winter_crops`, `year_round_crops`. The public `SeasonHelper.getSeasonState(Level)` and tropical-season helper exist in the 1.20.1 source [S10].

**Implementation outline:** Data only initially: add the new crop block to the three appropriate block tags and its seed to the corresponding item tags under `R/data/sereneseasons/tags/`. No Java dependency is needed. If yield seasonality is later enabled, isolate a soft adapter under `J/compat/sereneseasons/`, query only during harvest, and preserve native tropical rules. Do not add global per-tick season scans.

**Acceptance/verification:** Grow the crop through all seasons in temperate and tropical biomes; verify winter fallback still obtains fiber and immature plants do not produce extra material. Test the higher Forge/GlitchCore requirements in ledger 003.

**Save-compat/dependencies:** Existing crops remain registered in every season. PF-FEAT-012 and PF-FEAT-003; no seasonal feature is needed before a crop exists. Sources [S10].

### PF-COMPAT-004 — Tough As Nails

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 4/5; effort S; risk Medium.

**Verified target:** Tough As Nails / `toughasnails`, Forge / Minecraft 1.20.1, release 9.2.0.171. **Exact dependency pin:** `maven.modrinth:tough-as-nails:kWVOI0iS`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Grass armor has no temperature semantics. TAN already has leaf and wool clothing; avoid replacing its entire clothing progression with cheap grass armor.

**Design:** An optional cooling profile recognizes **only the grass helmet** initially as native cooling armor. Full-set membership remains an explicit pack override. No cold protection, thirst restoration, or raw-water purification is implied.

**Verified hooks:** Release resources include `toughasnails:cooling_armor` (leaf armor) and `toughasnails:heating_armor` (wool armor) item tags [S11]. These are categorical hooks, not per-piece numeric resistance settings.

**Implementation outline:** Data only: optional profile adds `plantfiber:grass_helmet` to `R/data/toughasnails/tags/items/cooling_armor.json`; do not add it to heating armor. Explain overlap with TAN leaf armor and the profile's limited role. No compile dependency. A default-on full-set claim of “slightly weaker cooling” would be unsupported by this tag mechanism.

**Acceptance/verification:** Compare temperature behavior wearing no hat, grass hat, and native leaf hat in identical conditions. The profile should apply exactly the target's categorical effect; if this trivializes leaf gear, keep the profile opt-in. Validate Forge ≥47.3.0 and GlitchCore before gameplay tests.

**Save-compat/dependencies:** Tag-only change, no equipment removal. PF-BUG-006 and PF-INC-001. Ledger 004 and [S11].

### PF-COMPAT-005 — Cold Sweat

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 4/5, fit 4/5; effort S; risk Medium.

**Verified target:** Cold Sweat / `cold_sweat`, Forge / Minecraft 1.20.1, release 2.4.2. **Exact dependency pin:** `maven.modrinth:cold-sweat:arQLKRjC`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Four grass armor items have no native Cold Sweat insulation data.

**Design:** Optional light hot-weather protection: each piece has proposed static insulation `cold:0`, `heat:0.5`. Cloth sewing material is a later separate balance choice, not a second bonus automatically stacked onto the same armor.

**Verified hooks:** Release data uses `data/<namespace>/cold_sweat/item/insulator/<name>.json` (the native namespace produces a repeated `cold_sweat/cold_sweat` path). The codec has `type` (slot), `item`, `insulation`, and native `required_mods`; StaticInsulation's codec has `cold` and `heat` fields. Native armor examples use `type:"armor"` and item selectors [S12].

**Implementation outline:** Data only: create four files at `D/cold_sweat/item/insulator/grass_<piece>.json` with the verified item selector and native schema, gated using Cold Sweat's own supported requirements/profile mechanism. Do not use a guessed generic Forge condition in this custom loader. No direct dependency or copied API implementation.

**Acceptance/verification:** Reload logs report no codec errors; native tooltips/temperature calculations recognize each piece, and total effect matches four modest pieces without double counting. Compare naked, grass, and native specialist clothing. Confirm removing the profile removes only the extra insulation.

**Save-compat/dependencies:** No stack migration; PF-BUG-006 and PF-INC-001. Ledger 005; licensing metadata discrepancy is recorded, not resolved by assumption. Sources [S12].

### PF-COMPAT-006 — Legendary Survival Overhaul

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 4/5; effort S; risk Medium.

**Verified target:** Legendary Survival Overhaul / `legendarysurvivaloverhaul`, Forge / Minecraft 1.20.1, release 1.20.1-2.4.7. **Exact dependency pin:** `maven.modrinth:legendary-survival-overhaul:BsBOQNzX`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Grass armor and regeneration dressings do not explicitly integrate with LSO temperature or injury systems.

**Design:** First ship temperature data: proposed `heat_resistance:0.5`, `cold_resistance:0`, `thermal_resistance:0`, `temperature:0` per grass armor piece, opt-in if multiple temperature mods coexist. Do not advertise limb-healing support from vanilla regeneration.

**Verified hooks:** The inspected release branch contains item files such as `data/minecraft/legendarysurvivaloverhaul/temperature/items/leather_chestplate.json`, with those four fields. Its `TemperatureDataManager` provides item lookup, but data files are sufficient [S13].

**Implementation outline:** Create `D/legendarysurvivaloverhaul/temperature/items/grass_<piece>.json` for all four pieces, preserving item namespace/path mapping. No Java or compile dependency. Keep LSO injury support out of this first task. **UNVERIFIED —** a stable 2.4.7 public API to route Plant Fiber items into LSO limb healing was not established; a future adapter must prove it before implementation.

**Acceptance/verification:** LSO reports the intended small heat resistance, zero cold resistance, and no unsolicited body-temperature offset. Test all four pieces, disabled armor, and simultaneous temperature mods with only one selected profile.

**Save-compat/dependencies:** Data-only; PF-BUG-006/PF-INC-001. Ledger 006 and [S13], specifically the Alex-Hashtag 1.20.1 source, not an assumed older fork.

### PF-COMPAT-007 — Thirst Was Taken

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P3. **Score:** value 2/5, fit 4/5; effort S; risk Medium.

**Verified target:** Thirst Was Taken / `thirst`, Forge / Minecraft 1.20.1, release 1.20.1-1.4.0. **Exact dependency pin:** `maven.modrinth:thirst-was-taken:v0d2UK5F`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Plant mesh resembles filtering media but has no hydration integration. Thirst already owns water purity, containers, and a Create filter.

**Design:** A small optional recipe variant uses one plant mesh instead of sand as the filter medium while retaining the native Create nozzle and fluid tank, yielding one native `thirst:sand_filter`. It is an alternative material route, not a new purifier or a better purity tier.

**Verified hooks:** `data/thirst/recipes/compat/create/sand_filter.json` is a Forge mod-loaded-gated shaped recipe using `#forge:sand`, `create:nozzle`, and `create:fluid_tank`; the output is `thirst:sand_filter` [S14].

**Implementation outline:** Add `D/recipes/compat/thirst/mesh_filter.json`, gated on **both** `thirst` and `create`. Mirror the native recipe with the one intended medium substitution. Do not craft filled canteens or reset water NBT. No compile dependency. Keep the original sand recipe intact, and leave this low-priority recipe opt-in if its player value is too slight.

**Acceptance/verification:** Recipe requires all native machine components and one mesh, produces one working native filter, and changes no purity outcome. With either dependency absent it is omitted cleanly.

**Save-compat/dependencies:** No new container or health data. PF-FEAT-004; ledger 007 plus Create pin 015. **UNVERIFIED —** direct water-purity API/NBT adapter is deliberately out of scope. Sources [S14].

### PF-COMPAT-008 — Diet coexistence, without invented food content

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2 check. **Score:** value 2/5, fit 3/5; effort S; risk Low.

**Verified target:** Diet / `diet`, Forge / Minecraft 1.20.1, release 2.1.1+1.20.1. **Exact dependency pin:** `maven.modrinth:diet:oHxMouff`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Fiber, bandages, plaster, mesh, and rope are not foods; no food properties are registered in `J/registry/ModItems.java`.

**Design:** Validate coexistence only. Dressings heal through existing regeneration, without granting hunger, saturation, or nutrition. A dedicated Diet nutrient group for fiber would have no player-facing justification.

**Verified hooks:** Diet 2.1.1 exists for this target; its native group data lives under `data/diet/diet/groups/`, with examples such as vegetables and numeric gain/decay/default fields [S15]. These hooks are real but unnecessary for non-food items.

**Implementation outline:** No production files or dependency required. Add an integration test scenario and a short compatibility note only if useful. Do not add `diet` data for dressings or mark raw fiber as a vegetable. If an actual food is ever designed, conduct a separate recipe/nutrition review.

**Acceptance/verification:** Use bandage/plaster while Diet tracks a controlled player: health changes according to the effect; food level, saturation, and nutrient groups do not receive an item-consumption food gain.

**Save-compat/dependencies:** None. PF-FEAT-008; ledger 008/[S15]. This row is a researched coexistence decision, not a surviving content feature counted toward expansion breadth.

### PF-COMPAT-009 — No Tree Punching primitive profile

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 5/5, fit 5/5; effort M; risk High.

**Verified target:** No Tree Punching / `notreepunching`, Forge / Minecraft 1.20.1, release 7.1.0. **Exact dependency pin:** `maven.modrinth:no-tree-punching:aPNOyI3O`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Plant Fiber can create vanilla string directly, while NTP provides its own plant fiber and plant string progression.

**Design:** Unify raw-material inputs through `plantfiber:fibers`, but preserve NTP's intended route from plant string to vanilla string. The explicit primitive profile disables Plant Fiber's direct vanilla-string recipe and adds three equivalent raw fibers → one `notreepunching:plant_string`. Native kindling can accept Plant Fiber. Do not auto-enable this profile merely on detecting NTP.

**Verified hooks:** Native `plant_string.json` uses three `notreepunching:plant_fiber`; a separate `string_from_plant_string` smelting recipe exists. Native `notreepunching:fire_starter_kindling` accepts plant fiber and string families. Its own `notreepunching:string` tag is used in resources [S16].

**Implementation outline:** Append optional native plant fiber to `D/tags/items/fibers.json`; add optional Plant Fiber membership to `R/data/notreepunching/tags/items/fire_starter_kindling.json`. Create a profile-specific replacement for `plantfiber:string_from_plant_fiber` and a gated native-plant-string recipe in `D/recipes/compat/notreepunching/`. Keep raw Plant Fiber out of `forge:string`. No Java dependency.

**Acceptance/verification:** Fresh primitive-profile player can obtain fiber, kindle fire, make plant string, and reach vanilla string through NTP's intended processing. No alternate Plant Fiber recipe bypasses that gate; native and Plant Fiber fibers cannot convert into extra units. Run the baseline without NTP to ensure vanilla string remains available there.

**Save-compat/dependencies:** Existing materials retained; profile changes recipes only. PF-BUG-007/014 and PF-FEAT-013. Ledger 009/[S16].

### PF-COMPAT-010 — TerraFirmaCraft experimental profile

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P3. **Score:** value 3/5, fit 3/5; effort L; risk High.

**Verified target:** TerraFirmaCraft / `tfc`, Forge / Minecraft 1.20.1, release 3.2.25. **Exact dependency pin:** `maven.modrinth:terrafirmacraft:F35Y7gTr`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** A cheap vanilla-style spawn bed, string route, and healing items can undermine a total conversion's material/health progression. A generic tag bridge is insufficient.

**Design:** Opt-in TFC profile only. Accept processed `tfc:jute_fiber` for selected Plant Fiber sinks; do not turn raw Plant Fiber into TFC processed jute or cloth. Gate Plant Fiber bed/cloth/armor and healing recipes behind deliberately selected native stages, retaining TFC's own thatch/cloth role. Default TFC coexistence should not claim a supported survival progression until this profile is tested.

**Verified hooks:** Native `tfc:barrel_sealed` jute recipe consumes `tfc:jute`, 200 mB water, and 8,000 ticks to produce `tfc:jute_fiber`. Native `tfc:loom` burlap recipe consumes 12 jute fiber with 12 required steps and a progress texture. Those exact 1.20.x resources were inspected [S17].

**Implementation outline:** Data/profile first: optional native processed-fiber entry, replace vanilla-ingredient PF recipes only in the TFC profile, and add barrel/loom recipes only when the native processing cost is preserved. Initially reuse native processed outputs instead of adding competing TFC cloth. Disable broad bonus drops on immature/native crops in this profile. No new hard dependency; the testing instance includes TFC's own required Patchouli. **UNVERIFIED —** exact bed eligibility and native healing substitutions require a complete TFC gameplay pass; do not invent them from vanilla analogues.

**Acceptance/verification:** Fresh TFC world reaches every advertised PF item through real native ingredients; no wood/metal/cloth/sleeping stage can be skipped using PF. Test barrel fluid accounting, loom quantity, crop maturity, death/respawn, and native nutrition/injuries.

**Save-compat/dependencies:** Preserve all PF and TFC IDs; profile opt-in is an economy change, not save conversion. PF-FEAT-013/005 and bed fixes. Effort L/high risk, ledger 010/[S17].

### PF-COMPAT-011 — Primal Winter fallback sources

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 4/5; effort S; risk Medium.

**Verified target:** Primal Winter / `primalwinter`, Forge / Minecraft 1.20.1, release 5.0.0. **Exact dependency pin:** `maven.modrinth:primal-winter:Q6znQM53`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Snow-covered worlds can reduce convenient access to the wild plants in `D/tags/blocks/vegetation.json`.

**Design:** Use consumed leaves/dead plant material as a winter-friendly route; keep glass-bottle wetting and covered drying usable. Do not grant cold immunity merely because the fiber is plant-based.

**Verified hooks:** Primal Winter 5.0.0 has a `primalwinter:replaceable_with_snowy_stuff` **block tag for terrain conversion**, containing dirt/sand/ice and native snowy terrain. It is not a fiber-source tag [S18].

**Implementation outline:** Data-only profile adjusts `plantfiber:processable_plants` and guidance; no edits to the terrain conversion tag. Validate naturally accessible leaf/dead-plant sources before selecting membership. If no credible source exists in a particular preset, document and ship a trader/loot fallback instead of tagging snow or ice as vegetation.

**Acceptance/verification:** In a fresh Primal Winter world, obtain enough fiber for string and one survival item without creative imports; confirm the route consumes finite plant material and does not require a summer-only crop. Test with and without a temperature mod separately.

**Save-compat/dependencies:** No biome or terrain rewrite. PF-FEAT-003/004, ledger 011/[S18].

### PF-COMPAT-012 — Supplementaries flax and distinct ropes

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 4/5, fit 5/5; effort S; risk Medium.

**Verified target:** Supplementaries / `supplementaries`, Forge / Minecraft 1.20.1, release 1.20-3.1.43-forge. **Exact dependency pin:** `maven.modrinth:supplementaries:S0TIJ1hU`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Both mods provide rope; Supplementaries also supplies flax.

**Design:** Accept native flax as a Plant Fiber input while preserving both rope blocks' mechanics. Let players choose the output they need; do not replace all ropes via registry remapping or unrestricted reciprocal recipes.

**Verified hooks:** Release JAR includes `forge:fiber/flax` and `forge:crops/flax` with `supplementaries:flax`. Native rope uses three crop-tag flax vertically for three `supplementaries:rope`; native recipe conditions include Supplementaries feature flags [S19].

**Implementation outline:** Append optional `supplementaries:flax` (or the verified optional fiber/flax tag where the pack accepts all its members) to `D/tags/items/fibers.json`. No Java. If adding a new native-rope recipe later, respect native rope/flax flags as well as mod presence after verifying the target serializer; do not bypass those flags with an unconditional recipe. Do not add generic Plant Fiber to the crop-specific flax tag.

**Acceptance/verification:** Native flax works in PF sinks, both rope recipes remain understandable, and no conversion cycle generates string/fiber. Test the exact Supplementaries release with its Moonlight requirement and selected Create 6 if present; Create 0.5 does not satisfy this release's optional range.

**Save-compat/dependencies:** Tag-only, no rope block migration. PF-BUG-007; ledger 012/[S19].

### PF-COMPAT-013 — Immersive Engineering hemp inputs

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 5/5; effort S; risk Medium.

**Verified target:** Immersive Engineering / `immersiveengineering`, Forge / Minecraft 1.20.1, release 10.2.0-183. **Exact dependency pin:** `maven.modrinth:immersiveengineering:WUzj4tgJ`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** IE already supplies processed hemp fiber and hemp fabric. Its technical material tags have broader consequences than ordinary crafting alternatives.

**Design:** Allow native hemp fiber in PF raw-fiber sinks and native hemp fabric in PF cloth sinks, directionally. Preserve IE's crop and machine progression; generic grass fiber/cloth must not become industrial hemp material.

**Verified hooks:** In this release the exact tags are **`forge:fiber_hemp` and `forge:fabric_hemp`**, containing `immersiveengineering:hemp_fiber` and `hemp_fabric`. They are not the newer-looking `forge:fiber/hemp` naming. Native hemp fabric consumes eight fiber-tag ingredients around a wooden rod [S20].

**Implementation outline:** Optional tag entries in `D/tags/items/fibers.json` and future `woven_cloth.json`. No Java or new machine recipes initially. A press/pulp machine recipe is deferred until the exact selected IE serializer is inspected; no guessed metal-press or blueprint JSON.

**Acceptance/verification:** Native processed materials craft PF outputs; PF materials do not suddenly satisfy IE technical recipes. No reversible recipe returns native hemp from cheaper grass cloth. Launch using a Forge ≥47.3 test instance.

**Save-compat/dependencies:** No native ID changes. PF-BUG-007/PF-FEAT-005; ledger 013/[S20].

### PF-COMPAT-014 — Comforts camping recipes

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 3/5, fit 4/5; effort S; risk Medium.

**Verified target:** Comforts / `comforts`, Forge / Minecraft 1.20.1, release 6.4.0+1.20.1. **Exact dependency pin:** `maven.modrinth:comforts:gBDsc134`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** PF grass bed sets spawn; Comforts' sleeping bags/hammocks serve portable/non-spawn sleep roles. Keep those distinctions.

**Design:** After cloth exists, offer a native white hammock recipe using one PF cloth in the central wool slot, retaining native string and wooden-rod costs. At 16 raw-fiber equivalents per cloth this is a material alternative with a measurable cost, not free wool. Do not add a competing PF sleeping bag.

**Verified hooks:** The Forge hammock recipes use `#forge:string`, `#forge:rods/wooden`, and a color-specific wool center in the `sws/sas/sws` shape. Native content retains its own sleep semantics [S21].

**Implementation outline:** Add `D/recipes/compat/comforts/white_hammock_from_cloth.json`, gated on `comforts`, mirroring the exact white output ID from the selected native recipe before implementation. Use the PF cloth tag only in this alternate recipe, not blanket wool membership. No Java dependency or respawn interception.

**Acceptance/verification:** Recipe consumes one cloth, four string-tag items, and four wooden rods for one native hammock. Sleep in PF bed then hammock: native Comforts behavior must preserve the bed's intended spawn point. Test recipe absence without Comforts.

**Save-compat/dependencies:** No sleep-data migration. PF-FEAT-005 and bed fixes. **UNVERIFIED —** re-read exact white hammock result ID before writing the new JSON; do not infer its spelling. Ledger 014/[S21].

### PF-COMPAT-015 — Create 6 bulk processing

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 5/5, fit 5/5; effort S/M; risk Medium.

**Verified target:** Create / `create`, Forge / Minecraft 1.20.1, release mc1.20.1-6.0.8. **Exact dependency pin:** `maven.modrinth:create:8amzvn9x`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Mesh currently requires a water bucket in each crafting operation. Create can supply fluid processing without a new PF machine system.

**Design:** Basin compacting consumes three fibers and 250 mB water for one mesh. The material cost matches the hand recipe; fluid handling is the automation benefit. A second cloth recipe may automate the exact four-reinforced-to-one ratio, with no increased output. Do not overwrite Create's existing wheat milling and remove its flour results.

**Verified hooks:** Selected Create 6.0.8 resources include `create:compacting` with item ingredients and fluid objects using `fluid`, `amount`, and `nbt`, plus result arrays. Native `create:milling` supports `processingTime` and probabilistic results, but is unnecessary for the first mesh recipe [S22].

**Implementation outline:** Data only: `D/recipes/compat/create/mesh_compacting.json`, gated on `create`; mirror the pinned compacting schema with PF fiber tag, 250 mB water, one mesh. Add a cloth compacting recipe only when cloth ships. Keep the test matrix on Create **6.0.8**; do not advertise 0.5 support without separate resources/runtime verification. No compile dependency.

**Acceptance/verification:** A measured 1,000 mB water plus 12 fibers produces exactly four mesh, with no container dupe. Check JEI/EMI display, basin processing, full output storage, and absent Create. Compare complete paper chain throughput against the cheap hand route.

**Save-compat/dependencies:** Additive recipes only. PF-BUG-007, PF-FEAT-004/005 as applicable; ledger 015/[S22].

### PF-COMPAT-016 — JEI information pages

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1. **Score:** value 5/5, fit 5/5; effort S; risk Low.

**Verified target:** JEI / `jei`, Forge / Minecraft 1.20.1, release 15.59.0.210. **Exact dependency pin:** `maven.modrinth:jei:4vbuxppk`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Vanilla crafting/cooking recipes are already discoverable to recipe viewers; non-recipe GLM sources and fence-knot controls are not represented as recipes.

**Design:** Add ingredient information for raw fiber, mesh, healing items, and rope, reflecting the active source/profile/config. Do not create redundant custom categories for ordinary crafting recipes. Add a drying category only after the frame exists and its real recipe representation is established.

**Verified hooks:** `IModPlugin` and `IRecipeRegistration.addIngredientInfo` exist in the selected 1.20.1 API; `VanillaTypes.ITEM_STACK` is available [S23].

**Implementation outline:** Soft client integration `J/compat/jei/PlantFiberJeiPlugin.java`, translations, compile-only deobfuscated dependency from ledger 016, dev runtime separately, optional metadata. Keep references to JEI/client classes out of common class initialization. Use effective config and translated components; rebuild information appropriately when joining a server with different rules.

**Acceptance/verification:** JEI shows accurate source and knot-use info, existing vanilla recipes appear once, and disabled routes are not advertised. PF launches on a dedicated server and client with JEI absent. Test login to two servers with different configs.

**Save-compat/dependencies:** None; client presentation only. PF-FEAT-001/PF-BUG-006; [S23].

### PF-COMPAT-017 — EMI information pages

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 5/5; effort S; risk Medium.

**Verified target:** EMI / `emi`, Forge / Minecraft 1.20.1, release 1.1.24+1.20.1+forge. **Exact dependency pin:** `maven.modrinth:emi:Axuu9I9R`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Same missing non-recipe guidance as JEI; players may use EMI alone or alongside JEI.

**Design:** Equivalent source/use information with stable PF information IDs, avoiding double registration when bridging another viewer.

**Verified hooks:** EMI's `EmiPlugin` and `EmiInfoRecipe` exist in its 1.20 source. The repository signatures use Yarn `Text`/`Identifier`; these names must not be pasted into this Mojmap Forge project [S24].

**Implementation outline:** Soft client adapter `J/compat/emi/PlantFiberEmiPlugin.java` using the remapped Forge artifact, shared translated description data, compile-only/dev-runtime ledger 017, optional metadata. **UNVERIFIED —** confirm the exact Forge plugin entrypoint and remapped constructor signatures after dependency resolution. No custom category is required for vanilla crafting.

**Acceptance/verification:** Test EMI only, JEI only, both, and neither. Each item has one appropriate information route, no duplicate or invalid recipes, and no common-side classloading error.

**Save-compat/dependencies:** None. PF-FEAT-001/PF-BUG-006; sources [S24].

### PF-COMPAT-018 — Jade state information

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 4/5, fit 5/5; effort S/M; risk Low.

**Verified target:** Jade / `jade`, Forge / Minecraft 1.20.1, release 11.13.3+forge. **Exact dependency pin:** `maven.modrinth:jade:xJQHCmWJ`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** Rope state and future drying progress are hard to inspect without debug state. `RopeBlock` already exposes facing, fence attachment, and waterlogging.

**Design:** Show attachment kind and waterlogging from the targeted rope's state, plus enabled/disabled use guidance for a bed. Once the frame exists, show contents and coarse progress. Do not scan an entire rope column on every overlay update.

**Verified hooks:** Forge 1.20 Jade has `IWailaPlugin.register`/`registerClient` and block component providers [S25].

**Implementation outline:** Isolate `J/compat/jade/PlantFiberJadePlugin.java` and block component provider. Initial state-only display needs no custom server payload; frame progress may use Jade's verified server-data interface once read from the selected API. Use existing BE sync when sufficient. Compile-only/dev-runtime ledger 018 and optional metadata; no hard Jade dependency.

**Acceptance/verification:** Overlay is accurate without measurable long-column cost. Test client-only Jade against a server without Jade for state-only features, and both sides for any later server-data provider. PF-only dedicated startup remains clean.

**Save-compat/dependencies:** No new persisted data beyond frame. PF-BUG-006/009; PF-FEAT-004 for progress. **UNVERIFIED —** exact frame server-data provider signature must be checked before adding that extension. Sources [S25].

### PF-COMPAT-019 — Curios spool equipment

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P3. **Score:** value 2/5, fit 3/5; effort M; risk Medium.

**Verified target:** Curios API / `curios`, Forge / Minecraft 1.20.1, release 5.14.1+1.20.1. **Exact dependency pin:** `maven.modrinth:curios:IPQlZkz1`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** There is no existing PF trinket, so adding Curios now would create integration code with no meaningful item.

**Design:** After the spool exists, let a pack-provided belt slot hold it. Do not grant new slots automatically or make all grass armor extra-slot armor. The initial goal is convenient equipment, not passive bonuses.

**Verified hooks:** `ICurioItem` exists; the native `data/curios/curios/slots/belt.json` defines an icon/order and uses the `curios:tag` validator [S26].

**Implementation outline:** Prefer a data-only spool tag for the existing belt validator after verifying the exact 5.14.1 item-tag mapping. If using the equipped spool directly during rope placement, add a soft adapter under `J/compat/curios/` and query it only on player action; do not scan Curios each tick. Compile dependency is none for tags, ledger 019 for code. Add optional metadata only for the adapter.

**Acceptance/verification:** Spool equips only in a supplied valid slot, does not add armor attributes, and preserves its count through equip/death/reconnect. Players without Curios retain full hand-held spool functionality.

**Save-compat/dependencies:** Preserve spool NBT. PF-FEAT-006. **UNVERIFIED —** exact belt item-tag lookup and equipped-item API signature must be read before implementation. Sources [S26].

### PF-COMPAT-020 — Patchouli field notes

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P2. **Score:** value 3/5, fit 5/5; effort S/M; risk Low.

**Verified target:** Patchouli / `patchouli`, Forge / Minecraft 1.20.1, release 1.20.1-85-forge. **Exact dependency pin:** `maven.modrinth:patchouli:94dtOLgZ`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** README and external mod-page text are the only comprehensive guide. An optional native book can explain the extended processing chain.

**Design:** PROPOSED book `plantfiber:field_notes`, covering source rules, mesh/drying, cloth uses, rope knots/spool, and healing. Core advancements/tooltips remain available without Patchouli.

**Verified hooks:** Patchouli's 1.20.1 test book places the **book definition under `data/.../patchouli_books/.../book.json`**, with `use_resource_pack:true`; localized categories/entries are under `assets/.../patchouli_books/.../en_us/`. Do not move every book file into one tree. `PatchouliAPI.get().getBookStack(ResourceLocation)` is a verified public factory, with book loading timing documented by its API [S27].

**Implementation outline:** Prefer data/resources: `D/patchouli_books/field_notes/book.json`; `A/patchouli_books/field_notes/en_us/categories/` and `entries/`; use native recipe/text pages. Add a gated survival recipe for the native book stack only after verifying its item/NBT recipe encoding. **UNVERIFIED —** exact native recipe-result NBT encoding has not been checked; if a soft factory adapter is chosen instead, use the verified factory after books load, under `J/compat/patchouli/`, with ledger 020 dependency/optional metadata. Do not introduce a hard dependency merely to hand out a book.

**Acceptance/verification:** Book opens in survival, all recipe links resolve, no unimplemented/disabled recipe is promised, language fallback works, and PF loads without Patchouli. Verify resource reload and dedicated-server book distribution.

**Save-compat/dependencies:** Additive book ID, original text/art. PF-FEAT-001/004/005; ledger 020/[S27].

### PF-COMPAT-021 — First Aid native dressings

**Severity:** N/A (enhancement). **Confidence:** NEEDS IN-GAME TEST. **Priority:** P1 after cloth. **Score:** value 4/5, fit 5/5; effort S; risk Medium.

**Verified target:** First Aid / `firstaid`, Forge / Minecraft 1.20.1, release 1.20.1-1.1. **Exact dependency pin:** `maven.modrinth:firstaid:JzYydXeE`. Data-only tasks have no production compile dependency; soft code uses this pin for compile-only/development runtime.

**Current vs. expected / location:** PF bandages apply vanilla regeneration; First Aid has native limb-targeting dressings. Two items named bandage need not perform the same healing operation.

**Design:** Once cloth exists, add an alternative recipe for native `firstaid:bandage` using three PF cloth plus the native four string ingredients. Native First Aid owns use time, body-part selection, and healing; PF dressings keep their documented regeneration behavior. This gives cloth a survival sink without duplicating injury logic.

**Verified hooks:** Native `data/firstaid/recipes/bandage.json` uses four vanilla strings and three wool-tag ingredients. `HealingItemApiHelper` exposes a public item-use operation involving `ItemHealing`, but its implementation installation/access details do not justify calling internal fields or subclassing blindly [S28].

**Implementation outline:** Data only: `D/recipes/compat/firstaid/bandage_from_cloth.json`, gated on `firstaid`, same shape/count as native recipe with the wool positions changed to PF cloth. Keep the native string requirement unless separately validating a tag substitution. No compile dependency. Do not intercept PF finish-use to apply both regeneration and native limb healing.

**Acceptance/verification:** Recipe consumes three cloth/four strings for one native bandage; limb UI and treatment behave exactly like a normally crafted native bandage. No double heal or double consume. Test a controlled injured limb, full-health limb, interrupted use, and First Aid absent.

**Save-compat/dependencies:** No native health data or item migration. PF-FEAT-005. This additional survival-focused candidate is stronger than speculative trinket bonuses. Ledger 021/[S28].

## 7. Part C — Execution plan

### 7.1 Operating instructions for the coding agent

This document is the handoff. Work from the repository, preserve unrelated author changes, and re-read the target files before editing. Confirm whether the checkout still matches the reviewed commit. If newer commits already fix a finding, record that evidence and skip redundant changes. The phases below are ordering guidance, **not approval gates**. Do not pause for author answers when a recommended default is supplied.

First establish a working Java 17/Gradle environment and obtain the pinned Minecraft/Forge sources. Use `./gradlew` on Linux/macOS or `.\gradlew.bat` on Windows. The review's DNS failure is not a reason to replace the wrapper or upgrade Minecraft. If the modding skill/scripts become available, read their instructions, read any existing MODMAP hand-maintained sections, run their documented `--help`, and invoke `modmap.py`, `check_mod.py`, and `find_api.py` using their actual supported arguments. Do not invent a script command line from this report.

Use a disposable run world for smoke tests; accept the EULA in that run directory when required, start the server until `Done`, then issue `stop`. Retain logs and inspect warnings. A launch must pass using the built production JAR as well as a development run. Do not test destructive bed/rope cases in the author's original world; use a copy.

**Required phase-end validation bundle:**

1. `./gradlew build` (Windows equivalent above); record task outcome and all relevant warnings.
2. `./gradlew runData`; inspect `git diff -- src/generated` and resource shadows. A no-provider run may legitimately generate no PF resources; do not call that proof of broad data coverage.
3. Run available `check_mod.py` according to its real usage. If still unavailable, record **NOT AVAILABLE**, and perform explicit JSON parsing, registry/resource/reference/tag/recipe checks. Do not report a substitute as a successful script run.
4. `./gradlew runServer`, wait for `Done`, then `stop`; run the phase's exact player-facing tests and the relevant integration absent/present cases.
5. Add a concise player-facing `CHANGELOG.md` entry and apply the phase's suggested version bump through the single source of version metadata. A phase can be combined with another release; do not ship a dependency-incomplete subset merely to match a number.

### Phase 0 — Build and metadata baseline

**Tasks:** Restore dependency access in the execution environment; reproduce build/runData/server baseline; inspect decompiled 1.20.1 APIs required by the high-risk tasks; complete PF-INC-004. Inspect packaged metadata and reobfuscation without assuming the current JAR is broken. Record missing runtime tooling separately. Add `--nogui` to the server run profile only if needed and using the verified MDK pattern.

**Result:** A reproducible baseline and one version source. **Suggested version:** prepare 1.2.1; do not release while Critical/High tasks remain unresolved. **Changelog text:** “Standardized release metadata and verified the Minecraft 1.20.1 Forge build.” Only claim verification after it passes.

**End of phase:** `gradlew build` → `gradlew runData` plus generated diff → `check_mod.py` or explicit unavailable record → `gradlew runServer` to `Done`, stop → changelog/version update.

### Phase 1 — Critical/High correctness

**Tasks:** PF-BUG-001 through PF-BUG-004. Include the minimum PF-BUG-006 effective-config work needed to fix armor correctly. Implement PF-BUG-010 with the placement transaction rather than postponing item accounting. Bed paired-state work may include PF-BUG-005 to avoid rewriting the same helper twice.

**Result:** Reliable item registration, bed recovery/safe respawn, and authorized rope placement. Reproduce or rule out each LIKELY finding; update confidence from actual evidence. **Suggested version:** 1.2.1. **Changelog text:** “Fixed grass-bed recovery and respawn safety, corrected armor configuration loading, and made knot rope placement respect placement restrictions.” Omit any claim ruled out by tests.

**End of phase:** `gradlew build` → `gradlew runData`/diff → `check_mod.py` → `gradlew runServer` to `Done`, stop; complete bed/permission/armor tests on copied 1.2.0 save → changelog/version update.

### Phase 2 — Medium/Low issues and documented behavior

**Tasks:** Remaining PF-BUG-005–015 and PF-INC-001–006, accounting for already completed overlapping work. Prioritize tag use before recipe-cost changes; config validation before new conditions; support policy and NBT validation before rope transforms. Add recipe-book unlocks and update all user-facing descriptions. Keep subjective palette work outside this maintenance phase.

**Result:** Consistent toggles, synchronized behavior, usable material tags, safe rope persistence, coherent starter costs, and discoverable recipes. **Suggested version:** 1.2.2; if the author treats recipe rebalance as a feature release, combine with 1.3.0. **Changelog text:** “Added recipe-book unlocks, improved material-tag support and starter armor costs, and corrected rope/config edge cases.” Explicitly note stricter unknown-condition validation and flower-source changes.

**End of phase:** `gradlew build` → `gradlew runData`/diff → `check_mod.py` → `gradlew runServer` to `Done`, stop; run flag matrix, recipe unlock, NBT and structure tests → changelog/version update.

### Phase 3 — Quick survival wins

**Tasks:** PF-FEAT-001, 003, 008, and 011. Start PF-FEAT-013 with documentation and explicit profile structure; diagnostics can wait for its API verification. PF-FEAT-002 may enter here if the default source policy cannot be expressed cleanly with tags alone. Keep the first release small enough to evaluate early-game pacing.

**Result:** Clear guidance, deterministic consumed-plant sources, configurable dressing behavior, and surplus-fiber sinks. **Suggested version:** 1.3.0. **Changelog text:** “Added plant-processing recipes, progression guidance, and practical uses for surplus fiber.” Document changed default sources and unchanged optional behavior precisely.

**End of phase:** `gradlew build` → `gradlew runData`/diff → `check_mod.py` → `gradlew runServer` to `Done`, stop; fresh-world first-fiber/string/bed/armor playthrough plus healing test → changelog/version update.

### Phase 4 — Small integrations and explicit profiles

**Tasks:** Ship independent existing-material integrations first: PF-COMPAT-001/002/009/012, then 004/005/006/011 and JEI/EMI/Jade state information as worthwhile. Run Diet coexistence check. Use a separate verified Forge 47.3+ test instance for targets imposing that floor. Finish primitive-profile rules before calling NTP supported.

**Dependency rule:** Cloth/frame/spool-dependent tasks (013 cloth portion, 014, 015 processing portion, 019, 020 full guide, 021) execute after their feature prerequisites in Phase 5. They are deliberately deferred, not blockers for independent Phase 4 integrations. Serene Seasons crop tags and TFC's experimental profile also wait for their prerequisites.

**Result:** Targeted, optional compatibility with documented versions and no hard dependencies. **Suggested version:** 1.4.0. **Changelog text:** “Added optional native-material and survival-mod compatibility profiles.” Name only combinations actually tested.

**End of phase:** `gradlew build` → `gradlew runData`/diff → `check_mod.py` → `gradlew runServer` to `Done`, stop; each integration alone/absent plus supported combined profiles → changelog/version update.

### Phase 5 — Processing, cloth, rope equipment, then optional larger systems

**Tasks:** PF-FEAT-004/005/006 in dependency order, then 007 and 009. Complete deferred cloth/automation/guide integrations. Add PF-FEAT-012 and PF-COMPAT-003 only if crop playtests show a gap that native farming mods do not fill. PF-COMPAT-010 remains an explicitly experimental TFC profile until its full progression test passes. Finish diagnostics if helpful to the now larger configuration surface.

**Result:** A complete fiber→mesh/paper and fiber→reinforcement→cloth/rope equipment chain, with reliable persistence and useful native-mod sinks. **Suggested version:** 1.5.0 for the processing/spool release; later 1.6.0 for an optional crop/TFC expansion if shipped separately. **Changelog text:** “Added slow mesh drying, woven cloth, and rope storage/retrieval, with optional native crafting integrations.” Only list systems actually delivered.

**End of phase:** `gradlew build` → `gradlew runData`/diff → `check_mod.py` → `gradlew runServer` to `Done`, stop; inventory/progress save/reload, multiplayer races, material-conservation and optional-mod tests → changelog/version update.

### 7.2 Focused regression and compatibility matrix

| Scenario | Exact test / expected invariant |
|---|---|
| Baseline dedicated server | Java 17, pinned Forge, PF alone; reach `Done`, connect two clients, stop cleanly; no common-side client class load |
| Packaged JAR | Install `build/libs/plantfiber-<version>.jar` in disposable production Forge server/client; verify matching packaged metadata and functioning recipes/rendering |
| Armor lifecycle | Fresh config, multipliers 0.5 and 1.0, old damaged stacks, server/client mismatch; no startup read error or silent repair |
| Bed | N/E/S/W, head/foot break, survival/creative, wake/cancel, explosion, blocked/hazardous respawn, disabled flags, old save |
| Rope permissions/accounting | Normal item and entity interaction, protected target at bottom, border/height limits, cancelled block/entity events, one remaining item, creative |
| Rope support | All break/drop-flag combinations, independent upper anchor, knot/fence removal, mixed facing columns, support cascades |
| Knot network/persistence | Two clients watch newly placed N/E/S/W knots; leave tracking range and return; late-join; unload chunk; restart; test missing/vertical ExitDir; extend afterward |
| Transform | Structure save with entities; all quarter rotations and both mirror modes; attachments survive and extend correctly |
| Water | Waterlogging true/false, placement in water, bucket insertion, plant replacement false; define and verify whether disabled waterlogging displaces water or rejects placement |
| Loot context | Chance 0/1; hand/shears/other tool; all source families; top/bottom of double plants; water/piston/explosion; fixed count per intended harvest |
| Recipes | 15 existing recipes, all flags, limited crafting unlocks, bucket remainder, optional ingredients absent/present, no output duplication |
| Healing | Each dressing alone/alternating, interrupted use, full health, stronger effect, creative, First Aid/LSO presence; no assumed native limb treatment |
| Performance | Long rope column interaction and collapse; dense knot area; no unbounded tick scan or full-world config migration; profile only if a measurable problem appears |
| Integration combinations | PF alone; each target separately; FD+Pam+Supplementaries fiber tags; NTP primitive; Create 6+Supplementaries; one temperature profile at a time; TFC experimental separately |

For uncertain loot rates, first use chance 1.0 to identify the number of loot evaluations per physical harvest. Only then measure a stochastic 0.25 sample. Never call two fibers from a double plant a confirmed dupe until the exact harvest/loot behavior is observed and the intended policy is written down.

## 8. Open questions for the author, with execution defaults

| Question | Recommended default so implementation can proceed |
|---|---|
| Is the author checkout newer than the reviewed public commit? | Compare HEAD and changed files first; preserve newer fixes and rebase task evidence. Do not overwrite unpublished work. |
| Keep singular mod ID despite plural project name? | Yes. Preserve `plantfiber` and every existing registry ID. |
| Should starter armor remain expensive? | Use 24 raw fibers total, leather defense, current nominal half durability; supply old-cost datapack recipe overrides if desired. |
| Should a repeatedly replanted flower produce free bonus fiber? | No by default; keep renewability through growth or consumed-plant processing. Offer a documented legacy source profile if requested. |
| Should disabling a feature remove old content? | No. Gate new crafting/use consistently, preserve old stacks/blocks, keep safe respawn fallback, and do not forcibly strip worn armor. |
| COMMON vs SERVER config? | Retain existing COMMON keys and early recipe reads, add a server-authoritative effective snapshot. A later SERVER migration must explicitly solve recipe-load timing and old-config migration. |
| One-click fence rope or current two-click behavior? | Keep two clicks and one rope per knot plus one per segment; fix documentation/accounting first. A one-click redesign would need atomic two-item placement. |
| Can disabled waterlogging replace water? | Prefer reject placement into water when waterlogging is disabled, avoiding silent fluid displacement; treat this as a documented behavior decision and test it. |
| Should multiple temperature mods stack effects? | Enable one explicit temperature profile at a time by default; do not silently apply all cooling integrations together. |
| Is raw fiber universally equivalent to string, wool, flax, or hemp? | No. Accept native materials into PF's own semantic tags directionally. Preserve native specialty tags and processing gates. |
| Introduce a dedicated loom now? | No. Start with a small drying frame and a hand-woven cloth recipe; use native Create/TFC processing where installed. |
| Add another fiber crop? | Only as opt-in later, one plant with one shared fiber output. Prefer native agricultural sources first. |
| Support Create 0.5 and 6 simultaneously? | First support the verified Create 6.0.8 release. Add a separate tested 0.5 profile only if there is demand; selected Supplementaries already requires Create 6 when present. |
| Raise Forge minimum? | Keep baseline pin while fixing core bugs; use an explicit newer test instance for selected mods requiring ≥47.3. Raise published minimum only after the combined matrix and document why. |
| Migrate all resources to datagen? | No immediate migration. Keep hand-written small additions; migrate one whole resource family only when maintenance benefit is demonstrated. |
| Commission texture changes? | Keep current art for correctness releases. Review armor/bed in-game against vanilla palettes, then make a separate artistic decision without registry changes. |
| Should dressings heal First Aid/LSO limbs directly? | No unsupported adapter. Use native First Aid recipe outputs; leave LSO limb support deferred until a verified API and gameplay test exist. |
| Is TFC support a normal automatic integration? | No. Explicit experimental profile with its own full progression test; never auto-rewrite a pack's recipes on mod detection. |

## 9. Appendices

### 9.1 Consolidated UNVERIFIED register

Every entry below is an outstanding verification task, not a factual claim that an API or behavior is absent. Complete it from the pinned runtime/source before implementing the dependent code. No author clarification is required to start the safer/default work.

1. **UNVERIFIED —** Successful compilation, compiler warnings, runData output/currentness, reobfuscated production JAR behavior, and dedicated-server startup. All three Gradle commands stopped at wrapper DNS download.
2. **UNVERIFIED —** Exact Forge 47.2.0 config-load ordering and ArmorItem constructor path. The likely early-read problem is grounded in PF code and Forge's 1.20.1 branch, but needs the pinned decompiled/runtime check.
3. **UNVERIFIED —** Runtime severity of knot placement-protection bypass for a specific claims system. Test vanilla/adventure/spawn restrictions and a deliberately cancelled target placement first; normal item placement is already Forge-wrapped.
4. **UNVERIFIED —** Natural triggering frequency of failed `setBlock`/`addFreshEntity` item loss. The unchecked returns are confirmed; controlled failure tests are required.
5. **UNVERIFIED —** Knot late-tracking/spawn-packet behavior and exact entity rotation/mirror handling. Do not replace the spawn packet or invent a transform hook without the multiplayer/structure tests.
6. **UNVERIFIED —** Exact loot evaluation counts for double plants, cascading crops, water/piston/explosion breaks, shears/silk touch/fortune, and actual default stochastic yield. Run the deterministic matrix before changing source policy.
7. **UNVERIFIED —** Native 1.20.1 bed-tag contents, safe stand-up helper signature on the pinned Forge patch, and any villager HOME point-of-interest integration. Bed tags alone do not establish villager compatibility.
8. **UNVERIFIED —** In-game armor texture fit/palette, recipe ergonomics, campfire payout details, and fixed heart restoration from short regeneration effects. These are not confirmed missing assets or healing defects.
9. **UNVERIFIED —** Exact Forge 1.20.1 networking registration/message handling signatures for the config snapshot; use official 1.20.1 docs and local sources, not newer networking tutorials.
10. **UNVERIFIED —** Drying-frame water-bottle identification/remainder API; confirm ordinary water versus potion NBT and return the empty bottle exactly once.
11. **UNVERIFIED —** Trade event/offer constructor, selected vanilla village chest loot IDs, item fuel hook, source-rule reload registration, and diagnostics command registration signatures. Verify each before coding the proposed optional features.
12. **UNVERIFIED —** Full launch/transitive dependency resolution of every selected integration combination. POM retrieval and release metadata verification are not Gradle/runtime resolution tests.
13. **UNVERIFIED —** A stable LSO 2.4.7 public limb-healing adapter; temperature data is verified, injury support is deferred.
14. **UNVERIFIED —** Direct Thirst water-purity/NBT adapter; only native filter recipe integration is specified.
15. **UNVERIFIED —** Exact TFC stage-appropriate PF bed/healing recipe substitutions and complete survival progression. Barrel/loom formats are verified; automatic general compatibility is not.
16. **UNVERIFIED —** Exact Comforts white hammock result ID must be re-read from the selected native recipe before writing the alternative. The shape and ingredient tags were verified.
17. **UNVERIFIED —** EMI's selected Forge plugin entrypoint/remapped constructor signatures; source Yarn names are not valid evidence for Mojmap names.
18. **UNVERIFIED —** Jade server-data provider signature for future frame progress; the initial state-only integration needs no such payload.
19. **UNVERIFIED —** Curios 5.14.1 belt item-tag validator mapping and equipped-item lookup signature; the slot definition and `ICurioItem` exist.
20. **UNVERIFIED —** Patchouli native guide-book recipe-result NBT encoding. Book definition/content directories and public book-stack factory were verified.
21. **UNVERIFIED —** IE custom machine recipe schemas beyond the inspected crafting recipe. The first integration uses tags only.
22. **UNVERIFIED —** Alternate target releases (Create 0.5, older FD 1.2, newer Minecraft/NeoForge ports) and any optional crop worldgen schema not explicitly researched. The roadmap does not advertise them as supported.
23. **UNVERIFIED —** Interpretation of inconsistent/custom third-party license metadata, especially Cold Sweat's nonstandard TOML version label. Use original data-only integration content and consult the actual license before any copying/distribution.
24. **UNVERIFIED —** The live external Plant Fiber CurseForge page text could not be retrieved reliably. `CURSEFORGE.md` was reviewed as the repository's supplied mod-page text; no claim is made that the published page is identical. Unpublished Windows changes were also inaccessible.

### 9.2 Considered and rejected/deferred ideas

| Idea | Decision and reason |
|---|---|
| Rename namespace to `plantfibers` or standardize by deleting old IDs | Reject: needless save break for cosmetic naming. |
| Make raw fiber `forge:string` or `minecraft:wool` | Reject: collapses the existing three-to-one string step and unrelated wool progression. |
| Put generic raw fiber in Pam's aggregate `forge:fiber` | Reject by default: native two-to-one string recipe changes PF's entire economy. |
| Put grass fiber/cloth in IE hemp technical tags | Reject: processing/technology bypass and semantic mismatch. |
| Universal interchangeable rope blocks or reciprocal conversions | Reject: FD crops, Supplementaries utility, and PF knots have different state and behavior. |
| Five new fiber species with nearly identical items | Reject initially: inventory clutter and duplicate native crop materials; use tags/shared outputs. |
| Hemp/flax worldgen on top of every farming integration | Defer: native mods already fill that space; one optional plant only if needed. |
| Full spinning wheel, loom, retting vat, dryer, and weaving machine at once | Defer: too many stations for a small mod; one useful frame and native automation first. |
| PF sleeping bag alongside Comforts | Reject: duplicates an established portable-sleep role; retain PF bed's spawn role. |
| Permanent temperature immunity from grass armor | Reject: undermines survival mods and native specialist clothing. |
| Add Diet nutrition to bandages/raw fiber | Reject: these items are not foods. |
| Treat mesh as universal instant safe-water purification | Reject: purity/NBT and survival progression belong to the target mod. |
| Automatically route regeneration into every injury mod | Reject: unsupported health semantics and risk of double healing. |
| Hard JEI/Patchouli/Curios dependency | Reject: core survival content works without those APIs. |
| New dedicated Curios trinkets with passive combat bonuses | Defer: poor identity fit; equip a real spool first. |
| Grappling hook, swinging physics, rope bridges, or elevators | Defer: large movement/sync/protection scope; climbing/storage/retrieval provide better near-term value. |
| Sails, boats, tents, backpacks, or a new transport system | Defer: possible cloth sinks, but unrelated large systems and strong overlap with existing mods. |
| Global humidity, rope rot, mildew, or per-block fiber aging | Reject initially: constant persistence/tick burden and potential loss of existing builds. |
| Track every player-placed flower to prevent farming | Reject: expensive state/migration solution to a source-policy problem solved by consumption. |
| Immersive Weathering rope decay | Defer: Forge 1.20.1 release 2.0.5 exists, but exact Forge weathering hooks were not verified; decay also conflicts with build stability [S29]. |
| Add a new village/structure just to distribute fiber | Defer: lightweight chest/trader presence is enough. |
| Automatic TFC recipe replacement on mod detection | Reject: total-conversion progression must be an explicit tested profile. |
| Mandatory 1.21/NeoForge migration during review fixes | Reject: changes the target and invalidates the verified resource/API work. |
| Declare dedicated-server safety solely from static import review | Reject: runServer and production-JAR smoke tests are still required. |

### 9.3 Research sources and reproducibility

All URLs below were used as primary project/docs/release evidence during the review; access date **2026-09-10**. GitHub branch links name the researched Minecraft branch; recorded SHAs identify the snapshot when known. A branch may move after this report. For release-JAR evidence, the listed exact Modrinth version metadata resolves the file download and hashes; inspect the named internal resource paths, not a current default branch. Maven resolution uses the exact ledger coordinates and POM formula in section 6.2.

**[S01] Plant Fiber source and author documentation.** Reviewed commit: https://github.com/otectus/PlantFibers/tree/e9a275cbb633e4f02156d589d6f6399372dd7c73 . README: https://github.com/otectus/PlantFibers/blob/e9a275cbb633e4f02156d589d6f6399372dd7c73/README.md . Mod-page text: https://github.com/otectus/PlantFibers/blob/e9a275cbb633e4f02156d589d6f6399372dd7c73/CURSEFORGE.md . Changelog: https://github.com/otectus/PlantFibers/blob/e9a275cbb633e4f02156d589d6f6399372dd7c73/CHANGELOG.md . Author-linked live page (retrieval not established): https://www.curseforge.com/minecraft/mc-mods/plant-fiber-grass-tier .

**[S02] Forge 1.20.1 configuration.** https://docs.minecraftforge.net/en/1.20.1/misc/config/ — config types, synchronization, load/reload events. Source premature-read behavior and `isLoaded`: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/common/ForgeConfigSpec.java .

**[S03] Forge conditional recipes.** https://docs.minecraftforge.net/en/1.20.1/resources/server/conditional/ — `forge:mod_loaded` and condition composition. These are Forge recipe hooks, not a universal condition system for all custom mod data loaders.

**[S04] Forge tags.** https://docs.minecraftforge.net/en/1.20.1/resources/server/tags/ — optional entries and tag extension behavior.

**[S05] Forge 1.20.1 behavior/API source.** Snapshot branch SHA `3c3f496015632ab84560be77a57cc57ac42c49e1`, not asserted to be the 47.2.0 tag. Bed hooks and native safe stand-up helper: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/common/extensions/IForgeBlock.java . Stack-aware durability: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/common/extensions/IForgeItem.java . Placement snapshot/events: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/common/ForgeHooks.java (`onPlaceItemIntoWorld`, inspected around lines 566–653). Player respawn patch: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/patches/minecraft/net/minecraft/world/entity/player/Player.java.patch . Sleep/wake patch: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/patches/minecraft/net/minecraft/world/entity/LivingEntity.java.patch .

**[S06] ForgeGradle and MDK.** https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/mdk/build.gradle — resource expansion, Java/ForgeGradle patterns, deobfuscated optional development dependencies. https://github.com/MinecraftForge/ForgeGradle/blob/FG_6.0/src/userdev/java/net/minecraftforge/gradle/userdev/UserDevPlugin.java — `assemble` dependency on reobfuscation (inspected around 283–289 and 324–344). Forge 1.20.1 release list: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html .

**[S07] Forge 1.20.1 networking reference for implementation.** https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/ . Exact message-registration signatures still require local pinned-source confirmation; no networking Java snippet is asserted in this report.

**[S08] Farmer's Delight.** Release: https://modrinth.com/mod/farmers-delight/version/SiIpcZzM ; metadata: https://api.modrinth.com/v2/version/SiIpcZzM . Source snapshot `e854d56d4eea8dc120650d43da0d1472711797ad`: https://github.com/vectorwing/FarmersDelight/tree/1.20 . Cutting format: https://github.com/vectorwing/FarmersDelight/blob/1.20/src/generated/resources/data/farmersdelight/recipes/cutting/rice_panicle.json . Native rope: https://github.com/vectorwing/FarmersDelight/blob/1.20/src/generated/resources/data/farmersdelight/recipes/rope.json . Project/license metadata: https://api.modrinth.com/v2/project/farmers-delight .

**[S09] Pam's HarvestCraft 2 Crops.** Exact Forge/1.20.1-listed file: https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-crops/files/4687624 . Project material descriptions: https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-crops . Inspected release JAR: https://www.cursemaven.com/curse/maven/pams-harvestcraft-2-crops-361385/4687624/pams-harvestcraft-2-crops-361385-4687624.jar . Internal evidence: `META-INF/mods.toml`, `data/forge/tags/items/fiber/cotton.json`, `fiber/flax.json`, corresponding jute/kenaf/sisal tag/model paths, and `data/pamhc2crops/recipes/string_fiber.json`. Public older source: https://github.com/MatrexsVigil/phc2crops — not treated as authoritative for unverified 1.20.1 Java APIs.

**[S10] Serene Seasons.** Release: https://modrinth.com/mod/serene-seasons/version/WMrhrybF ; metadata: https://api.modrinth.com/v2/version/WMrhrybF . Source snapshot `61df18c2029d50e1294e7fc8d61824bbd6facb75`: https://github.com/Glitchfiend/SereneSeasons/tree/1.20.1 . API: https://github.com/Glitchfiend/SereneSeasons/blob/1.20.1/common/src/main/java/sereneseasons/api/season/SeasonHelper.java . Release-JAR evidence: `META-INF/mods.toml` and `data/sereneseasons/tags/{blocks,items}/{spring_crops,summer_crops,autumn_crops,winter_crops,year_round_crops}.json`. License metadata: https://api.modrinth.com/v2/project/serene-seasons .

**[S11] Tough As Nails.** Release: https://modrinth.com/mod/tough-as-nails/version/kWVOI0iS ; metadata: https://api.modrinth.com/v2/version/kWVOI0iS . Project source: https://github.com/Glitchfiend/ToughAsNails . Inspected exact release: `META-INF/mods.toml`, `data/toughasnails/tags/items/cooling_armor.json`, `heating_armor.json`, and native water-purifying recipe resources. License metadata: https://api.modrinth.com/v2/project/tough-as-nails . Only armor tags are specified for implementation.

**[S12] Cold Sweat.** Release: https://modrinth.com/mod/cold-sweat/version/arQLKRjC ; metadata: https://api.modrinth.com/v2/version/arQLKRjC . Source snapshot `c985aa5778f6f601d19bdbe1b5807f1506c97316`: https://github.com/Momo-Softworks/Cold-Sweat/tree/1.20.1-FG . Codec: https://github.com/Momo-Softworks/Cold-Sweat/blob/1.20.1-FG/src/main/java/com/momosoftworks/coldsweat/data/codec/configuration/InsulatorData.java . Numeric insulation: https://github.com/Momo-Softworks/Cold-Sweat/blob/1.20.1-FG/src/main/java/com/momosoftworks/coldsweat/api/insulation/StaticInsulation.java . Release-JAR native example folder: `data/cold_sweat/cold_sweat/item/insulator/ad_astra/`. License metadata: https://api.modrinth.com/v2/project/cold-sweat ; compare with the selected JAR's TOML before copying anything.

**[S13] Legendary Survival Overhaul.** Release: https://modrinth.com/mod/legendary-survival-overhaul/version/BsBOQNzX ; metadata: https://api.modrinth.com/v2/version/BsBOQNzX . Source snapshot `59bfcf39fbe3f080d9002f9a681f830889cdfdd7`: https://github.com/Alex-Hashtag/LegendarySurvivalOverhaul/tree/1.20.1 . Exact native data: https://github.com/Alex-Hashtag/LegendarySurvivalOverhaul/blob/1.20.1/src/generated/resources/data/minecraft/legendarysurvivaloverhaul/temperature/items/leather_chestplate.json . Source manager: `api/data/manager/TemperatureDataManager.java` in that branch. License metadata: https://api.modrinth.com/v2/project/legendary-survival-overhaul .

**[S14] Thirst Was Taken.** Release: https://modrinth.com/mod/thirst-was-taken/version/v0d2UK5F ; metadata: https://api.modrinth.com/v2/version/v0d2UK5F . Source snapshot `3f280b12e2d29c116ac19628af8990176d83e9be`: https://github.com/ghen-git/Thirst-Mod/tree/1.20.1 . Native filter: https://github.com/ghen-git/Thirst-Mod/blob/1.20.1/src/main/resources/data/thirst/recipes/compat/create/sand_filter.json . License metadata: https://api.modrinth.com/v2/project/thirst-was-taken .

**[S15] Diet.** Release: https://modrinth.com/mod/diet/version/oHxMouff ; metadata: https://api.modrinth.com/v2/version/oHxMouff . Source snapshot `c737eab4b0024bd245a71aa8e90fb188bc1874a3`: https://github.com/TheIllusiveC4/Diet/tree/1.20.x . Group example: https://github.com/TheIllusiveC4/Diet/blob/1.20.x/common/src/main/resources/data/diet/diet/groups/vegetables.json . License metadata: https://api.modrinth.com/v2/project/diet .

**[S16] No Tree Punching.** Release: https://modrinth.com/mod/no-tree-punching/version/aPNOyI3O ; metadata: https://api.modrinth.com/v2/version/aPNOyI3O . Source snapshot `05c46ca24a69302f0b1fe86b48a54e5704cf4928`: https://github.com/alcatrazEscapee/no-tree-punching/tree/1.20.x . Native plant string: https://github.com/alcatrazEscapee/no-tree-punching/blob/1.20.x/Common/src/main/resources/data/notreepunching/recipes/plant_string.json . Kindling: https://github.com/alcatrazEscapee/no-tree-punching/blob/1.20.x/Common/src/main/resources/data/notreepunching/tags/items/fire_starter_kindling.json . License metadata: https://api.modrinth.com/v2/project/no-tree-punching .

**[S17] TerraFirmaCraft.** Release: https://modrinth.com/mod/terrafirmacraft/version/F35Y7gTr ; metadata: https://api.modrinth.com/v2/version/F35Y7gTr . Source snapshot `b158c9c968cad7afb99cea2c937b301ff275e2e6`: https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.20.x . Retting: https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/recipes/barrel/jute_fiber.json . Loom: https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/recipes/loom/burlap_cloth.json . License/dependency metadata: https://api.modrinth.com/v2/project/terrafirmacraft and the exact version metadata above.

**[S18] Primal Winter.** Release: https://modrinth.com/mod/primal-winter/version/Q6znQM53 ; metadata: https://api.modrinth.com/v2/version/Q6znQM53 . Source snapshot `18ec9bd3263325dee065b42de31caf3a99957339`: https://github.com/alcatrazEscapee/primal-winter/tree/1.20.x . Terrain tag: https://github.com/alcatrazEscapee/primal-winter/blob/1.20.x/Common/src/main/resources/data/primalwinter/tags/blocks/replaceable_with_snowy_stuff.json . License metadata: https://api.modrinth.com/v2/project/primal-winter .

**[S19] Supplementaries.** Release: https://modrinth.com/mod/supplementaries/version/S0TIJ1hU ; metadata: https://api.modrinth.com/v2/version/S0TIJ1hU . Project: https://github.com/MehVahdJukaar/Supplementaries . Use selected release-JAR evidence, not current master: `META-INF/mods.toml`, `data/forge/tags/items/fiber/flax.json`, `data/forge/tags/items/crops/flax.json`, `data/supplementaries/recipes/rope.json`. License metadata: https://api.modrinth.com/v2/project/supplementaries .

**[S20] Immersive Engineering.** Release: https://modrinth.com/mod/immersiveengineering/version/WUzj4tgJ ; metadata: https://api.modrinth.com/v2/version/WUzj4tgJ . Source snapshot `e63e4824800945eccf3684200a2c2270e2e1cdf2`: https://github.com/BluSunrize/ImmersiveEngineering/tree/1.20.1 . Inspected release paths: `META-INF/mods.toml`, `data/forge/tags/items/fiber_hemp.json`, `fabric_hemp.json`, and `data/immersiveengineering/recipes/crafting/hemp_fabric.json`. License metadata: https://api.modrinth.com/v2/project/immersiveengineering .

**[S21] Comforts.** Release: https://modrinth.com/mod/comforts/version/gBDsc134 ; metadata: https://api.modrinth.com/v2/version/gBDsc134 . Source snapshot `deac808fa7c612389c0bdc378dcf814cd5b83d05`: https://github.com/TheIllusiveC4/Comforts/tree/1.20.x . Inspected Forge release hammock recipes under `data/comforts/recipes/`, including their `forge:string`/`forge:rods/wooden` ingredients. Project behavior/license metadata: https://modrinth.com/mod/comforts and https://api.modrinth.com/v2/project/comforts .

**[S22] Create.** Release: https://modrinth.com/mod/create/version/8amzvn9x ; metadata: https://api.modrinth.com/v2/version/8amzvn9x . Source snapshot `588124239141029c5321afd38d20c2eaf872b32b`: https://github.com/Creators-of-Create/Create/tree/mc1.20.1/dev . Inspected 6.0.8 JAR: `META-INF/mods.toml`, `data/create/recipes/compacting/andesite_from_flint.json`, `data/create/recipes/milling/wheat.json`. License metadata: https://api.modrinth.com/v2/project/create ; inspect upstream code/assets licensing before reuse.

**[S23] JEI.** Release: https://modrinth.com/mod/jei/version/4vbuxppk ; metadata: https://api.modrinth.com/v2/version/4vbuxppk . Source snapshot `5f488cd22bb938ceb00370175acb3b0d2760d5ed`: https://github.com/mezz/JustEnoughItems/tree/1.20.1 . API: https://github.com/mezz/JustEnoughItems/blob/1.20.1/CommonApi/src/main/java/mezz/jei/api/IModPlugin.java and https://github.com/mezz/JustEnoughItems/blob/1.20.1/CommonApi/src/main/java/mezz/jei/api/registration/IRecipeRegistration.java . License metadata: https://api.modrinth.com/v2/project/jei .

**[S24] EMI.** Release: https://modrinth.com/mod/emi/version/Axuu9I9R ; metadata: https://api.modrinth.com/v2/version/Axuu9I9R . Source snapshot `7d1665c8c6acd09c6d2069a19d78d1bd0215f2de`: https://github.com/emilyploszaj/emi/tree/1.20 . API: https://github.com/emilyploszaj/emi/blob/1.20/xplat/src/main/java/dev/emi/emi/api/EmiPlugin.java and https://github.com/emilyploszaj/emi/blob/1.20/xplat/src/main/java/dev/emi/emi/api/recipe/EmiInfoRecipe.java . License metadata: https://api.modrinth.com/v2/project/emi .

**[S25] Jade.** Release: https://modrinth.com/mod/jade/version/xJQHCmWJ ; metadata: https://api.modrinth.com/v2/version/xJQHCmWJ . Source snapshot `d7a9e1d3b782f2ea4c387c66e973cc46b9768270`: https://github.com/Snownee/Jade/tree/1.20-forge . API: https://github.com/Snownee/Jade/blob/1.20-forge/src/main/java/snownee/jade/api/IWailaPlugin.java and sibling `IBlockComponentProvider.java`. License metadata: https://api.modrinth.com/v2/project/jade .

**[S26] Curios.** Release: https://modrinth.com/mod/curios/version/IPQlZkz1 ; metadata: https://api.modrinth.com/v2/version/IPQlZkz1 . Source snapshot `a91da6ba2396f6ed9042c8a47a471ca5efc97da8`: https://github.com/TheIllusiveC4/Curios/tree/1.20.x . API: https://github.com/TheIllusiveC4/Curios/blob/1.20.x/src/main/java/top/theillusivec4/curios/api/type/capability/ICurioItem.java . Belt slot: https://github.com/TheIllusiveC4/Curios/blob/1.20.x/src/main/resources/data/curios/curios/slots/belt.json . License metadata: https://api.modrinth.com/v2/project/curios .

**[S27] Patchouli.** Release: https://modrinth.com/mod/patchouli/version/94dtOLgZ ; metadata: https://api.modrinth.com/v2/version/94dtOLgZ . Source snapshot `1a3eb97229a4763ec9d205ef0f102e1239de9c5a`: https://github.com/VazkiiMods/Patchouli/tree/1.20.1 . **Definition:** https://github.com/VazkiiMods/Patchouli/blob/1.20.1/Xplat/src/main/resources/data/patchouli/patchouli_books/comprehensive_test_book/book.json . **Localized content:** https://github.com/VazkiiMods/Patchouli/tree/1.20.1/Xplat/src/main/resources/assets/patchouli/patchouli_books/comprehensive_test_book . API/factory: https://github.com/VazkiiMods/Patchouli/blob/1.20.1/Xplat/src/main/java/vazkii/patchouli/api/PatchouliAPI.java . License metadata: https://api.modrinth.com/v2/project/patchouli .

**[S28] First Aid.** Release: https://modrinth.com/mod/firstaid/version/JzYydXeE ; metadata: https://api.modrinth.com/v2/version/JzYydXeE . Source snapshot `692d4899af5847f0283c335b5d679b7bb4b4f835`: https://github.com/ichttt/FirstAid/tree/1.20.x . Native recipe: https://github.com/ichttt/FirstAid/blob/1.20.x/src/main/resources/data/firstaid/recipes/bandage.json . API/license header: https://github.com/ichttt/FirstAid/blob/1.20.x/src/main/java/ichttt/mods/firstaid/api/healing/HealingItemApiHelper.java . Project license metadata: https://api.modrinth.com/v2/project/firstaid .

**[S29] Additional candidate, deferred.** Immersive Weathering Forge 1.20.1 release 2.0.5: https://modrinth.com/mod/immersive-weathering/version/Fl2lADP9 ; metadata: https://api.modrinth.com/v2/version/Fl2lADP9 . Availability was checked; exact Forge weathering hooks were not. No implementation relies on them.

### 9.4 Completion record for this review

- Full static source/resource/documentation audit completed at the stated public commit.
- 21 evidence-bearing findings, 13 core feature designs, and 21 named integration/coexistence decisions documented; additional candidate and rejected ideas recorded.
- Every finding has severity, confidence, location, current/expected behavior, cause, concrete change scope, acceptance test, and save/dependency guidance. Feature/integration defaults and scores are explicit.
- Runtime verification remains blocked as stated; no in-game result is represented as reproduced.
- This Markdown document is the sole authored project file. Execute fixes in a separate implementation task; this review itself changes no gameplay.
