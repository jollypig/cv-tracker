package com.example.cv.importing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlCvParserTest {

    private final HtmlCvParser parser = new HtmlCvParser(10 * 1024 * 1024);

    @Test
    void extractsHeadingsParagraphsListsTablesAndLinks() {
        String html = """
                <html><body>
                <h1>Ada Lovelace</h1>
                <p>Engineer &amp; mathematician</p>
                <ul><li>Analytical Engine</li></ul>
                <table><tr><th>Role</th><td></td><td>Programmer</td></tr></table>
                <a href="https://example.com/profile">Profile</a>
                </body></html>
                """;

        NormalizedCvDocument document = parser.parse(html.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8");

        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::type)
                .containsExactly(
                        NormalizedCvBlock.Type.HEADING,
                        NormalizedCvBlock.Type.PARAGRAPH,
                        NormalizedCvBlock.Type.LIST_ITEM,
                        NormalizedCvBlock.Type.TABLE_ROW,
                        NormalizedCvBlock.Type.LINK);
        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::text)
                .containsExactly(
                        "Ada Lovelace",
                        "Engineer & mathematician",
                        "Analytical Engine",
                        "Role | | Programmer",
                        "Profile");
        assertThat(document.blocks().get(4).link()).isEqualTo("https://example.com/profile");
    }

    @Test
    void rejectsUnsupportedMediaType() {
        assertThatThrownBy(() -> parser.parse(new byte[]{1}, "text/plain"))
                .isInstanceOf(HtmlCvParserException.class)
                .extracting(exception -> ((HtmlCvParserException) exception).reason())
                .isEqualTo(HtmlCvParserException.Reason.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsEmptyAndOversizedHtml() {
        assertThatThrownBy(() -> parser.parse(new byte[0], "text/html"))
                .isInstanceOf(HtmlCvParserException.class)
                .extracting(exception -> ((HtmlCvParserException) exception).reason())
                .isEqualTo(HtmlCvParserException.Reason.INVALID_HTML);
        assertThatThrownBy(() -> new HtmlCvParser(1).parse(new byte[]{1, 2}, "text/html"))
                .isInstanceOf(HtmlCvParserException.class)
                .extracting(exception -> ((HtmlCvParserException) exception).reason())
                .isEqualTo(HtmlCvParserException.Reason.FILE_TOO_LARGE);
    }
}