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

import java.nio.file.Files;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class UpdateHelpSetXmlLangAttrFunctionalTest extends FunctionalTest {

    private static final String UPDATE_TASK = ":updateLang";

    private void setupTask(String taskConfig) throws Exception {
        createFile(
                """
                plugins {
                    id("org.zaproxy.add-on")
                }
                tasks.register<org.zaproxy.gradle.addon.internal.tasks.UpdateHelpSetXmlLangAttr>("updateLang") {
                %s
                }
                """
                        .formatted(taskConfig),
                projectDir.resolve("build.gradle.kts"));
    }

    private void writeHelpSet(String relPath, String xmlLang) throws Exception {
        createFile(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <helpset version="2.0" xml:lang="%s">
                </helpset>
                """
                        .formatted(xmlLang),
                projectDir.resolve(relPath));
    }

    private String readHelpSet(String relPath) throws Exception {
        return Files.readString(projectDir.resolve(relPath));
    }

    private BuildResult runUpdateLang() throws Exception {
        return build(UPDATE_TASK);
    }

    static Stream<Arguments> localeReplacements() {
        return Stream.of(
                Arguments.of("pt_BR", "pt-BR"),
                Arguments.of("zh_CN", "zh-CN"),
                Arguments.of("ja_JP", "ja-JP"),
                Arguments.of("ar", "ar"));
    }

    @ParameterizedTest
    @MethodSource("localeReplacements")
    void shouldUpdateXmlLangAttributeForLocaleHelpSet(String locale, String expectedLang)
            throws Exception {
        // Given
        String helpSetFile = "helpset_" + locale + ".hs";
        setupTask(
                """
                    helpSets.from(files("%s"))
                """
                        .formatted(helpSetFile));
        writeHelpSet(helpSetFile, "en");

        // When
        BuildResult result = runUpdateLang();

        // Then
        assertTaskSuccess(result, UPDATE_TASK);
        assertThat(readHelpSet(helpSetFile)).contains("xml:lang=\"" + expectedLang + "\"");
    }

    @Test
    void shouldSkipMainHelpSetWhenFileNameDoesNotMatchPattern() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("helpset.hs"))
                """);
        writeHelpSet("helpset.hs", "en");

        // When
        BuildResult result = runUpdateLang();

        // Then
        assertTaskSuccess(result, UPDATE_TASK);
        assertThat(readHelpSet("helpset.hs")).contains("xml:lang=\"en\"");
    }

    @Test
    void shouldUpdateMultipleLocaleHelpSetsIndependently() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("helpset_pt_BR.hs", "helpset_ja_JP.hs"))
                """);
        writeHelpSet("helpset_pt_BR.hs", "en");
        writeHelpSet("helpset_ja_JP.hs", "en");

        // When
        BuildResult result = runUpdateLang();

        // Then
        assertTaskSuccess(result, UPDATE_TASK);
        assertThat(readHelpSet("helpset_pt_BR.hs")).contains("xml:lang=\"pt-BR\"");
        assertThat(readHelpSet("helpset_ja_JP.hs")).contains("xml:lang=\"ja-JP\"");
    }

    @Test
    void shouldPreserveOtherXmlContent() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("helpset_pt_BR.hs"))
                """);
        createFile(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <helpset version="2.0" xml:lang="en">
                <title>Test Help</title>
                <maps><homeID>overview</homeID></maps>
                </helpset>
                """,
                projectDir.resolve("helpset_pt_BR.hs"));

        // When
        BuildResult result = runUpdateLang();

        // Then
        assertTaskSuccess(result, UPDATE_TASK);
        assertThat(readHelpSet("helpset_pt_BR.hs"))
                .contains("xml:lang=\"pt-BR\"")
                .contains("<title>");
    }

    @Test
    void shouldFailWhenHelpSetFileDoesNotExist() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("helpset_pt_BR.hs"))
                """);

        // When
        BuildResult result = buildAndFail(UPDATE_TASK);

        // Then
        assertTaskFailed(result, UPDATE_TASK);
        assertThat(result.getOutput()).contains("does not exist");
    }

    @Test
    void shouldFailWhenHelpSetHasNoXmlLangAttribute() throws Exception {
        // Given
        setupTask(
                """
                    helpSets.from(files("helpset_pt_BR.hs"))
                """);
        createFile(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <helpset version="2.0">
                </helpset>
                """,
                projectDir.resolve("helpset_pt_BR.hs"));

        // When
        BuildResult result = buildAndFail(UPDATE_TASK);

        // Then
        assertTaskFailed(result, UPDATE_TASK);
        assertThat(result.getOutput()).contains("xml:lang attribute not found");
    }

    @Test
    void shouldUseCustomFileNameLocalePattern() throws Exception {
        // Given
        setupTask(
                """
                    fileNameLocalePattern.set("locale_([^.]+)\\\\.hs")
                    helpSets.from(files("locale_de.hs"))
                """);
        writeHelpSet("locale_de.hs", "en");

        // When
        BuildResult result = runUpdateLang();

        // Then
        assertTaskSuccess(result, UPDATE_TASK);
        assertThat(readHelpSet("locale_de.hs")).contains("xml:lang=\"de\"");
    }
}
