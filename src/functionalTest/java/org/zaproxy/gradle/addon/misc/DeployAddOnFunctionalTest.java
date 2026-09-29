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
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.gradle.addon.AddOnStatus;
import org.zaproxy.gradle.addon.FunctionalTest;

class DeployAddOnFunctionalTest extends FunctionalTest {

    private static final String DEPLOY_ADD_ON_TASK = ":deployZapAddOn";
    private static final String ADD_ON_ID = "testaddon";
    private static final String ADD_ON_VERSION = "1";
    private static final String ADD_ON_FILE =
            ADD_ON_ID + "-" + AddOnStatus.ALPHA + "-" + ADD_ON_VERSION + ".zap";

    private Path homeDir;

    @BeforeEach
    void setup() throws Exception {
        homeDir = Files.createDirectories(projectDir.resolve("zap-home"));
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
    void shouldDeployAddOnToHomeDirSpecifiedViaCommandLineArgument() throws Exception {
        // Given
        buildFile("");

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK, "--zap-home-dir", homeDir.toString());

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertAddOnDeployed();
    }

    @Test
    void shouldDeployAddOnToHomeDirSpecifiedViaProjectProperty() throws Exception {
        // Given
        buildFile("");

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK, "-Pzap.home.dir=" + homeDir);

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertAddOnDeployed();
    }

    @Test
    void shouldDeployAddOnToHomeDirSpecifiedViaTaskProperty() throws Exception {
        // Given
        buildFile(
                """
                tasks.named<org.zaproxy.gradle.addon.misc.DeployAddOn>("deployZapAddOn") {
                    homeDir.set(file("zap-home"))
                }
                """);

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK);

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertAddOnDeployed();
    }

    @Test
    void shouldDeployHomeFilesToHomeDir() throws Exception {
        // Given
        buildFile("");
        createFile("", projectDir.resolve("src/main/zapHomeFiles/config.xml"));
        createFile("", projectDir.resolve("src/main/zapHomeFiles/scripts/myscript.js"));

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK, "--zap-home-dir", homeDir.toString());

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertThat(homeDir.resolve("config.xml")).exists();
        assertThat(homeDir.resolve("scripts/myscript.js")).exists();
        assertAddOnDeployed();
    }

    @Test
    void shouldDeleteStaleHomeFilesBeforeDeployingByDefault() throws Exception {
        // Given
        buildFile("");
        createFile("", projectDir.resolve("src/main/zapHomeFiles/scripts/current.js"));
        createFile("stale content", homeDir.resolve("scripts/stale.js"));

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK, "--zap-home-dir", homeDir.toString());

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertThat(homeDir.resolve("scripts/stale.js")).doesNotExist();
        assertThat(homeDir.resolve("scripts/current.js")).exists();
    }

    @Test
    void shouldNotDeleteStaleHomeFilesWhenDeleteStaleIsDisabledViaCommandLineArgument()
            throws Exception {
        // Given
        buildFile("");
        createFile("", projectDir.resolve("src/main/zapHomeFiles/scripts/current.js"));
        createFile("stale content", homeDir.resolve("scripts/stale.js"));

        // When
        BuildResult result =
                build(
                        DEPLOY_ADD_ON_TASK,
                        "--zap-home-dir",
                        homeDir.toString(),
                        "--delete-stale=false");

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertThat(homeDir.resolve("scripts/stale.js")).exists();
        assertThat(homeDir.resolve("scripts/current.js")).exists();
    }

    @Test
    void shouldNotDeleteStaleHomeFilesWhenDeleteStaleIsDisabledViaTaskProperty() throws Exception {
        // Given
        buildFile(
                """
                tasks.named<org.zaproxy.gradle.addon.misc.DeployAddOn>("deployZapAddOn") {
                    deleteStale.set(false)
                }
                """);
        createFile("", projectDir.resolve("src/main/zapHomeFiles/scripts/current.js"));
        createFile("stale content", homeDir.resolve("scripts/stale.js"));

        // When
        BuildResult result = build(DEPLOY_ADD_ON_TASK, "--zap-home-dir", homeDir.toString());

        // Then
        assertTaskSuccess(result, DEPLOY_ADD_ON_TASK);
        assertThat(homeDir.resolve("scripts/stale.js")).exists();
        assertThat(homeDir.resolve("scripts/current.js")).exists();
    }

    private void assertAddOnDeployed() {
        assertThat(homeDir.resolve("plugin").resolve(ADD_ON_FILE)).exists();
    }
}
