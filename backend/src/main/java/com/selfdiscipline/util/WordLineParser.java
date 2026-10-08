package com.selfdiscipline.util;

/**
 * 词库行解析：把一行文本切成 4 段 —— 单词 | 中文翻译 | 例句 | 音标。
 *
 * <p>设计要点（Issue #3 Bug 4）：
 * <ul>
 *   <li>历史格式 {@code 单词} 和 {@code 单词|翻译} 必须继续可用，因此缺失的段一律回填空串，
 *       只按实际出现的分隔符切分；</li>
 *   <li>分隔符兼容 {@code |}、Tab、逗号，优先级同上；</li>
 *   <li>切分上限为 4，多余的 {@code |} 会保留在最后一段里，不会抛异常；</li>
 *   <li>{@code #} 开头的注释行由调用方跳过（词库文件用注释写数据来源与署名）。</li>
 * </ul>
 */
public final class WordLineParser {

    /** 支持的字段数：单词、翻译、例句、音标。 */
    public static final int FIELD_COUNT = 4;

    private WordLineParser() {
    }

    /**
     * @param line 原始行，允许为 null 或带首尾空白
     * @return 长度恒为 {@value #FIELD_COUNT} 的数组，缺失的段为空串（非 null）
     */
    public static String[] parse(String line) {
        String[] fields = new String[FIELD_COUNT];
        for (int i = 0; i < FIELD_COUNT; i++) {
            fields[i] = "";
        }
        if (line == null) {
            return fields;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return fields;
        }

        String[] parts;
        if (trimmed.contains("|")) {
            parts = trimmed.split("\\|", FIELD_COUNT);
        } else if (trimmed.contains("\t")) {
            parts = trimmed.split("\\t", FIELD_COUNT);
        } else if (trimmed.contains(",")) {
            parts = trimmed.split(",", FIELD_COUNT);
        } else {
            parts = new String[]{trimmed};
        }

        for (int i = 0; i < parts.length && i < FIELD_COUNT; i++) {
            fields[i] = parts[i] == null ? "" : parts[i].trim();
        }
        return fields;
    }
}
