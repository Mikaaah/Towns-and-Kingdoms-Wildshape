# Getting started

Install **`wildshape-1.0.3-neoforge-1.21.1.jar`** with **Minecraft 1.21.1, NeoForge and Java 21**. Replace the earlier terrain JAR and restart Minecraft. Remove superseded `utfstreams`, `tfacfix` and `utfoptimizer` adapters; keep Flow Control if you use its tools. Optional mods are installed separately.

Select **Wildshape** in Create World. On a dedicated server, set:

```properties
level-type=wildshape:wildshape
```

Wildshape 1.0.3 keeps the `wildshape` registry identity used by 1.0.0–1.0.2. Terrain corrections apply to newly generated chunks; saved terrain is not rewritten. With optional SkyLands 0.9.4 installed, the extra Overworld build space is supplied at startup, including existing saves. New islands appear only in new chunks, beyond any pregenerated area.

**Worlds from an `ultraterraforged` build require a fresh world or a separately verified migration.** Importing settings into `config/wildshape/` does not migrate saved registry references. See the [identity guide](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/WILDSHAPE_IDENTITY.md).

See the [edition table](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/wiki/Compatibility).

[Documentation home](Home)
