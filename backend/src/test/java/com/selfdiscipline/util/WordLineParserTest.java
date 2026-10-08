package com.selfdiscipline.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 词库行解析的契约测试（Issue #3 Bug 4：词库需要支持例句与音标）。
 */
class WordLineParserTest {

    @Test
    void parsesAllFourFields() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "They had to abandon the car.", "/əˈbæn.dən/"},
                WordLineParser.parse("abandon|放弃|They had to abandon the car.|/əˈbæn.dən/"));
    }

    @Test
    void keepsLegacyTwoFieldFormat() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "", ""},
                WordLineParser.parse("abandon|放弃"));
    }

    @Test
    void keepsLegacyBareWordFormat() {
        assertArrayEquals(
                new String[]{"abandon", "", "", ""},
                WordLineParser.parse("abandon"));
    }

    @Test
    void padsMissingTrailingFieldsWithEmptyStrings() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "", ""},
                WordLineParser.parse("abandon|放弃||"));
    }

    @Test
    void supportsThreeFieldFormat() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "They had to abandon the car.", ""},
                WordLineParser.parse("abandon|放弃|They had to abandon the car."));
    }

    @Test
    void supportsTabAndCommaSeparators() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "", ""},
                WordLineParser.parse("abandon\t放弃"));
        assertArrayEquals(
                new String[]{"abandon", "放弃", "", ""},
                WordLineParser.parse("abandon,放弃"));
    }

    @Test
    void trimsWhitespaceAroundFields() {
        assertArrayEquals(
                new String[]{"abandon", "放弃", "sample", "/əˈbæn.dən/"},
                WordLineParser.parse("  abandon | 放弃 | sample | /əˈbæn.dən/  "));
    }

    @Test
    void extraPipesStayInTheLastFieldInsteadOfThrowing() {
        String[] fields = WordLineParser.parse("a|b|c|d|e");
        assertEquals(WordLineParser.FIELD_COUNT, fields.length);
        assertEquals("a", fields[0]);
        assertEquals("d|e", fields[3]);
    }

    @Test
    void nullAndBlankLinesYieldEmptyFields() {
        assertArrayEquals(new String[]{"", "", "", ""}, WordLineParser.parse(null));
        assertArrayEquals(new String[]{"", "", "", ""}, WordLineParser.parse(""));
        assertArrayEquals(new String[]{"", "", "", ""}, WordLineParser.parse("   "));
    }

    @Test
    void alwaysReturnsExactlyFourNonNullFields() {
        for (String line : new String[]{"a", "a|b", "a|b|c", "a|b|c|d", "a|b|c|d|e", "|", "|||"}) {
            String[] fields = WordLineParser.parse(line);
            assertEquals(WordLineParser.FIELD_COUNT, fields.length, "line=" + line);
            for (String field : fields) {
                assertEquals(false, field == null, "line=" + line);
            }
        }
    }
}
