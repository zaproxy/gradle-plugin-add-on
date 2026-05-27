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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.AddOnStatus;
import org.zaproxy.gradle.addon.FunctionalTest;

class CopyAddOnFunctionalTest extends FunctionalTest {

    private static final String COPY_ADD_ON_TASK = ":copyZapAddOn";
    private static final String ADD_ON_ID = "testaddon";
    private static final String ADD_ON_VERSION = "1";
    private static final String ADD_ON_FILE =
            ADD_ON_ID + "-" + AddOnStatus.ALPHA + "-" + ADD_ON_VERSION + ".zap";

    private Path destDir;

    @BeforeEach
    void setup() throws Exception {
        destDir = Files.createDirectories(projectDir.resolve("dest"));
    }

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

    @Test
    void shouldCopyAddOnToDefaultDirectory() throws Exception {
        // Given
        Path nestedProjectDir = projectDir.resolve("project");
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
                        .formatted(ADD_ON_VERSION, ADD_ON_ID),
                nestedProjectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(nestedProjectDir.toFile())
                        .withArguments(COPY_ADD_ON_TASK)
                        .withPluginClasspath()
                        .build();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        Path defaultDest = projectDir.resolve("zaproxy/zap/src/main/dist/plugin");
        assertThat(defaultDest.resolve(ADD_ON_FILE)).exists();
    }

    @Test
    void shouldCopyAddOnToSpecifiedDirectory() throws Exception {
        // Given
        buildFile("");

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        assertThat(destDir.resolve(ADD_ON_FILE)).exists();
    }

    @ParameterizedTest
    @MethodSource("existingAddOnVersionFileNames")
    void shouldDeleteExistingAddOnsWithSameIdBeforeCopying(List<String> existingFileNames)
            throws Exception {
        // Given
        buildFile("");

        for (String name : existingFileNames) {
            createAddOnFile(destDir, name);
        }

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        for (String name : existingFileNames) {
            assertThat(destDir.resolve(name)).doesNotExist();
        }
        assertThat(destDir.resolve(ADD_ON_FILE)).exists();
    }

    static Stream<Arguments> existingAddOnVersionFileNames() {
        return Stream.of(
                Arguments.of(List.of(ADD_ON_ID + "-0.zap")),
                Arguments.of(List.of(ADD_ON_ID + "-alpha-2.zap", ADD_ON_ID + "-beta-1.0.0.zap")));
    }

    @Test
    void shouldNotDeleteAddOnsWithDifferentIdBeforeCopying() throws Exception {
        // Given
        buildFile("");

        String unrelatedFile = "otheraddon-1.zap";
        createAddOnFile(destDir, unrelatedFile);

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        assertThat(destDir.resolve(unrelatedFile)).exists();
        assertThat(destDir.resolve(ADD_ON_FILE)).exists();
    }

    @Test
    void shouldBeUpToDateWhenAddOnAlreadyCopied() throws Exception {
        // Given
        buildFile("");
        runCopyAddOn();

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskUpToDate(result, COPY_ADD_ON_TASK);
    }

    @Test
    void shouldNotBeUpToDateWhenMultipleAddOnsWithSameIdExistInDestination() throws Exception {
        // Given
        buildFile("");
        runCopyAddOn();
        String otherVersionAddOnFile = ADD_ON_ID + "-99.zap";
        createAddOnFile(destDir, otherVersionAddOnFile);

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        assertThat(destDir.resolve(otherVersionAddOnFile)).doesNotExist();
        assertThat(destDir.resolve(ADD_ON_FILE)).exists();
    }

    @Test
    void shouldUsedefaultAddOnId() throws Exception {
        // Given
        String customId = "custom-addon";
        settingsFile("rootProject.name = \"%s\"".formatted(customId));
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
                    addOnName.set("Test Add-On")
                }
                """
                        .formatted(ADD_ON_VERSION));

        createAddOnFile(destDir, customId + "-0.zap");

        // When
        BuildResult result = runCopyAddOn();

        // Then
        assertTaskSuccess(result, COPY_ADD_ON_TASK);
        assertThat(destDir.resolve(customId + "-0.zap")).doesNotExist();
        assertThat(
                        destDir.resolve(
                                customId + "-" + AddOnStatus.ALPHA + "-" + ADD_ON_VERSION + ".zap"))
                .exists();
    }

    private static void createAddOnFile(Path dir, String fileName) throws IOException {
        Files.createFile(dir.resolve(fileName));
    }

    private BuildResult runCopyAddOn() throws Exception {
        return build(COPY_ADD_ON_TASK, "--into", destDir.toString());
    }
}
