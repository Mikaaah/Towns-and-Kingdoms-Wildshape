package nl.townsandkingdoms.wildshape.data.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.data.preset.settings.StructureSettings.StructureSetEntry;

/** Patch loaded JSON before its native codec runs, including sets absent from the server's vanilla bootstrap. */
public final class StructureSetJsonPatcher {
    private StructureSetJsonPatcher() {}

    public static boolean patch(Preset preset, ResourceLocation id, JsonElement data) {
        if (!data.isJsonObject()) return false;
        JsonObject set = data.getAsJsonObject();
        if (!set.has("placement") || !set.get("placement").isJsonObject()) return false;
        JsonObject placement = set.getAsJsonObject("placement");
        StructureSetEntry entry = preset.structures().entries.get(id);
        if (entry == null) {
            if (!placement.has("spacing")) return false;
            int current = placement.get("spacing").getAsInt();
            int wanted = StructureSetPolicy.spacing(id, current);
            if (wanted == current) return false;
            placement.addProperty("spacing", wanted);
            return true;
        }
        String before = data.toString();
        if (Boolean.FALSE.equals(entry.enabled)) set.add("structures", new JsonArray());
        if (placement.has("spacing")) {
            int spacing = Math.max(1, entry.spacing != null ? entry.spacing : placement.get("spacing").getAsInt());
            int separation = entry.separation != null ? entry.separation
                    : placement.has("separation") ? placement.get("separation").getAsInt() : 0;
            if (entry.spacing != null) placement.addProperty("spacing", spacing);
            if (entry.separation != null || separation >= spacing)
                placement.addProperty("separation", Math.max(0, Math.min(separation, spacing - 1)));
        }
        if (entry.frequency != null) placement.addProperty("frequency", entry.frequency);
        if (entry.salt != null) placement.addProperty("salt", entry.salt);
        if (placement.has("distance") && entry.distance != null) placement.addProperty("distance", entry.distance);
        if (placement.has("spread") && entry.spread != null) placement.addProperty("spread", entry.spread);
        if (placement.has("count") && entry.count != null) placement.addProperty("count", entry.count);
        return !before.equals(data.toString());
    }
}
