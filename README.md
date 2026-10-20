# UltraTerraForged — Community Maintenance Fork

**Large-scale terrain and underground world generation for Minecraft 1.21.1.**
Maintained by **Mikaaah** for the *Towns & Kingdoms 3* modpack, building on the work of the original UltraTerraForged, ReTerraForged and TerraForged developers.

> **Unofficial fork.** Not the upstream UltraTerraForged project. Existing worlds are not regenerated, and significant world-generation updates can cause seams between old and newly generated chunks.

| Minecraft | Loader | Status |
| --- | --- | --- |
| **1.21.1 / Java 21** | **NeoForge 21.1.x** | Primary edition; optional Alex's Caves integration |
| **1.21.1 / Java 21** | **Fabric + Fabric API** | Supported edition; Alex's Caves integration not included |

## What changed in this fork?

### Bigger, interconnected underground caves
- Giant caverns roughly **230–350 blocks across**, with broad irregular chamber shapes, lakes and rock pillars.
- **New winding connecting passages** between neighboring cavern sites, built deterministically across chunk borders.
- Cavern sites are more common under higher terrain while roofs stay **at or below Y=30**, protected by at least 30 blocks of cover.
- Original small vanilla spaghetti, noodle and carver cave densities are **reduced in shipped presets**; cheese caves stay enabled. Personal/custom presets retain their saved settings.
- Normal optional cave features (underground rivers, karst, lava tubes, sea caves and more) remain configurable.

### Rivers, water and caves
- Gentler river transitions, smaller elevation terraces and improved water continuity at river mouths.
- Raised-water filling recognizes existing source water.
- Cave-water reconciliation floods newly exposed cavities when connected to a local source and schedules boundary updates. **Cross-chunk fluid continuity must still be inspected in-game**; this is not an aquifer replacement.
- More compact primitive water-repair queues and water-level tracking reduce temporary allocations.

### Performance and stability
- Retry failed tile-generation futures rather than permanently caching a broken generation result.
- Return pooled arrays after generation failures, *after* all parallel workers have finished.
- Avoid polling caches that never expire; make bounded caches and worker pools safer.
- Correct stale world-lookup caching, non-UTF spawn injection, thread-local restoration and certain resource-lifecycle errors.
- Fix mouse click/drag handling in Create World preset sliders.

### Optional Alex's Caves integration (NeoForge only)
The fork integrates **Mikaaah's own [TerraForged × Alex's Caves compatibility fix](https://github.com/Mikaaah/Terraforged-Alexs-Caves-Fix)** into the NeoForge module. It preserves native cave biomes/chambers and adjusts placement for UltraTerraForged. **Alex's Caves is optional**; it is never bundled or required for normal worlds. When enabled, use the compatible Alex's Caves 2.0.10 NeoForge port and its Citadel dependency.

**Do not install the separate `tfacfix` mod with this integrated edition**—its Mixins would duplicate the built-in integration.

## Installation
1. Use **one** UltraTerraForged JAR matching your loader and Minecraft **1.21.1**.
2. Install Fabric API on Fabric; use a compatible NeoForge 21.1.x on NeoForge.
3. Start a **new world** with the UltraTerraForged world type. Back up any existing saves before experimenting.
4. Customize terrain via the Create World **Terrain** tab, then select your preset.
5. For a comparable performance/terrain test, pregenerate fresh chunks on the same seed with identical mod lists.

## Source, branches and validation
- **[NeoForge 1.21.1](https://github.com/Mikaaah/ultraterraforged-personal/tree/neoforge-1.21.1)** — primary development branch.
- **[Fabric 1.21.1](https://github.com/Mikaaah/ultraterraforged-personal/tree/fabric-1.21.1)** — same shared generation engine, loader-specific Fabric files.
- [Detailed change history](CHANGELOG-PERSONAL.md) · [Original 23-finding audit](AUDIT-STATUS.md) · [Alex's Caves notes](docs/alex-caves-native-compat.md)

**Builds and automated tests are not proof of in-game visual correctness.** River seams, cavern frequency, server memory usage and input handling need real-world verification with the full modpack.

### Development
```bash
./gradlew :neoforge:build -Putf.perfMods=false
./gradlew :fabric:build -Putf.perfMods=false
./gradlew :common:test -Putf.perfMods=false
```

## Attribution and licenses
- **UltraTerraForged:** [Pandaismyname1](https://github.com/Pandaismyname1/ultraterraforged).
- **ReTerraForged:** [raccoonman](https://github.com/racoonman2/ReTerraForged) and contributors.
- **TerraForged:** [dags](https://github.com/dags-/), [Won-Ton](https://github.com/Won-Ton) and contributors.
- **Community fork and Alex's Caves compatibility implementation:** Mikaaah / Towns & Kingdoms.

The original UltraTerraForged/TerraForged MIT copyright notices are preserved in [LICENSE](LICENSE). The incorporated NeoForge Alex's Caves compatibility sources carry the terms described in [LICENSE-ALEX-COMPAT-GPL-3.0](LICENSE-ALEX-COMPAT-GPL-3.0). Ownership of the compatibility implementation does not cancel licensing obligations for other contributed code.
