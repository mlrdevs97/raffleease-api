package com.raffleease.raffleease.Common.Utils;

public class SanitizeUtils {
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }
    public static String trimAndLower(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}