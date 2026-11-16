# Rebuild optional existing-world habitat overlays

Use Java 21 from the repository root:

```bash
mkdir -p /tmp/wildshape-habitat-export
javac -d /tmp/wildshape-habitat-export common/src/main/java/nl/townsandkingdoms/wildshape/data/preset/tags/BetaPackHabitats.java common/src/main/java/nl/townsandkingdoms/wildshape/data/preset/tags/PresetBiomeCompat.java common/src/main/java/nl/townsandkingdoms/wildshape/data/preset/tags/CurrentPackHabitats.java tools/habitats/ExportHabitatOverlay.java
java -cp /tmp/wildshape-habitat-export nl.townsandkingdoms.wildshape.data.preset.tags.ExportHabitatOverlay common/src/main/resources
```

The exporter writes only new current-pack bridges and Blooming Nature additions, each as an append-only optional biome tag. This lets an older saved preset retain its source and settings while acquiring current habitat additions. Native registry features, biome modifier rates and spawns are not recreated. Existing tag membership is merged according to ordinary datapack priority; a higher-priority explicit replacement/removal remains authoritative.

These common tag resources apply whenever Wildshape is installed, including ordinary worlds. This scope is intentional for saved-world compatibility. `HabitatOverlayTest` checks optional references, duplicate avoidance, preset/resource parity and linked Epic habitats.
