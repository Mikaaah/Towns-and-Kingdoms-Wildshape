# Wildshape — Development

Documentation branding does not rename registry IDs, configuration paths, artifacts or mod metadata. The current technical name is Wildshape; the internal mod ID is `wildshape`.

Build on Java 21:

```sh
./gradlew :neoforge:build -Pwildshape.perfMods=false
./gradlew :fabric:build -Pwildshape.perfMods=false
./gradlew :common:test -Pwildshape.perfMods=false
```

Read the [23-finding audit matrix](../AUDIT-STATUS.md). Automated builds do not establish full-pack stability. Test multiple seeds, high terrain, river junctions, caves and dungeon types with the actual TNK3 mod list.
