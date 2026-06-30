# Plant Fiber + Grass Tier

**Plant Fiber + Grass Tier** is a lightweight survival utility mod for **Minecraft 1.20.1 (Forge)**. It enriches the early game with plant-based crafting: harvest fiber from vegetation, patch yourself up with grass bandages, work your way up a primitive material chain into paper and rope, and sleep on a custom grass bed — all without overhauling vanilla. Everything is tag-driven, data-pack friendly, and toggleable in config.

## Features

### 🌿 Plant Fiber
Breaking grass and other vegetation has a chance (default **25%**) to drop **Plant Fiber**. The source list is the `plantfiber:vegetation` block tag, so modpacks and data packs can add or remove plants freely. Fiber is the backbone of every recipe in the mod.

### ❤️ Healing Consumables
- **Grass Bandage** — right-click to apply **Regeneration I** with a short cooldown.
- **Grass Plaster** — a stronger patch granting **Regeneration II** for a longer duration, with a longer cooldown.

Both items show their regeneration level, duration, and cooldown in the tooltip, and every value is configurable.

### 🧱 Grass Fiber Block
A decorative, flammable building block crafted from Plant Fibers. Mineable with a hoe and fully reversible — break one block back down into **9 Plant Fiber**.

### 🛏️ Grass Bed
A custom **3D grass-mound bed** built from JSON block models (no vanilla bed renderer). It features a scattered horizontal grass overlay, a raised pillow hump on the head, and a dedicated hand-held item model. Sleeps and sets your spawn just like a vanilla bed.

### 🛡️ Grass Armor
A full grass-tier armor set (helmet, chestplate, leggings, boots) — leather-level protection at reduced durability, repairable with Plant Fiber. Disposable starter gear that gets you moving toward iron. Toggleable in config.

### 🧵 Material Progression
- **Plant Mesh** — press Plant Fibers around a **Water Bucket** (`PPP` / ` W `) into a woven sheet; the bucket is returned. Plant Mesh dries into vanilla **Paper** via furnace smelting or campfire cooking.
- **Reinforced Fibers** — combine **Plant Fiber + String** into a sturdier cord, the ingredient for Rope.

### 🪢 Rope
A climbable, self-supporting hanging Rope crafted vertically from **3 Reinforced Fibers** (yields 3 Rope):
- **Climbable** like a ladder or vine (via `#minecraft:climbable`) — no solid block needed behind each segment.
- **Extend downward** by right-clicking an existing Rope with Rope in hand; the column grows to the bottom of the connected chain.
- **Tie it to a fence** — side-click any block in `#minecraft:fences` to wrap a **rope knot** around the post; the Rope starts one block outward and then hangs down.
- **Support cascade** — Rope must hang from a valid anchor or connected Rope; lose support and the unsupported Rope below breaks, cascading downward.
- Optional **waterlogging**, flammable, destroyed by pistons, and drops itself.

## Configuration

Every feature can be tuned or disabled in the mod's common config (`config/plantfiber-common.toml`):

- **drops** — enable Plant Fiber drops; set the per-block drop chance (default `0.25`).
- **bandage** — enable; regeneration duration, amplifier, and cooldown.
- **plaster** — enable; regeneration duration, amplifier, and cooldown.
- **grass** — enable the Grass Bed; enable Grass Armor; armor durability multiplier vs. leather (default `0.5`).
- **crafting** — enable the Plant Mesh → Paper recipes; enable Reinforced Fibers crafting.
- **rope** — enable Rope; allow fence ties; break unsupported Rope; whether broken Rope drops; allow waterlogging; allow Rope to replace plants when extending.

## Compatibility

- **Tag-driven** — vegetation sources (`plantfiber:vegetation`), fence anchors (`#minecraft:fences`), and climbing (`#minecraft:climbable`) are all tags you can extend from a data pack.
- **Plays nice with other mods** — fiber drops use a Forge global loot modifier with `replace: false`, so it stacks alongside other mods' loot modifiers instead of overriding loot tables.
- **Server-safe** — all rendering code is client-gated; the mod runs cleanly on dedicated servers.
- **Update-safe** — no existing registry IDs, recipes, tags, or loot tables were changed between versions, so older worlds load unaffected.

## Requirements

- Minecraft **1.20.1**
- Minecraft **Forge 47+**

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1.
2. Get the mod through the **CurseForge app**, or download the `.jar` from the Files tab.
3. Place the `.jar` in your `.minecraft/mods/` folder.
4. Launch the game.

## License

All Rights Reserved.
