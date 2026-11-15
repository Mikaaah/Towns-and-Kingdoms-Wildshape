# Compatibility

**Current edition: Wildshape 1.0.3 · NeoForge 1.21.1 · Java 21.** The mod ID and resource namespace are `wildshape`; configuration uses `config/wildshape/`.

| Integration | Supported versions / verification |
| --- | --- |
| Streams Reflowing | Inspected adapters for 2.15.2, 2.16.2, 2.17.1, 2.17.3 and 2.17.4; 1.0.2 generation/save/reload tested with 2.17.4 |
| Flow Control | Optional; manual directions and stillwater overrides retain priority. Earlier tested pairings: 0.2.2 + Streams 2.16.2, and 1.0.0 + Streams 2.17.1. Later Flow Control releases must support the selected Streams version themselves. |
| Alex's Caves | Integrated optional NeoForge adapter; keep the actual cave mod and its dependencies installed |
| SkyLands NeoForge Port 0.9.4 | Optional height compatibility for new and existing Wildshape worlds. Native sky biomes/islands remain owned by SkyLands. New chunks only; see the [setup and verification guide](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/SKYLANDS_COMPATIBILITY.md). |
| C2ME | 1.0.2 generation/save/reload fixture passed with 0.4.0-alpha.0.124, Streams 2.17.4 and Nature's Spirit 2.3.2 on NeoForge 21.1.266 |

Remove the superseded `utfstreams`, `tfacfix` and `utfoptimizer` adapters when using their integrated equivalents. Keep Flow Control for its tools. Use one terrain generator JAR.

**Use a fresh world when replacing an `ultraterraforged` build.** Importing settings does not migrate registry references in an old save or datapack. Version 1.0.2 keeps the registry identity of Wildshape 1.0.0/1.0.1; generation corrections apply to new chunks.

The isolated server fixtures and focused tests do not certify the full graphical modpack or replay the original server seed/preset. See the [1.0.2 audit and exact test scope](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/WILDSHAPE_WORLDGEN_FIX_2026-11-15.md).

[Current compatibility matrix](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK3_COMPATIBILITY.md) · [Streams details](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK3_STREAMS_REFLOWING.md)

[Guide home](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/wiki) · [Report an issue](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/issues) · [Discord](https://discord.gg/CRYk3uDPVt)
