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
package org.zaproxy.gradle.addon.internal.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.zaproxy.gradle.addon.FunctionalTest;

class CopyCommonHelpDataFunctionalTest extends FunctionalTest {

    private static final String COPY_TASK = ":copyHelp";

    private void setupTask(String taskConfig) throws Exception {
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.internal.tasks.CopyCommonHelpData>("copyHelp") {
                %s
                }
                """
                        .formatted(taskConfig),
                projectDir.resolve("build.gradle.kts"));
    }

    private BuildResult runCopyHelp() throws Exception {
        return build(COPY_TASK);
    }

    @Test
    void shouldCopyFileToLocaleDirectory() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));
        createFile("content", projectDir.resolve("help/map.jhm"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/map.jhm")).exists();
    }

    @Test
    void shouldCopyDirectoryContentsToLocaleDirectory() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));
        createFile("image", projectDir.resolve("help/contents/images/logo.png"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/contents/images/logo.png")).exists();
    }

    @Test
    void shouldCopyCommonDataToMultipleLocaleDirectories() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs", "help_ja_JP/helpset_ja_JP.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));
        createFile("", projectDir.resolve("help_ja_JP/helpset_ja_JP.hs"));
        createFile("content", projectDir.resolve("help/map.jhm"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/map.jhm")).exists();
        assertThat(projectDir.resolve("help_ja_JP/map.jhm")).exists();
    }

    @Test
    void shouldNotCopyToMainHelpDirectory() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));
        createFile("content", projectDir.resolve("help/map.jhm"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/map.jhm")).exists();
        assertThat(projectDir.resolve("help/map.jhm")).hasContent("content");
    }

    @Test
    void shouldDoNothingWhenHelpSetsIsEmpty() throws Exception {
        // Given
        setupTask("");

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
    }

    @Test
    void shouldDoNothingWhenCommonDataFilesDoNotExist() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/map.jhm")).doesNotExist();
    }

    @Test
    void shouldFailWhenMainHelpSetNotFound() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help_pt_BR/helpset_pt_BR.hs"))
                """);
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));

        // When
        BuildResult result = buildAndFail(COPY_TASK);

        // Then
        assertTaskFailed(result, COPY_TASK);
        assertThat(result.getOutput()).contains("main HelpSet file not found");
    }

    @Test
    void shouldUseCustomCommonDataList() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("help/helpset.hs", "help_pt_BR/helpset_pt_BR.hs"))
                    commonData.set(listOf("custom.xml"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_pt_BR/helpset_pt_BR.hs"));
        createFile("content", projectDir.resolve("help/custom.xml"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_pt_BR/custom.xml")).exists();
        assertThat(projectDir.resolve("help_pt_BR/map.jhm")).doesNotExist();
    }

    @Test
    void shouldUseCustomFileNameHelpSetPattern() throws Exception {
        // Given
        setupTask(
                """
                    fileNameHelpSetPattern.set("lang_([^.]+)\\\\.hs")
                    helpSets.from(files("help/helpset.hs", "help_de/lang_de.hs"))
                """);
        createFile("", projectDir.resolve("help/helpset.hs"));
        createFile("", projectDir.resolve("help_de/lang_de.hs"));
        createFile("content", projectDir.resolve("help/map.jhm"));

        // When
        BuildResult result = runCopyHelp();

        // Then
        assertTaskSuccess(result, COPY_TASK);
        assertThat(projectDir.resolve("help_de/map.jhm")).exists();
    }
}
