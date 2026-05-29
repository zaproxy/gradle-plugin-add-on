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
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class ExtractLatestChangesFromChangelogFunctionalTest extends FunctionalTest {

    private static final String EXTRACT_TASK = ":extractLatestChanges";
    private static final String CUSTOM_EXTRACT_TASK = ":customExtractLatestChanges";
    private static final String CHANGELOG_FILE = "CHANGELOG.md";
    private static final String LATEST_CHANGES_FILE = "build/zapAddOn/latest-changes.md";

    @Override
    protected void buildFile(String content) throws Exception {
        super.buildFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "1"
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
                }
                """
                        + content);
    }

    private void writeChangelog(String content) throws Exception {
        createFile(content, projectDir.resolve(CHANGELOG_FILE));
    }

    private String readLatestChanges() throws Exception {
        return Files.readString(projectDir.resolve(LATEST_CHANGES_FILE));
    }

    @ParameterizedTest
    @MethodSource("changelogs")
    void shouldExtractLatestChanges(String changelog, String expectedChanges) throws Exception {
        // Given
        writeChangelog(changelog);
        buildFile("");

        // When
        BuildResult result = build(EXTRACT_TASK);

        // Then
        assertTaskSuccess(result, EXTRACT_TASK);
        assertThat(readLatestChanges()).isEqualTo(expectedChanges);
    }

    static Stream<Arguments> changelogs() {
        String preamble =
                """
                # Changelog

                ## [Unreleased]
                ### Added
                - New feature.

                ### Fixed
                - Fixed a bug.
                """;
        String expectedUnreleased =
                """
                ### Added
                - New feature.

                ### Fixed
                - Fixed a bug.""";
        return Stream.of(
                Arguments.of(
                        preamble
                                + """

                                ## [1.0.0] - 2024-01-01
                                ### Added
                                - Initial release.
                                """,
                        expectedUnreleased),
                Arguments.of(
                        preamble
                                + """

                                [Unreleased]: https://github.com/example
                                """,
                        expectedUnreleased),
                Arguments.of(preamble, expectedUnreleased),
                Arguments.of(
                        """
                        # Changelog

                        ## 1.0.0
                        ### Added
                        - Released feature.

                        ## 0.9.0
                        ### Added
                        - Old feature.
                        """,
                        """
                        ### Added
                        - Released feature."""));
    }

    @Test
    void shouldBeUpToDateOnSubsequentRunWithoutChanges() throws Exception {
        // Given
        writeChangelog(
                """
                ## [Unreleased]
                - Change.
                """);
        buildFile("");
        build(EXTRACT_TASK);

        // When
        BuildResult result = build(EXTRACT_TASK);

        // Then
        assertTaskUpToDate(result, EXTRACT_TASK);
    }

    @Test
    void shouldFailWhenChangelogIsEmpty() throws Exception {
        // Given
        writeChangelog("");
        buildFile("");

        // When
        BuildResult result = buildAndFail(EXTRACT_TASK);

        // Then
        assertTaskFailed(result, EXTRACT_TASK);
        assertThat(result.getOutput()).contains("Failed to read any characters from:");
    }

    @Test
    void shouldFailWhenChangelogHasNoVersionHeader() throws Exception {
        // Given
        writeChangelog("No version header here.");
        buildFile("");

        // When
        BuildResult result = buildAndFail(EXTRACT_TASK);

        // Then
        assertTaskFailed(result, EXTRACT_TASK);
        assertThat(result.getOutput()).contains("No version matching");
    }

    @Test
    void shouldUseSpecifedProperties() throws Exception {
        // Given
        createFile(
                """
                ## [Unreleased]
                - Custom change.
                """,
                projectDir.resolve("docs/CHANGES.md"));
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog>("customExtractLatestChanges") {
                    changelog.set(layout.projectDirectory.file("docs/CHANGES.md"))
                    latestChanges.set(layout.buildDirectory.file("out/latest.md"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = build(CUSTOM_EXTRACT_TASK);

        // Then
        assertTaskSuccess(result, CUSTOM_EXTRACT_TASK);
        assertThat(projectDir.resolve("build/out/latest.md")).hasContent("- Custom change.");
        assertThat(projectDir.resolve(LATEST_CHANGES_FILE)).doesNotExist();
    }

    @Test
    void shouldFailWhenChangelogPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog>("customExtractLatestChanges") {
                    latestChanges.set(layout.buildDirectory.file("latest.md"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CUSTOM_EXTRACT_TASK);

        // Then
        assertTaskFailed(result, CUSTOM_EXTRACT_TASK);
        assertThat(result.getOutput()).contains("'changelog'");
    }

    @Test
    void shouldFailWhenLatestChangesPropertyIsNotSet() throws Exception {
        // Given
        writeChangelog(
                """
                ## [Unreleased]
                - Change.
                """);
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }

                tasks.register<org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog>("customExtractLatestChanges") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CUSTOM_EXTRACT_TASK);

        // Then
        assertTaskFailed(result, CUSTOM_EXTRACT_TASK);
        assertThat(result.getOutput()).contains("'latestChanges'");
    }
}
