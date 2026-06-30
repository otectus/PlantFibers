# Plant Fiber + Grass Tier

A Minecraft 1.20.1 Forge mod that adds plant fiber drops from vegetation, healing consumables, grass-tier blocks, grass armor, primitive paper-making, and climbable rope.

## Features

- **Plant Fiber drops** from breaking grass and other vegetation
- **Healing items** — Grass Bandage and Grass Plaster with regeneration effects and cooldowns
- **Grass Fiber Block** — decorative, flammable building block crafted from plant fibers
- **Grass Bed** — custom 3D grass mound bed with scattered grass overlay and pillow
- **Grass Armor** — full set of grass-tier armor (toggleable in config)
- **Material progression** — press Plant Fibers + a Water Bucket into **Plant Mesh** (smelts into Paper), and combine Plant Fiber + String into **Reinforced Fibers**
- **Rope** — climbable, self-supporting hanging rope crafted from Reinforced Fibers; hang it below a sturdy block, or **tie it to the side of a fence** so it starts one block outward and then extends downward. Tag-driven anchors (`#minecraft:fences`), optional waterlogging, and an unsupported-rope break cascade
- **Configurable** — drop rates, healing values, armor toggle, and a full `rope` config group

## Requirements

- Minecraft 1.20.1
- Forge 47+

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1
2. Get the mod from the [CurseForge project page](https://www.curseforge.com/minecraft/mc-mods/plant-fiber-grass-tier) (or download the `.jar` from GitHub Releases)
3. Place the `.jar` in your `.minecraft/mods/` folder
4. Launch the game

## Configuration

Every feature can be tuned or disabled in `config/plantfiber-common.toml`:

- **drops** — toggle Plant Fiber drops and set the per-block drop chance (default `0.25`)
- **bandage** / **plaster** — toggle each consumable and set regeneration duration, amplifier, and cooldown
- **grass** — toggle the Grass Bed and Grass Armor, and set armor durability relative to leather
- **crafting** — toggle the Plant Mesh → Paper recipes and Reinforced Fibers crafting
- **rope** — toggle Rope, fence ties, the unsupported-rope break cascade, waterlogging, and plant replacement

See [CURSEFORGE.md](CURSEFORGE.md) for the full feature and configuration breakdown.

## License

All Rights Reserved
