package com.konorpg.client.render;

import java.util.Set;

/** Skins that use the slim (3px) arm layout. Generated from the skin PNGs (see tools/). */
public final class SlimSkins {
    private static final Set<String> SLIM = Set.of();

    public static boolean is(String skin) { return SLIM.contains(skin); }

    private SlimSkins() {}
}
