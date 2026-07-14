package com.SheriffAnya.justRaces.utils;

public class MessageHelper {

    public static String error(String message) {
        return "§c" + message;
    }

    public static String success(String message) {
        return "§a" + message;
    }

    public static String translateColors(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
}
