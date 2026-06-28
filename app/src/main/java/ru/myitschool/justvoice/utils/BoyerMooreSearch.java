package ru.myitschool.justvoice.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoyerMooreSearch {

    public static List<Integer> search(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();

        if (text == null || pattern == null ||
                text.isEmpty() || pattern.isEmpty() ||
                pattern.length() > text.length()) {
            return positions;
        }

        char[] textChars = text.toCharArray();
        char[] patternChars = pattern.toCharArray();
        int n = textChars.length;
        int m = patternChars.length;

        Map<Character, Integer> badChar = buildBadCharTable(patternChars);
        int[] goodSuffix = buildGoodSuffixTable(patternChars);

        int i = 0;

        while (i <= n - m) {
            int j = m - 1;

            while (j >= 0 && patternChars[j] == textChars[i + j]) {
                j--;
            }

            if (j < 0) {
                positions.add(i);
                i += (i + m < n) ? m - badChar.getOrDefault(textChars[i + m], -1) : 1;
            } else {
                int badCharShift = j - badChar.getOrDefault(textChars[i + j], -1);
                int goodSuffixShift = goodSuffix[j];

                i += Math.max(badCharShift, goodSuffixShift);
                i = Math.max(i, 1);
            }
        }

        return positions;
    }

    private static Map<Character, Integer> buildBadCharTable(char[] pattern) {
        Map<Character, Integer> badChar = new HashMap<>();

        for (int i = 0; i < pattern.length; i++) {
            badChar.put(pattern[i], i);
        }

        return badChar;
    }

    private static int[] buildGoodSuffixTable(char[] pattern) {
        int m = pattern.length;
        int[] goodSuffix = new int[m];
        int[] suffix = new int[m];

        suffix[m - 1] = m;
        int g = m - 1;
        int f = 0;

        for (int i = m - 2; i >= 0; i--) {
            if (i > g && suffix[i + m - 1 - f] < i - g) {
                suffix[i] = suffix[i + m - 1 - f];
            } else {
                if (i < g) {
                    g = i;
                }
                f = i;
                while (g >= 0 && pattern[g] == pattern[g + m - 1 - f]) {
                    g--;
                }
                suffix[i] = f - g;
            }
        }

        for (int i = 0; i < m; i++) {
            goodSuffix[i] = m;
        }

        for (int i = m - 1; i >= 0; i--) {
            if (suffix[i] == i + 1) {
                for (int j = 0; j < m - 1 - i; j++) {
                    if (goodSuffix[j] == m) {
                        goodSuffix[j] = m - 1 - i;
                    }
                }
            }
        }

        for (int i = 0; i < m - 1; i++) {
            goodSuffix[m - 1 - suffix[i]] = m - 1 - i;
        }

        return goodSuffix;
    }

    public static List<SearchResult> searchWithContext(String text, String pattern, int contextChars) {
        List<SearchResult> results = new ArrayList<>();
        List<Integer> positions = search(text.toLowerCase(), pattern.toLowerCase());

        for (int pos : positions) {
            int start = Math.max(0, pos - contextChars);
            int end = Math.min(text.length(), pos + pattern.length() + contextChars);

            String context = text.substring(start, end);
            if (start > 0) context = "..." + context;
            if (end < text.length()) context = context + "...";

            results.add(new SearchResult(pos, context));
        }

        return results;
    }

    public static String highlightMatches(String text, String pattern, String highlightStart, String highlightEnd) {
        if (text == null || pattern == null) return text;

        List<Integer> positions = search(text.toLowerCase(), pattern.toLowerCase());
        if (positions.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        for (int pos : positions) {
            result.append(text, lastEnd, pos);
            result.append(highlightStart);
            result.append(text, pos, pos + pattern.length());
            result.append(highlightEnd);
            lastEnd = pos + pattern.length();
        }

        result.append(text.substring(lastEnd));
        return result.toString();
    }

    public static class SearchResult {
        private final int position;
        private final String context;

        public SearchResult(int position, String context) {
            this.position = position;
            this.context = context;
        }

        public int getPosition() { return position; }
        public String getContext() { return context; }
    }
}