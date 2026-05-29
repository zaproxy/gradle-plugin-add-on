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
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.gradle.addon.FunctionalTest;

class PrepareAddOnReleaseFunctionalTest extends FunctionalTest {

    private static final String PREPARE_RELEASE_TASK = ":prepareRelease";
    private static final String CHANGELOG_FILE = "CHANGELOG.md";

    @Override
    protected void buildFile(String content) throws Exception {
        super.buildFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    currentVersion.set("1.0.0")
                    releaseLink.set("https://example.com/releases/@CURRENT_VERSION@")
                    releaseDate.set("2026-01-01")
                }
                """
                        + content);
    }

    @BeforeEach
    void setUp() throws Exception {
        buildFile("");
    }

    private void writeChangelog(String content) throws Exception {
        createFile(content, projectDir.resolve(CHANGELOG_FILE));
    }

    private String readChangelog() throws Exception {
        return Files.readString(projectDir.resolve(CHANGELOG_FILE));
    }

    private BuildResult runPrepareRelease() throws Exception {
        return build(PREPARE_RELEASE_TASK);
    }

    @Test
    void shouldReplaceUnreleasedSectionHeader() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldReplaceLinkedUnreleasedSectionHeader() throws Exception {
        // Given
        writeChangelog(
                """
                ## [Unreleased]

                ## [0.9.0] - 2025-12-01
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldAddReleaseLinkReplacingCurrentVersionToken() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog()).contains("[1.0.0]: https://example.com/releases/1.0.0");
    }

    @Test
    void shouldAddReleaseLinkReplacingBothVersionTokens() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    currentVersion.set("1.0.0")
                    releaseLink.set("https://example.com/compare/@PREVIOUS_VERSION@...@CURRENT_VERSION@")
                    releaseDate.set("2026-01-01")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/compare/0.9.0...1.0.0
                        """);
    }

    @Test
    void shouldReplaceExistingUnreleasedLink() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01

                [Unreleased]: https://old/compare/0.9.0...HEAD
                [0.9.0]: https://old/0.9.0
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        [0.9.0]: https://old/0.9.0
                        """);
    }

    @Test
    void shouldInsertReleaseLinkBeforeExistingVersionLink() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01

                [0.9.0]: https://old/0.9.0
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        [0.9.0]: https://old/0.9.0
                        """);
    }

    @Test
    void shouldAppendReleaseLinkWhenLastLineIsEmpty() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01

                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldAppendReleaseLinkWithBlankLineWhenLastLineIsNotEmpty() throws Exception {
        // Given
        writeChangelog("## Unreleased\n\n## [0.9.0] - 2025-12-01\n");

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldPreserveContentInUnreleasedSection() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased
                ### Changed
                Some change.

                ## [0.9.0] - 2025-12-01
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01
                        ### Changed
                        Some change.

                        ## [0.9.0] - 2025-12-01

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldOnlyReplaceUnreleasedSectionNotSubsequentVersions() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## [0.9.0] - 2025-12-01
                ### Changed
                Old change.

                ## [0.8.0] - 2025-01-01
                ### Fixed
                Old fix.
                """);

        // When
        BuildResult result = runPrepareRelease();

        // Then
        assertTaskSuccess(result, PREPARE_RELEASE_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [1.0.0] - 2026-01-01

                        ## [0.9.0] - 2025-12-01
                        ### Changed
                        Old change.

                        ## [0.8.0] - 2025-01-01
                        ### Fixed
                        Old fix.

                        [1.0.0]: https://example.com/releases/1.0.0
                        """);
    }

    @Test
    void shouldFailWhenChangelogHasNoUnreleasedSection() throws Exception {
        // Given
        String changelog =
                """
                ## [0.9.0] - 2025-12-01
                ### Changed
                Something.
                """;
        writeChangelog(changelog);

        // When
        BuildResult result = buildAndFail(PREPARE_RELEASE_TASK);

        // Then
        assertTaskFailed(result, PREPARE_RELEASE_TASK);
        assertThat(result.getOutput()).contains("unreleased section");
        assertThat(readChangelog()).isEqualTo(changelog);
    }

    @Test
    void shouldFailWhenPreviousVersionTokenUsedButNoReleasedVersion() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    currentVersion.set("1.0.0")
                    releaseLink.set("https://example.com/compare/@PREVIOUS_VERSION@...@CURRENT_VERSION@")
                    releaseDate.set("2026-01-01")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        String changelog = "## Unreleased\n";
        writeChangelog(changelog);

        // When
        BuildResult result = buildAndFail(PREPARE_RELEASE_TASK);

        // Then
        assertTaskFailed(result, PREPARE_RELEASE_TASK);
        assertThat(result.getOutput()).contains("released version");
        assertThat(readChangelog()).isEqualTo(changelog);
    }

    @Test
    void shouldFailWhenCurrentVersionPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    releaseLink.set("https://example.com/releases/1.0.0")
                    releaseDate.set("2026-01-01")
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(PREPARE_RELEASE_TASK);

        // Then
        assertTaskFailed(result, PREPARE_RELEASE_TASK);
        assertThat(result.getOutput()).contains("'currentVersion'");
    }

    @Test
    void shouldFailWhenReleaseLinkPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    currentVersion.set("1.0.0")
                    releaseDate.set("2026-01-01")
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(PREPARE_RELEASE_TASK);

        // Then
        assertTaskFailed(result, PREPARE_RELEASE_TASK);
        assertThat(result.getOutput()).contains("'releaseLink'");
    }

    @Test
    void shouldFailWhenChangelogPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.PrepareAddOnRelease>("prepareRelease") {
                    currentVersion.set("1.0.0")
                    releaseLink.set("https://example.com/releases/1.0.0")
                    releaseDate.set("2026-01-01")
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(PREPARE_RELEASE_TASK);

        // Then
        assertTaskFailed(result, PREPARE_RELEASE_TASK);
        assertThat(result.getOutput()).contains("'changelog'");
    }
}
