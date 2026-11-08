package nl.townsandkingdoms.wildshape.test;

import nl.townsandkingdoms.wildshape.platform.ConfigMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @TempDir Path root;

    @Test void importsNestedPresetsWithCanonicalThenNewestLegacyPriority() throws Exception {
        Files.createDirectories(root.resolve("tnkterraforged/presets"));
        Files.createDirectories(root.resolve("ultraterraforged/presets"));
        Files.createDirectories(root.resolve("reterraforged/presets"));
        Files.createDirectories(root.resolve("wildshape"));
        Files.writeString(root.resolve("wildshape/client.json"), "current");
        Files.writeString(root.resolve("tnkterraforged/client.json"), "old");
        Files.writeString(root.resolve("tnkterraforged/presets/custom.json"), "fork preset");
        Files.writeString(root.resolve("ultraterraforged/presets/custom.json"), "upstream preset");
        Files.writeString(root.resolve("reterraforged/presets/custom.json"), "older preset");
        Files.writeString(root.resolve("reterraforged/presets/extra.json"), "extra preset");
        ConfigMigration.migrate(root);
        ConfigMigration.migrate(root);
        assertEquals("current", Files.readString(root.resolve("wildshape/client.json")));
        assertEquals("fork preset", Files.readString(root.resolve("wildshape/presets/custom.json")));
        assertEquals("extra preset", Files.readString(root.resolve("wildshape/presets/extra.json")));
        assertEquals("old", Files.readString(root.resolve("tnkterraforged/client.json")));
    }

    @Test void freshInstallCreatesOnlyCanonicalFolder() throws Exception {
        ConfigMigration.migrate(root);
        assertTrue(Files.isDirectory(root.resolve("wildshape")));
        assertFalse(Files.exists(root.resolve("tnkterraforged")));
        assertFalse(Files.exists(root.resolve("ultraterraforged")));
    }

    @Test void importsOptionalCompatSettingsBeforeNeoForgeRegistersThem() throws Exception {
        Files.writeString(root.resolve("ultraterraforged-alex-caves.toml"), "alex setting");
        Files.writeString(root.resolve("tnkterraforged-streams.toml"), "current streams setting");
        Files.writeString(root.resolve("utfstreams-common.toml"), "older streams setting");
        ConfigMigration.migrate(root);
        assertEquals("alex setting", Files.readString(root.resolve("wildshape/alex-caves.toml")));
        assertEquals("current streams setting", Files.readString(root.resolve("wildshape/streams.toml")));
    }

    @Test void canonicalFolderWinsOverLooseSettingsAndMigrationSkipsSymlinks() throws Exception {
        Files.createDirectories(root.resolve("wildshape"));
        Files.createDirectories(root.resolve("tnkterraforged"));
        Files.writeString(root.resolve("wildshape/streams.toml"), "canonical");
        Files.writeString(root.resolve("tnkterraforged-streams.toml"), "loose");
        Path outside = Files.createDirectory(root.resolve("outside"));
        Files.writeString(outside.resolve("ignored.json"), "not a setting");
        Files.createSymbolicLink(root.resolve("tnkterraforged/linked"), outside);
        ConfigMigration.migrate(root);
        assertEquals("canonical", Files.readString(root.resolve("wildshape/streams.toml")));
        assertFalse(Files.exists(root.resolve("wildshape/linked")));
        assertTrue(Files.exists(root.resolve("tnkterraforged-streams.toml")));
    }
}
