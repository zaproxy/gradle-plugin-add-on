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
package org.zaproxy.gradle.addon.jh.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.gradle.addon.FunctionalTest;

class JavaHelpIndexerFunctionalTest extends FunctionalTest {

    private static final String JHINDEX_TASK = ":jhindex";
    private static final String DB_NAME = "JavaHelpSearch";

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
                tasks.register<org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer>("jhindex") {
                    helpset.set(layout.projectDirectory.file("help/helpset.hs"))
                    destinationDir.set(layout.buildDirectory.dir("jhindex"))
                    setSource(project.fileTree("help").matching { include("**/*.html") })
                }
                """
                        + content);
    }

    @BeforeEach
    void setUp() throws Exception {
        buildFile("");
    }

    private void writeHelpSet(String xmlLang) throws Exception {
        createFile(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <helpset version="2.0" lang="%s">
                </helpset>
                """
                        .formatted(xmlLang),
                projectDir.resolve("help/helpset.hs"));
    }

    private void writeHtmlFile(String relPath, String content) throws Exception {
        createFile(content, projectDir.resolve(relPath));
    }

    private BuildResult runJhindex() throws Exception {
        return build(JHINDEX_TASK);
    }

    @Test
    void shouldGenerateSearchIndexFromHtmlFiles() throws Exception {
        // Given
        writeHelpSet("en");
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = runJhindex();

        // Then
        assertTaskSuccess(result, JHINDEX_TASK);
        assertThat(projectDir.resolve("build/jhindex/" + DB_NAME)).isDirectory();
    }

    @Test
    void shouldBeUpToDateOnSubsequentRunWithoutChanges() throws Exception {
        // Given
        writeHelpSet("en");
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");
        runJhindex();

        // When
        BuildResult result = runJhindex();

        // Then
        assertTaskUpToDate(result, JHINDEX_TASK);
    }

    @Test
    void shouldUseCustomDbName() throws Exception {
        // Given
        String customDbName = "CustomIndex";
        createFile(
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
                tasks.register<org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer>("jhindex") {
                    helpset.set(layout.projectDirectory.file("help/helpset.hs"))
                    destinationDir.set(layout.buildDirectory.dir("jhindex"))
                    dbName.set("%s")
                    setSource(project.fileTree("help").matching { include("**/*.html") })
                }
                """
                        .formatted(customDbName),
                projectDir.resolve("build.gradle.kts"));
        writeHelpSet("en");
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = runJhindex();

        // Then
        assertTaskSuccess(result, JHINDEX_TASK);
        assertThat(projectDir.resolve("build/jhindex/" + customDbName)).isDirectory();
    }

    @Test
    void shouldUseOutputPrefixForWorkingDirectory() throws Exception {
        // Given
        String prefix = "myPrefix";
        createFile(
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
                tasks.register<org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer>("jhindex") {
                    helpset.set(layout.projectDirectory.file("help/helpset.hs"))
                    destinationDir.set(layout.buildDirectory.dir("jhindex"))
                    outputPrefix.set("%s")
                    setSource(project.fileTree("help").matching { include("**/*.html") })
                }
                """
                        .formatted(prefix),
                projectDir.resolve("build.gradle.kts"));
        writeHelpSet("en");
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = runJhindex();

        // Then
        assertTaskSuccess(result, JHINDEX_TASK);
        assertThat(projectDir.resolve("build/jhindex/" + prefix + "/" + DB_NAME)).isDirectory();
    }

    @Test
    void shouldRespectLocaleFromHelpset() throws Exception {
        // Given
        writeHelpSet("pt-BR");
        writeHtmlFile("help/page.html", "<html><body>Conteúdo de teste</body></html>");

        // When
        BuildResult result = runJhindex();

        // Then
        assertTaskSuccess(result, JHINDEX_TASK);
        assertThat(projectDir.resolve("build/jhindex/" + DB_NAME)).isDirectory();
    }

    @Test
    void shouldFailWhenHelpsetFileDoesNotExist() throws Exception {
        // Given
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = buildAndFail(JHINDEX_TASK);

        // Then
        assertTaskFailed(result, JHINDEX_TASK);
        assertThat(result.getOutput())
                .contains("property 'helpset' specifies file '")
                .contains("/help/helpset.hs' which doesn't exist.");
    }

    @Test
    void shouldFailWhenHelpsetHasNoXmlLangAttribute() throws Exception {
        // Given
        createFile(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <helpset version="2.0">
                </helpset>
                """,
                projectDir.resolve("help/helpset.hs"));
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = buildAndFail(JHINDEX_TASK);

        // Then
        assertTaskFailed(result, JHINDEX_TASK);
        assertThat(result.getOutput())
                .contains("Required helpset xml:lang attribute not found or empty in:")
                .contains("/help/helpset.hs");
    }

    @Test
    void shouldFailWhenHelpsetPropertyIsNotSet() throws Exception {
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
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
                }
                tasks.register<org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer>("jhindex") {
                    destinationDir.set(layout.buildDirectory.dir("jhindex"))
                    setSource(project.fileTree("help").matching { include("**/*.html") })
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = buildAndFail(JHINDEX_TASK);

        // Then
        assertTaskFailed(result, JHINDEX_TASK);
        assertThat(result.getOutput())
                .contains("property 'helpset' doesn't have a configured value.");
    }

    @Test
    void shouldFailWhenDestinationDirPropertyIsNotSet() throws Exception {
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
                zapAddOn {
                    addOnId.set("testaddon")
                    addOnName.set("Test Add-On")
                }
                tasks.register<org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer>("jhindex") {
                    helpset.set(layout.projectDirectory.file("help/helpset.hs"))
                    setSource(project.fileTree("help").matching { include("**/*.html") })
                }
                """,
                projectDir.resolve("build.gradle.kts"));
        writeHelpSet("en");
        writeHtmlFile("help/page.html", "<html><body>Test content</body></html>");

        // When
        BuildResult result = buildAndFail(JHINDEX_TASK);

        // Then
        assertTaskFailed(result, JHINDEX_TASK);
        assertThat(result.getOutput())
                .contains("property 'destinationDir' doesn't have a configured value.");
    }
}
