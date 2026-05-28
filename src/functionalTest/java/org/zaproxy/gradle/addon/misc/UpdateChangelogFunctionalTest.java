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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class UpdateChangelogFunctionalTest extends FunctionalTest {

    private static final String UPDATE_CHANGELOG_TASK = ":updateChangelog";
    private static final String CUSTOM_TASK = ":customUpdateChangelog";
    private static final String CHANGELOG_FILE = "CHANGELOG.md";

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
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
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

    private BuildResult runUpdateChangelog(String... extraArgs) throws Exception {
        String[] args = new String[1 + extraArgs.length];
        args[0] = UPDATE_CHANGELOG_TASK;
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        return build(args);
    }

    @Test
    void shouldAddChangeWithDefaultChangeType() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);

        // When
        BuildResult result = runUpdateChangelog("--change", "New change.");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        New change.

                        ## 1.0.0
                        """);
    }

    @ParameterizedTest
    @EnumSource(UpdateChangelog.ChangeType.class)
    void shouldAddChangeToExistingSection(UpdateChangelog.ChangeType changeType) throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased
                %s
                ## 1.0.0
                """
                        .formatted(changeType.markdown()));

        // When
        BuildResult result =
                runUpdateChangelog("--change-type", changeType.name(), "--change", "Entry.");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        %sEntry.

                        ## 1.0.0
                        """
                                .formatted(changeType.markdown()));
    }

    @ParameterizedTest
    @EnumSource(UpdateChangelog.ChangeType.class)
    void shouldCreateSectionAndAddChangeWhenNotPresent(UpdateChangelog.ChangeType changeType)
            throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);

        // When
        BuildResult result =
                runUpdateChangelog("--change-type", changeType.name(), "--change", "Entry.");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        %sEntry.

                        ## 1.0.0
                        """
                                .formatted(changeType.markdown()));
    }

    @Test
    void shouldInsertChangeAtTopOfSection() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased
                ### Changed
                Old entry.

                ## 1.0.0
                """);

        // When
        runUpdateChangelog("--change", "New entry.");

        // Then
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        New entry.
                        Old entry.

                        ## 1.0.0
                        """);
    }

    @Test
    void shouldPreserveContentBeforeVersionSection() throws Exception {
        // Given
        writeChangelog(
                """
                # Changelog
                Text abc.

                ## Unreleased

                ## 1.0.0
                """);

        // When
        runUpdateChangelog("--change", "New change.");

        // Then
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        # Changelog
                        Text abc.

                        ## Unreleased
                        ### Changed
                        New change.

                        ## 1.0.0
                        """);
    }

    @Test
    void shouldOnlyModifyFirstVersionSection() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                ### Changed
                V1 change.
                """);

        // When
        runUpdateChangelog("--change", "New change.");

        // Then
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        New change.

                        ## 1.0.0
                        ### Changed
                        V1 change.
                        """);
    }

    @Test
    void shouldAddChangeWhenSingleVersionSection() throws Exception {
        // Given
        writeChangelog("## Unreleased\n");

        // When
        BuildResult result = runUpdateChangelog("--change", "New change.");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        New change.

                        """);
    }

    @Test
    void shouldNotAddDuplicateChangeByDefault() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);
        runUpdateChangelog("--change", "Entry.");

        // When
        BuildResult result = runUpdateChangelog("--change", "Entry.");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        Entry.

                        ## 1.0.0
                        """);
    }

    @Test
    void shouldAddDuplicateChangeWhenAllowed() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);
        runUpdateChangelog("--change", "Entry.");

        // When
        BuildResult result = runUpdateChangelog("--change", "Entry.", "--duplicate", "true");

        // Then
        assertTaskSuccess(result, UPDATE_CHANGELOG_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        Entry.
                        Entry.

                        ## 1.0.0
                        """);
    }

    @ParameterizedTest
    @MethodSource("sectionHeaderCasings")
    void shouldMatchExistingSectionCaseInsensitively(
            UpdateChangelog.ChangeType changeType, String sectionHeader) throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased
                %s

                ## 1.0.0
                """
                        .formatted(sectionHeader));

        // When
        runUpdateChangelog("--change-type", changeType.name(), "--change", "Entry.");

        // Then
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        %s
                        Entry.

                        ## 1.0.0
                        """
                                .formatted(sectionHeader));
    }

    static Stream<Arguments> sectionHeaderCasings() {
        return Stream.of(UpdateChangelog.ChangeType.values())
                .flatMap(
                        type -> {
                            String capitalized = type.markdown().strip().substring(4);
                            return Stream.of(
                                    Arguments.of(type, "### " + capitalized.toLowerCase()),
                                    Arguments.of(type, "### " + type.name()),
                                    Arguments.of(type, "###" + capitalized));
                        });
    }

    @Test
    void shouldNotAccumulateBlankLinesOnSuccessiveRuns() throws Exception {
        // Given
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);
        runUpdateChangelog("--change", "Entry 1.");

        // When
        runUpdateChangelog("--change", "Entry 2.");

        // Then
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        Entry 2.
                        Entry 1.

                        ## 1.0.0
                        """);
    }

    @Test
    void shouldFailWhenChangeOptionIsNotProvided() throws Exception {
        // Given
        String changelog =
                """
                ## Unreleased

                ## 1.0.0
                """;
        writeChangelog(changelog);

        // When
        BuildResult result = buildAndFail(UPDATE_CHANGELOG_TASK);

        // Then
        assertTaskFailed(result, UPDATE_CHANGELOG_TASK);
        assertThat(result.getOutput()).contains("no value available");
        assertThat(readChangelog()).isEqualTo(changelog);
    }

    @Test
    void shouldFailWhenChangelogHasNoVersionSection() throws Exception {
        // Given
        String changelog =
                """
                # Changelog
                No version here.
                """;
        writeChangelog(changelog);

        // When
        BuildResult result = buildAndFail(UPDATE_CHANGELOG_TASK, "--change", "Entry.");

        // Then
        assertTaskFailed(result, UPDATE_CHANGELOG_TASK);
        assertThat(result.getOutput()).contains("does not have a version section");
        assertThat(readChangelog()).isEqualTo(changelog);
    }

    @Test
    void shouldAddChangeToCustomChangelogFile() throws Exception {
        // Given
        String customFile = "custom.md";
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.UpdateChangelog>("customUpdateChangelog") {
                    changelog.set(layout.projectDirectory.file("%s"))
                    change.set("Entry.")
                }
                """
                        .formatted(customFile),
                projectDir.resolve("build.gradle.kts"));
        createFile(
                """
                ## Unreleased

                ## 1.0.0
                """,
                projectDir.resolve(customFile));

        // When
        BuildResult result = build(CUSTOM_TASK);

        // Then
        assertTaskSuccess(result, CUSTOM_TASK);
        assertThat(Files.readString(projectDir.resolve(customFile)))
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        Entry.

                        ## 1.0.0
                        """);
    }

    @ParameterizedTest
    @EnumSource(UpdateChangelog.ChangeType.class)
    void shouldAddChangeWithChangeTypeProperty(UpdateChangelog.ChangeType changeType)
            throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.UpdateChangelog>("customUpdateChangelog") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    changeType.set(org.zaproxy.gradle.addon.misc.UpdateChangelog.ChangeType.%s)
                    change.set("Entry.")
                }
                """
                        .formatted(changeType.name()),
                projectDir.resolve("build.gradle.kts"));
        writeChangelog(
                """
                ## Unreleased

                ## 1.0.0
                """);

        // When
        BuildResult result = build(CUSTOM_TASK);

        // Then
        assertTaskSuccess(result, CUSTOM_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        %sEntry.

                        ## 1.0.0
                        """
                                .formatted(changeType.markdown()));
    }

    @Test
    void shouldAddDuplicateWithDuplicateProperty() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.UpdateChangelog>("customUpdateChangelog") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                    change.set("Entry.")
                    duplicate.set(true)
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeChangelog(
                """
                ## Unreleased
                ### Changed
                Entry.

                ## 1.0.0
                """);

        // When
        BuildResult result = build(CUSTOM_TASK);

        // Then
        assertTaskSuccess(result, CUSTOM_TASK);
        assertThat(readChangelog())
                .isEqualTo(
                        """
                        ## Unreleased
                        ### Changed
                        Entry.
                        Entry.

                        ## 1.0.0
                        """);
    }

    @Test
    void shouldFailWhenChangePropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.UpdateChangelog>("customUpdateChangelog") {
                    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        String changelog =
                """
                ## Unreleased

                ## 1.0.0
                """;
        writeChangelog(changelog);

        // When
        BuildResult result = buildAndFail(CUSTOM_TASK);

        // Then
        assertTaskFailed(result, CUSTOM_TASK);
        assertThat(result.getOutput()).contains("no value available");
        assertThat(readChangelog()).isEqualTo(changelog);
    }

    @Test
    void shouldFailWhenChangelogPropertyIsNotSet() throws Exception {
        // Given
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.misc.UpdateChangelog>("customUpdateChangelog") {
                    change.set("Entry.")
                }
                """,
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = buildAndFail(CUSTOM_TASK);

        // Then
        assertTaskFailed(result, CUSTOM_TASK);
        assertThat(result.getOutput()).contains("no value available");
    }
}
