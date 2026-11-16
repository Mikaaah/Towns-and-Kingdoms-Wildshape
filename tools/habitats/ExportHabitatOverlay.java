package nl.townsandkingdoms.wildshape.data.preset.tags;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

/** Run with the three pure-Java table classes. No Minecraft/loader classes or dependencies needed. */
public final class ExportHabitatOverlay {
    public static void main(String[] args) throws Exception {
        var current = new LinkedHashMap<String, List<String>>();
        CurrentPackHabitats.addTo(current);
        var output = Path.of(args[0]);
        int count = 0;
        for (var entry : PresetBiomeCompat.BRIDGES.entrySet()) {
            var references = current.containsKey(entry.getKey()) ? current.get(entry.getKey())
                    : entry.getValue().stream().filter(value -> value.startsWith("bloomingnature:")).toList();
            if (references.isEmpty()) continue;
            String[] split = entry.getKey().split(":", 2);
            Path path = output.resolve("data/" + split[0] + "/tags/worldgen/biome/" + split[1] + ".json");
            var json = new StringBuilder("{\n  \"replace\": false,\n  \"values\": [\n");
            for (int i = 0; i < references.size(); i++) {
                if (i != 0) json.append(",\n");
                json.append("    { \"id\": \"").append(references.get(i)).append("\", \"required\": false }");
            }
            json.append("\n  ]\n}\n");
            Files.createDirectories(path.getParent());
            Files.writeString(path, json.toString());
            count++;
        }
        System.out.println("Wrote " + count + " append-only optional habitat tag resources");
    }
}
