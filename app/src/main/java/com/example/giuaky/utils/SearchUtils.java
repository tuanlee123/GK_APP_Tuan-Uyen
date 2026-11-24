package com.example.giuaky.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class SearchUtils {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SearchUtils() {
        // Prevent instantiation
    }

    public static String removeAccents(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics.replace('đ', 'd').replace('Đ', 'D').toLowerCase().trim();
    }

    public static boolean matches(String target, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        if (target == null) {
            return false;
        }
        String normalizedTarget = removeAccents(target);
        String normalizedQuery = removeAccents(query);
        return normalizedTarget.contains(normalizedQuery);
    }
}
