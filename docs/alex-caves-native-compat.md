# Optional native Alex's Caves integration

Ported from [Terraforged Alex's Caves Fix](https://github.com/Mikaaah/Terraforged-Alexs-Caves-Fix)
for unofficial Alex's Caves 1.21.1 NeoForge 2.0.10.

- Compiled against the port using a **compile-only** dependency. Its 71 MB JAR is not included in this mod.
- Conditional Mixins only apply when `alexscaves` is installed; otherwise vanilla/other dimensions are unchanged.
- Integrated rare cave biome placement, native chamber structure generation, corrected locator, and sampler context binding.
- Built-in UTF native-carving protection replaces the old companion CarvingMixin, avoiding double hooks.
- When testing this integrated fork, **remove the standalone `tfacfix` mod** to prevent duplicate Mixin injections. Keep Alex's Caves and its required Citadel port.
- GPL-3.0 compatibility source has been imported into the combined NeoForge distribution. The original MIT terms/notices for upstream UltraTerraForged remain intact. See `LICENSE-ALEX-COMPAT-GPL-3.0`.
- The native integration must still pass real-game testing with Alex's Caves 2.0.10 before being considered stable.
