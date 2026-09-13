# Plant Fibers visual refresh

Open [the interactive before-and-after preview](preview.html) directly in a browser, or
view [the static contact sheet](preview.png). The interactive preview includes the grass
bed, fiber block, hanging rope, fence attachment, equipped armor, and bed inventory model.
It is a self-contained snapshot of this asset pass and needs no network connection.

## Artwork

All 17 existing textures were refreshed, and two dedicated bed textures were added in
[`src/main/resources/assets/plantfiber/textures`](../../src/main/resources/assets/plantfiber/textures):

- Item and block textures remain **16×16**. Muted moss and sage greens, straw highlights,
  and warm twine replace the saturated greens and black outlines.
- Loose fibers, reinforced bundles, mesh, rope, healing items, and armor have distinct
  inventory silhouettes. Mesh openings and the leggings gap are transparent.
- Armor atlases remain **64×32**, with the original alpha masks preserved pixel for pixel.
  This retains the helmet opening, leaf fringe, and existing equipment coverage.
- The rope knot atlas remains **32×32**, matching the entity model's UV layout. Atlas sizes
  describe several unwrapped surfaces, not an increase in texture density.
- The final PNGs share a 16-color palette, use binary alpha, and contain no interpolated
  edge pixels. Materials used on solid block faces are fully opaque.

The artwork was generated and revised with the **built-in imagegen tool**. The
[final prompt set](texture-prompts.json) records each installed texture, its dimensions,
and the generation prompt. Import used ImageMagick for nearest-neighbor resizing,
sprite framing, palette normalization without dithering, and exact armor alpha masking.
Generated source images were retained in the imagegen cache; the runtime PNGs are stored
in the project. Discarded draft atlases are not included.

## Models

The grass bed now has a stepped pressed-grass base, a woven sleeping surface, a pale straw
pillow, two crosswise twine bindings, and small loose fibers. Both placed halves and the
inventory model use the same materials. The placed geometry fits within the existing
six-pixel collision height, and the two halves meet at their block boundaries.

The hanging rope has a stepped cross section with its four square corners removed.
Both centered and fence-side variants retain their original attachment bounds, and the
fence knot's side strand uses the same profile. UVs remain within the existing atlas.
The fiber block now uses its end-grain texture on both the top and bottom.

Bed models declare the cutout render type explicitly, following
[Forge's model render-type guidance](https://docs.minecraftforge.net/en/1.20.x/rendering/modelextensions/rendertypes/).
Gameplay code, blockstates, recipes, configuration, and collision shapes were not changed
by this visual pass.

## Validation

- `./gradlew --offline compileJava processResources jar reobfJar` passed.
- All 19 PNGs passed checks for dimensions, palette membership, alpha, and armor masks.
- All 17 model trees passed parent/texture resolution, UV bounds, face-area, and
  blockstate-reference checks against the local Minecraft 1.20.1 resources.
- Bed height, both rope attachment envelopes, and knot atlas bounds were checked.
- All 40 packaged asset files match their source bytes. Development fixtures remain
  excluded from the JAR.
- The offline preview rendered all six model views at four rotations without JavaScript
  errors; the resulting before-and-after views were visually inspected.

The preview is an orthographic asset viewer, not a Minecraft screenshot. In-game lighting,
ambient occlusion, and resource-pack interactions were not tested in a running client.
