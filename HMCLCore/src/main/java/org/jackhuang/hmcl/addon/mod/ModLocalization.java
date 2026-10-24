package org.jackhuang.hmcl.addon.mod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * @author Wulian233
 */
public record ModLocalization(Set<String> languages, String name, String description) {
    public static final ModLocalization EMPTY = new ModLocalization(Set.of(), null, null);

    public ModLocalization {
        languages = Set.copyOf(languages);
    }

    public static ModLocalization read(Path file, String modId, String language) {
        try (var tree = CompressingUtils.openZipTree(file)) {
            var assets = tree.getDirectory("assets");
            if (assets == null) return EMPTY;

            Set<String> languages = new HashSet<>();
            String name = null;
            String description = null;
            String filename = language.toLowerCase(Locale.ROOT) + ".json";
            for (var namespace : assets.getSubDirs().values()) {
                var lang = namespace.getSubDirs().get("lang");
                if (lang == null) continue;
                for (String languageFile : lang.getFiles().keySet()) {
                    if (languageFile.endsWith(".json"))
                        languages.add(languageFile.substring(0, languageFile.length() - 5).toLowerCase(Locale.ROOT));
                }
                var entry = lang.getFiles().get(filename);
                if (entry == null || name != null && description != null) continue;

                JsonObject translations = JsonUtils.fromNonNullJsonFully(tree.getInputStream(entry), JsonObject.class);
                if (name == null)
                    name = translation(translations, modId, "fml.menu.mods.info.displayname.", "modmenu.nameTranslation.");
                if (description == null)
                    description = translation(translations, modId, "fml.menu.mods.info.description.", "modmenu.descriptionTranslation.");
            }
            return new ModLocalization(languages, name, description);
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to scan mod language files in " + file, e);
            return EMPTY;
        }
    }

    private static String translation(JsonObject json, String modId, String firstPrefix, String secondPrefix) {
        String value = string(json, firstPrefix + modId);
        return value != null ? value : string(json, secondPrefix + modId);
    }

    private static String string(JsonObject json, String key) {
        var value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String text = value.getAsString();
        return text.isBlank() ? null : text;
    }
}
