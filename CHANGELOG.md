# Changelog

## [1.2.0] - 2026-06-20

### Rope — Fence Ties & Textures
- Redrew the Rope block texture as a braided green cord matching the Rope **item**'s palette and twist (the previous texture rendered as mottled noise on the block)
- Tying Rope to a fence now wraps a **rope knot around the fence post itself** (a lightweight knot entity, leash-knot style) rather than a block beside the fence; the hanging rope then extends outward and down from it
- Documented the fence-tie behavior: side-click a fence (any block in `#minecraft:fences`) to start a Rope one block outward, then extend it downward like any other Rope

### Fixes
- Rope placement now returns `PASS` instead of `FAIL` when it cannot place at the targeted spot (and when Rope is disabled in config), so the off-hand item is still tried — e.g. Rope in the main hand no longer eats an off-hand item's use against a non-anchor surface

## [1.1.0] - 2026-06-19

### Material Progression
- Added **Plant Mesh** — press Plant Fibers and a Water Bucket into a woven sheet (`PPP` / ` W `); the bucket is returned via the vanilla crafting remainder
- Plant Mesh dries into vanilla **Paper** via furnace smelting and campfire cooking (cook time 200, 0.1 XP)
- Added **Reinforced Fibers** — 1 Plant Fiber + 1 String (shapeless)

### Rope
- Added **Rope**, a climbable, self-supporting hanging block crafted vertically from 3 Reinforced Fibers (yields 3 Rope)
- Climbable like a ladder/vine (via `#minecraft:climbable`) without needing a solid block behind every segment
- Right-click an existing Rope with Rope in hand to extend the column downward to the bottom of the connected chain
- Tie Rope to the side of a fence (any block in `#minecraft:fences`): side-click a fence to start a Rope one block outward (`fence_anchored` state, facing back toward the fence); it is supported by the fence and breaks when the fence is removed
- Support validation: Rope must hang from a valid anchor (sturdy face above or `#plantfiber:rope_anchor_blocks`) or from connected Rope; losing support breaks unsupported Rope below, cascading downward
- Waterloggable (opt-in via config), flammable, destroyed by pistons, drops itself

### Tags
- Reused the existing `plantfiber:vegetation` tag as the plant-fiber source list
- Added `plantfiber:fibers`, `plantfiber:reinforced_fibers`, `plantfiber:rope_anchor_blocks`, `plantfiber:rope_replaceable_blocks`, `plantfiber:rope_fence_anchors` (defaults to `#minecraft:fences`), and registered Rope in `minecraft:climbable`

### Config
- Added `crafting` toggles: `enablePlantMeshPaperRecipe`, `enableReinforcedFibers` (gate recipes via a `plantfiber:config` recipe condition)
- Added `rope` group: `enableRope`, `enableFenceRopeAnchors`, `ropeBreaksWhenUnsupported`, `unsupportedRopeDropsItems`, `ropeCanBeWaterlogged`, `ropeCanReplacePlants`

### Compatibility
- No existing registry IDs, recipes, tags, or loot tables changed — old worlds load unaffected

## [1.0.2] - 2026-03-23

### Custom Grass Bed
- Replaced vanilla BedBlock with custom GrassBedBlock using JSON block models instead of BedRenderer
- Grass Bed now renders as a 3D grass mound with scattered horizontal grass overlay planes
- Head block features a raised pillow hump at the far end
- Grass strands lay flat across the surface with subtle tilt for natural depth
- Dedicated 3D hand-held item model with proper display transforms for all views
- Eliminated all reflection hacks (ModSetup validBlocks, ClientSetup BED_MATERIALS)
- Added custom BlockEntity and BlockEntityType for bed sleep/respawn compatibility
- Registered cutout render type for proper grass texture transparency

### Bug Fixes
- Fixed crash caused by invalid ResourceLocation `.*:blocks/.*` in loot modifier condition — reverted to empty conditions with Java-side filtering

## [1.0.1] - 2026-03-23

### Bug Fixes
- Fixed grass armor being equippable via shift-click or dispenser when disabled in config by overriding `canEquip()`

### Corrections
- Renamed "Grass Block" to "Grass Fiber Block" in localization to avoid conflict with vanilla's Grass Block
- Added missing `minecraft:kelp_plant`, `minecraft:cave_vines`, and `minecraft:cave_vines_plant` to vegetation tag
- Made Grass Fiber Block flammable (ignitedByLava, flammability 60, fire spread 30)

### Enhancements
- Added `minecraft:mineable/hoe` tag for Grass Fiber Block
- Changed bandage/plaster use animation from EAT to BOW
- Expanded vegetation tag with 13 additional plant blocks (dead bush, lily pad, spore blossom, dripleaf, hanging roots, moss carpet, pitcher plant, torchflower, pink petals)
- Added tooltips to healing items showing regeneration level, duration, and cooldown
- Added decomposition recipe: 1 Grass Fiber Block -> 9 Plant Fiber

### Repo Setup
- Added proper `.gitignore` for Forge mod projects
- Removed platform-specific Java home path from `gradle.properties`
