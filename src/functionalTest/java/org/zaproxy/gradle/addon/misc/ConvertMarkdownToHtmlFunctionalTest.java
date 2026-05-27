/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2026 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.gradle.addon.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class ConvertMarkdownToHtmlFunctionalTest extends FunctionalTest {

    private static final String CONVERT_TASK = ":convertMarkdownToHtml";
    private static final String INPUT_FILE = "input.md";
    private static final String OUTPUT_FILE = "build/output.html";

    @Override
    protected void buildFile(String content) throws Exception {
        super.buildFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml>("convertMarkdownToHtml") {
                    markdown.set(layout.projectDirectory.file("input.md"))
                    html.set(layout.buildDirectory.file("output.html"))
                }
                """
                        + content);
    }

    @BeforeEach
    void setUp() throws Exception {
        buildFile("");
    }

    private void writeMarkdown(String content) throws Exception {
        createFile(content, projectDir.resolve(INPUT_FILE));
    }

    private String readHtml() throws Exception {
        return Files.readString(projectDir.resolve(OUTPUT_FILE));
    }

    @Test
    void shouldSucceedWithMarkdownContent() throws Exception {
        // Given
        writeMarkdown("Hello");

        // When
        BuildResult result = build(CONVERT_TASK);

        // Then
        assertTaskSuccess(result, CONVERT_TASK);
        assertThat(projectDir.resolve(OUTPUT_FILE)).exists();
        assertThat(readHtml()).isEqualTo("<p>Hello</p>\n");
    }

    @Test
    void shouldBeUpToDateOnSubsequentRunWithoutChanges() throws Exception {
        // Given
        writeMarkdown("Hello");
        build(CONVERT_TASK);

        // When
        BuildResult result = build(CONVERT_TASK);

        // Then
        assertThat(result.task(CONVERT_TASK))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void shouldFailWhenMarkdownPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml>("convertMarkdownToHtml") {
                    html.set(layout.buildDirectory.file("output.html"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CONVERT_TASK);

        // Then
        assertTaskFailed(result, CONVERT_TASK);
        assertThat(result.getOutput()).contains("'markdown'");
    }

    @Test
    void shouldFailWhenHtmlPropertyIsNotSet() throws Exception {
        // Given
        writeMarkdown("Hello");
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml>("convertMarkdownToHtml") {
                    markdown.set(layout.projectDirectory.file("input.md"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CONVERT_TASK);

        // Then
        assertTaskFailed(result, CONVERT_TASK);
        assertThat(result.getOutput()).contains("'html'");
    }

    @Test
    void shouldFailWhenBothPropertiesAreNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml>("convertMarkdownToHtml") {
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CONVERT_TASK);

        // Then
        assertTaskFailed(result, CONVERT_TASK);
        assertThat(result.getOutput()).contains("'markdown'");
        assertThat(result.getOutput()).contains("'html'");
    }

    @ParameterizedTest
    @MethodSource("headingMarkdowns")
    void shouldConvertHeadingToHtml(String markdown, String expectedHtml) throws Exception {
        // Given
        writeMarkdown(markdown);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo(expectedHtml);
    }

    private static Stream<Arguments> headingMarkdowns() {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(
                        i ->
                                Arguments.of(
                                        "#".repeat(i) + " Heading",
                                        "<h" + i + ">Heading</h" + i + ">\n"));
    }

    @ParameterizedTest
    @MethodSource("inlineFormattings")
    void shouldConvertInlineFormattingToHtml(String markdown, String expectedHtml)
            throws Exception {
        // Given
        writeMarkdown(markdown);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo(expectedHtml);
    }

    private static Stream<Arguments> inlineFormattings() {
        return Stream.of(
                Arguments.of("**bold**", "<p><strong>bold</strong></p>\n"),
                Arguments.of("*italic*", "<p><em>italic</em></p>\n"),
                Arguments.of("`code`", "<p><code>code</code></p>\n"));
    }

    @Test
    void shouldConvertParagraphToHtml() throws Exception {
        // Given
        writeMarkdown("Text");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo("<p>Text</p>\n");
    }

    @Test
    void shouldConvertBlockquoteToHtml() throws Exception {
        // Given
        writeMarkdown("> Quote");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <blockquote>
                        <p>Quote</p>
                        </blockquote>
                        """);
    }

    @Test
    void shouldConvertHorizontalRuleToHtml() throws Exception {
        // Given
        writeMarkdown("---");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo("<hr />\n");
    }

    @Test
    void shouldConvertLinkToHtml() throws Exception {
        // Given
        writeMarkdown("[text](https://example.com)");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo("<p><a href=\"https://example.com\">text</a></p>\n");
    }

    @Test
    void shouldConvertUnorderedListToHtml() throws Exception {
        // Given
        writeMarkdown("- Item 1\n- Item 2");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                        </ul>
                        """);
    }

    @Test
    void shouldConvertOrderedListToHtml() throws Exception {
        // Given
        writeMarkdown("1. Item 1\n2. Item 2");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <ol>
                        <li>Item 1</li>
                        <li>Item 2</li>
                        </ol>
                        """);
    }

    @Test
    void shouldConvertFencedCodeBlockToHtml() throws Exception {
        // Given
        writeMarkdown("```\ncode\n```");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <pre><code>code
                        </code></pre>
                        """);
    }

    @ParameterizedTest
    @MethodSource("strikethroughSubscripts")
    void shouldConvertStrikethroughSubscriptExtensionToHtml(String markdown, String expectedHtml)
            throws Exception {
        // Given
        writeMarkdown(markdown);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml()).isEqualTo(expectedHtml);
    }

    private static Stream<Arguments> strikethroughSubscripts() {
        return Stream.of(
                Arguments.of("~~strikethrough~~", "<p><del>strikethrough</del></p>\n"),
                Arguments.of("~subscript~", "<p><sub>subscript</sub></p>\n"));
    }

    @Test
    void shouldConvertTableToHtml() throws Exception {
        // Given
        writeMarkdown(
                """
                | H1 | H2 |
                | --- | --- |
                | A  | B  |
                """);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <table>
                        <thead>
                        <tr><th>H1</th><th>H2</th></tr>
                        </thead>
                        <tbody>
                        <tr><td>A</td><td>B</td></tr>
                        </tbody>
                        </table>
                        """);
    }

    @Test
    void shouldConvertTableAppendingMissingColumnsToHtml() throws Exception {
        // Given
        writeMarkdown(
                """
                | H1 | H2 |
                | --- | --- |
                | A  |
                """);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <table>
                        <thead>
                        <tr><th>H1</th><th>H2</th></tr>
                        </thead>
                        <tbody>
                        <tr><td>A</td><td></td></tr>
                        </tbody>
                        </table>
                        """);
    }

    @Test
    void shouldConvertTableDiscardingExtraColumnsToHtml() throws Exception {
        // Given
        writeMarkdown(
                """
                | H1 | H2 |
                | --- | --- |
                | A  | B  | C |
                """);

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <table>
                        <thead>
                        <tr><th>H1</th><th>H2</th></tr>
                        </thead>
                        <tbody>
                        <tr><td>A</td><td>B</td></tr>
                        </tbody>
                        </table>
                        """);
    }

    @Test
    void shouldConvertTaskListToHtml() throws Exception {
        // Given
        writeMarkdown("- [x] Done\n- [ ] Todo");

        // When
        build(CONVERT_TASK);

        // Then
        assertThat(readHtml())
                .isEqualTo(
                        """
                        <ul>
                        <li class="task-list-item"><input type="checkbox" class="task-list-item-checkbox" checked="checked" disabled="disabled" readonly="readonly" />&nbsp;Done</li>
                        <li class="task-list-item"><input type="checkbox" class="task-list-item-checkbox" disabled="disabled" readonly="readonly" />&nbsp;Todo</li>
                        </ul>
                        """);
    }
}
