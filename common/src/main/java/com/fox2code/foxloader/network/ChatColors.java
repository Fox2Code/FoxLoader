package com.fox2code.foxloader.network;

/**
 * List of supported color code by ReIndev render engine,
 * private entries are unsupported by ReIndev
 */
public class ChatColors {
    private ChatColors() {}
    public static final char COLOR_CHAR = '\u00A7';
    public static final String BLACK = COLOR_CHAR + "0";
    public static final String DARK_BLUE = COLOR_CHAR + "1";
    public static final String DARK_GREEN = COLOR_CHAR + "2";
    public static final String DARK_AQUA = COLOR_CHAR + "3";
    public static final String DARK_RED = COLOR_CHAR + "4";
    public static final String DARK_PURPLE = COLOR_CHAR + "5";
    public static final String GOLD = COLOR_CHAR + "6";
    public static final String GRAY = COLOR_CHAR + "7";
    public static final String DARK_GRAY = COLOR_CHAR + "8";
    public static final String BLUE = COLOR_CHAR + "9";
    public static final String GREEN = COLOR_CHAR + "a";
    public static final String AQUA = COLOR_CHAR + "b";
    public static final String RED = COLOR_CHAR + "c";
    public static final String LIGHT_PURPLE = COLOR_CHAR + "d";
    public static final String YELLOW = COLOR_CHAR + "e";
    public static final String WHITE = COLOR_CHAR + "f";

    private static final String OBFUSCATED = COLOR_CHAR + "k"; // unsupported
    public static final String BOLD = COLOR_CHAR + "l";
    private static final String STRIKETHROUGH = COLOR_CHAR + "m"; // unsupported
    private static final String UNDERLINE = COLOR_CHAR + "n"; // unsupported
    private static final String ITALIC = COLOR_CHAR + "o"; // unsupported
    public static final String RESET = COLOR_CHAR + "r";

    /**
     * ReIndev specific
     */
    public static final String RAINBOW = COLOR_CHAR + "g";
}
