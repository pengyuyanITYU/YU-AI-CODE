package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * Shared text normalization for document processors.
 */
public final class DocumentTextNormalizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern EXCESSIVE_EMPTY_LINES = Pattern.compile("\\n{3,}");

    private DocumentTextNormalizer() {
    }

    public static NormalizeResult normalizeAndLimit(String rawText, int maxChars, String truncationNote) {
        String normalized = normalize(rawText);
        if (normalized.isEmpty()) {
            return new NormalizeResult("", false, 0);
        }

        boolean truncated = maxChars > 0 && normalized.length() > maxChars;
        String limitedText = truncated ? normalized.substring(0, maxChars) : normalized;
        if (truncated && StrUtil.isNotBlank(truncationNote)) {
            limitedText = limitedText + "\n\n" + truncationNote;
        }

        return new NormalizeResult(limitedText, truncated, normalized.length());
    }

    public static String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }

        String normalized = rawText
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        normalized = CONTROL_CHARS.matcher(normalized).replaceAll("");
        normalized = EXCESSIVE_EMPTY_LINES.matcher(normalized).replaceAll("\n\n");

        return normalized.trim();
    }

    public record NormalizeResult(String text, boolean truncated, int originalLength) {
    }
}
