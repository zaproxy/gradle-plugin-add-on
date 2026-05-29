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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class PrepareAddOnNextDevIterFunctionalTest extends FunctionalTest {

    private static final String PREPARE_NEXT_DEV_ITER_TASK = ":prepareAddOnNextDevIter";
    private static final String CHANGELOG_FILE = "CHANGELOG.md";
    private static final String ADD_ON_VERSION = "1";
    private static final String ADD_ON_ID = "testaddon";

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
                version = "%s"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                """
                                .formatted(ADD_ON_VERSION, ADD_ON_ID)
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

    private String readBuildFile() throws Exception {
        return Files.readString(projectDir.resolve("build.gradle.kts"));
    }

    private BuildResult runPrepareNextDevIter() throws Exception {
        return build(PREPARE_NEXT_DEV_ITER_TASK);
    }

    @Test
    void shouldInsertUnreleasedSection() throws Exception {
        // Given
        writeChangelog(
                """
                ## 1.0.0
                ### Changed
                Entry.
                """);

        // When
        BuildResult result = runPrepareNextDevIter();

        // Then
        assertTaskSuccess(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased


                        ## 1.0.0
                        ### Changed
                        Entry.
                        """);
        assertThat(readBuildFile()).contains("version = \"2\"");
    }

    @Test
    void shouldInsertLinkedUnreleasedSectionWhenUnreleasedLinkIsSet() throws Exception {
        // Given
        createFile(
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
                    unreleasedLink.set("https://example.com/compare/@CURRENT_VERSION@...HEAD")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog("## 1.0.0\n");

        // When
        BuildResult result = runPrepareNextDevIter();

        // Then
        assertTaskSuccess(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [Unreleased]


                        ## 1.0.0

                        [Unreleased]: https://example.com/compare/1...HEAD
                        """);
        assertThat(readBuildFile()).contains("version = \"2\"");
    }

    @Test
    void shouldInsertUnreleasedLinkBeforeExistingVersionLink() throws Exception {
        // Given
        createFile(
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
                    unreleasedLink.set("https://example.com/compare/@CURRENT_VERSION@...HEAD")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog(
                """
                ## 1.0.0

                [1.0.0]: https://old/1.0.0
                """);

        // When
        BuildResult result = runPrepareNextDevIter();

        // Then
        assertTaskSuccess(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## [Unreleased]


                        ## 1.0.0

                        [Unreleased]: https://example.com/compare/1...HEAD
                        [1.0.0]: https://old/1.0.0
                        """);
        assertThat(readBuildFile()).contains("version = \"2\"");
    }

    @ParameterizedTest
    @MethodSource("integerVersionBumps")
    void shouldBumpIntegerVersion(String fromVersion, String toVersion) throws Exception {
        // Given
        createFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "%s"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                """
                        .formatted(fromVersion, ADD_ON_ID),
                projectDir.resolve("build.gradle.kts"));
        writeChangelog("## 1.0.0\n");

        // When
        BuildResult result = runPrepareNextDevIter();

        // Then
        assertTaskSuccess(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(readBuildFile()).contains("version = \"" + toVersion + "\"");
        assertThat(readBuildFile()).doesNotContain("version = \"" + fromVersion + "\"");
    }

    static Stream<Arguments> integerVersionBumps() {
        return Stream.of(Arguments.of("1", "2"), Arguments.of("10", "11"));
    }

    @Test
    void shouldBumpSemanticVersion() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "1.2.3"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                """
                        .formatted(ADD_ON_ID),
                projectDir.resolve("build.gradle.kts"));
        writeChangelog("## 1.2.3\n");

        // When
        BuildResult result = runPrepareNextDevIter();

        // Then
        assertTaskSuccess(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(readBuildFile()).contains("version = \"1.3.0\"");
        assertThat(readBuildFile()).doesNotContain("version = \"1.2.3\"");
    }

    @Test
    void shouldFailWhenUnreleasedSectionAlreadyPresent() throws Exception {
        // Given
        String changelog =
                """
                ## Unreleased

                ## 1.0.0
                """;
        writeChangelog(changelog);
        String buildFile = readBuildFile();

        // When
        BuildResult result = buildAndFail(PREPARE_NEXT_DEV_ITER_TASK);

        // Then
        assertTaskFailed(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(result.getOutput()).contains("already contains the unreleased section");
        assertThat(readChangelog()).isEqualTo(changelog);
        assertThat(readBuildFile()).isEqualTo(buildFile);
    }

    @Test
    void shouldFailWhenChangelogHasNoVersionSection() throws Exception {
        // Given
        String changelog = "# Changelog\n";
        writeChangelog(changelog);
        String buildFile = readBuildFile();

        // When
        BuildResult result = buildAndFail(PREPARE_NEXT_DEV_ITER_TASK);

        // Then
        assertTaskFailed(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(result.getOutput()).contains("no version section found");
        assertThat(readChangelog()).isEqualTo(changelog);
        assertThat(readBuildFile()).isEqualTo(buildFile);
    }

    @Test
    void shouldFailWhenVersionLineNotFoundInBuildFile() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                project.version = "1"
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog("## 1.0.0\n");

        // When
        BuildResult result = buildAndFail(PREPARE_NEXT_DEV_ITER_TASK);

        // Then
        assertTaskFailed(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(result.getOutput()).contains("current version line not found");
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased


                        ## 1.0.0
                        """);
        assertThat(readBuildFile()).contains("project.version = \"1\"");
    }

    @Test
    void shouldFailWhenVersionCannotBeParsed() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "not-supported"
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog("## 1.0.0\n");

        // When
        BuildResult result = buildAndFail(PREPARE_NEXT_DEV_ITER_TASK);

        // Then
        assertTaskFailed(result, PREPARE_NEXT_DEV_ITER_TASK);
        assertThat(result.getOutput()).contains("Failed to parse the current version");
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased


                        ## 1.0.0
                        """);
        assertThat(readBuildFile()).contains("version = \"not-supported\"");
    }
}
