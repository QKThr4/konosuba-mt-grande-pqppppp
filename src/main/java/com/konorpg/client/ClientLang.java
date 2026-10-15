package com.konorpg.client;

import com.konorpg.util.Lang;
import net.minecraft.client.Minecraft;

/** Client-side shortcut: translates English source text to the player's game language. */
public final class ClientLang {
    public static String code() { return Minecraft.getInstance().options.languageCode; }

    public static String tr(String en, Object... args) { return Lang.tr(code(), en, args); }

    private ClientLang() {}
}
