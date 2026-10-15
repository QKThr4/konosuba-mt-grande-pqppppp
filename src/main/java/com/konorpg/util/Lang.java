package com.konorpg.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konorpg.KonoRPG;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tiny gettext-style localization shared by server and client. All source strings are English; the
 * Portuguese table (assets/konorpg/lang/texts_pt.json: {english: portuguese}) is applied when the
 * player's game language starts with "pt". Any other language falls back to English.
 */
public final class Lang {
    private static volatile Map<String, String> pt;
    private static final Map<UUID, String> CODES = new ConcurrentHashMap<>();

    private static Map<String, String> table() {
        Map<String, String> t = pt;
        if (t == null) {
            t = new HashMap<>();
            try (InputStream in = Lang.class.getResourceAsStream("/assets/konorpg/lang/texts_pt.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : o.entrySet()) t.put(e.getKey(), e.getValue().getAsString());
                }
            } catch (Exception ex) {
                KonoRPG.LOGGER.error("Could not load Portuguese texts", ex);
            }
            pt = t;
        }
        return t;
    }

    public static boolean isPt(String code) { return code != null && code.toLowerCase(Locale.ROOT).startsWith("pt"); }

    public static String tr(String code, String en, Object... args) {
        String s = en;
        if (isPt(code)) {
            String p = table().get(en);
            if (p != null) s = p;
        }
        if (args.length == 0) return s;
        try {
            return String.format(Locale.ROOT, s, args);
        } catch (Exception e) {
            return s;
        }
    }

    public static String tr(ServerPlayer p, String en, Object... args) {
        return tr(p == null ? null : CODES.get(p.getUUID()), en, args);
    }

    public static void set(UUID id, String code) { CODES.put(id, code == null ? "en_us" : code); }

    public static void forget(UUID id) { CODES.remove(id); }

    private Lang() {}
}
