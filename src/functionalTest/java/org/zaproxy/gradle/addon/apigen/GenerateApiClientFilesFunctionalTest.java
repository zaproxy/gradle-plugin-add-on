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
package org.zaproxy.gradle.addon.apigen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;

class GenerateApiClientFilesFunctionalTest extends FunctionalTest {

    private static final String GENERATE_ALL_TASK = ":generateZapApiClientFiles";

    private static final String DOTNET_SUBDIR =
            "zap-api-dotnet/src/OWASPZAPDotNetAPI/OWASPZAPDotNetAPI/Generated";
    private static final String GO_SUBDIR = "zap-api-go/zap";
    private static final String JAVA_SUBDIR =
            "zap-api-java/subprojects/zap-clientapi/src/main/java/org/zaproxy/clientapi/gen";
    private static final String NODEJS_SUBDIR = "zap-api-nodejs/src";
    private static final String PHP_SUBDIR = "zap-api-php/src/Zap";
    private static final String PYTHON_SUBDIR = "zap-api-python/src/zapv2";
    private static final String RUST_SUBDIR = "zap-api-rust/src";

    private static final List<String> ALL_SUBDIRS =
            List.of(
                    DOTNET_SUBDIR,
                    GO_SUBDIR,
                    JAVA_SUBDIR,
                    NODEJS_SUBDIR,
                    PHP_SUBDIR,
                    PYTHON_SUBDIR,
                    RUST_SUBDIR);

    @Override
    protected void buildFile(String content) throws Exception {
        buildFileWithAddOnSetup(content);
    }

    @Test
    void shouldGenerateApiClientFilesForAllLanguages() throws Exception {
        // Given
        createTestApiClass();
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createAllLanguageOutputDirs(baseDir);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_ALL_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_ALL_TASK);
        assertOutputFileExists(baseDir, DOTNET_SUBDIR, "Testapi.cs");
        assertOutputFileExists(baseDir, GO_SUBDIR, "testapi_generated.go");
        assertOutputFileExists(baseDir, JAVA_SUBDIR, "Testapi.java");
        assertOutputFileExists(baseDir, NODEJS_SUBDIR, "testapi.js");
        assertOutputFileExists(baseDir, PHP_SUBDIR, "Testapi.php");
        assertOutputFileExists(baseDir, PYTHON_SUBDIR, "testapi.py");
        assertOutputFileExists(baseDir, RUST_SUBDIR, "testapi.rs");
    }

    @ParameterizedTest
    @MethodSource("languages")
    void shouldGenerateApiClientFilesForLanguage(
            String taskName, String subdir, String expectedFile) throws Exception {
        // Given
        createTestApiClass();
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, subdir);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = build(taskName);

        // Then
        assertTaskSuccess(result, taskName);
        assertOutputFileExists(baseDir, subdir, expectedFile);
    }

    private static Stream<Arguments> languages() {
        return Stream.of(
                Arguments.of(":generateDotNetZapApiClientFiles", DOTNET_SUBDIR, "Testapi.cs"),
                Arguments.of(":generateGoZapApiClientFiles", GO_SUBDIR, "testapi_generated.go"),
                Arguments.of(":generateJavaZapApiClientFiles", JAVA_SUBDIR, "Testapi.java"),
                Arguments.of(":generateNodeJsZapApiClientFiles", NODEJS_SUBDIR, "testapi.js"),
                Arguments.of(":generatePhpZapApiClientFiles", PHP_SUBDIR, "Testapi.php"),
                Arguments.of(":generatePythonZapApiClientFiles", PYTHON_SUBDIR, "testapi.py"),
                Arguments.of(":generateRustZapApiClientFiles", RUST_SUBDIR, "testapi.rs"));
    }

    @Test
    void shouldGenerateApiClientFilesWithDescriptionsFromMessages() throws Exception {
        // Given
        createTestApiClass();
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, JAVA_SUBDIR);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = build(":generateJavaZapApiClientFiles");

        // Then
        assertTaskSuccess(result, ":generateJavaZapApiClientFiles");
        String generatedFile = readOutputFile(baseDir, JAVA_SUBDIR, "Testapi.java");
        assertThat(generatedFile).contains("Test view description");
        assertThat(generatedFile).contains("Test action description");
    }

    @Test
    void shouldGenerateApiClientFilesWithOptions() throws Exception {
        // Given
        createTestApiClass();
        createTestOptionsClass();
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, JAVA_SUBDIR);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        options.set("com.example.TestOptions")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = build(":generateJavaZapApiClientFiles");

        // Then
        assertTaskSuccess(result, ":generateJavaZapApiClientFiles");
        String generatedFile = readOutputFile(baseDir, JAVA_SUBDIR, "Testapi.java");
        assertThat(generatedFile).contains("Test view description");
        assertThat(generatedFile).contains("Test action description");
        assertThat(generatedFile).contains("Test option description");
    }

    @Test
    void shouldGenerateApiClientFilesWithCustomMessages() throws Exception {
        // Given
        createTestApiClass();
        String customMessagesPath = "src/main/resources/com/example/resources/Messages.properties";
        createFile(
                """
                testapi.api.view.testView=Custom view description
                testapi.api.action.testAction=Custom action description
                """,
                projectDir.resolve(customMessagesPath));
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, JAVA_SUBDIR);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        baseDir.set(file("api-output"))
                        messages.set(file("%s"))
                    }
                }
                """
                        .formatted(customMessagesPath));

        // When
        BuildResult result = build(":generateJavaZapApiClientFiles");

        // Then
        assertTaskSuccess(result, ":generateJavaZapApiClientFiles");
        String generatedFile = readOutputFile(baseDir, JAVA_SUBDIR, "Testapi.java");
        assertThat(generatedFile).contains("Custom view description");
        assertThat(generatedFile).contains("Custom action description");
    }

    @Test
    void shouldFailWhenApiPropertyIsNotSet() throws Exception {
        // Given
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = buildAndFail(GENERATE_ALL_TASK);

        // Then
        assertTaskFailed(result, GENERATE_ALL_TASK);
        assertThat(result.getOutput()).contains("property 'api' doesn't have a configured value");
    }

    @Test
    void shouldFailWhenApiClassDoesNotExist() throws Exception {
        // Given
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, PYTHON_SUBDIR);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.NonExistentApi")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = buildAndFail(":generatePythonZapApiClientFiles");

        // Then
        assertTaskFailed(result, ":generatePythonZapApiClientFiles");
        assertThat(result.getOutput())
                .contains("ClassNotFoundException: com.example.NonExistentApi");
    }

    @Test
    void shouldFailWhenOptionsClassDoesNotExist() throws Exception {
        // Given
        createTestApiClass();
        createMessagesWithDescriptions();
        Path baseDir = createBaseDir();
        createLanguageOutputDir(baseDir, JAVA_SUBDIR);
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        options.set("com.example.NonExistentOptions")
                        baseDir.set(file("api-output"))
                    }
                }
                """);

        // When
        BuildResult result = buildAndFail(":generateJavaZapApiClientFiles");

        // Then
        assertTaskFailed(result, ":generateJavaZapApiClientFiles");
        assertThat(result.getOutput())
                .contains("ClassNotFoundException: com.example.NonExistentOptions");
    }

    @Test
    void shouldFailWhenBaseDirDoesNotExist() throws Exception {
        // Given
        createTestApiClass();
        createMessagesWithDescriptions();
        buildFile(
                """
                zapAddOn {
                    apiClientGen {
                        api.set("com.example.TestApi")
                        baseDir.set(file("nonexistent-dir"))
                    }
                }
                """);

        // When
        BuildResult result = buildAndFail(":generatePythonZapApiClientFiles");

        // Then
        assertTaskFailed(result, ":generatePythonZapApiClientFiles");
        assertThat(result.getOutput())
                .contains(
                        "IllegalArgumentException: The property basedir is not a directory or does not exist");
    }

    private void createTestApiClass() throws Exception {
        createFile(
                """
                package com.example;

                import org.zaproxy.zap.extension.api.ApiAction;
                import org.zaproxy.zap.extension.api.ApiImplementor;
                import org.zaproxy.zap.extension.api.ApiView;

                public class TestApi extends ApiImplementor {

                    public TestApi() {
                        addApiView(
                                new ApiView(
                                        "testView",
                                        new String[] {"mandatoryParam"},
                                        new String[] {"optionalParam"}));
                        addApiAction(
                                new ApiAction(
                                        "testAction",
                                        new String[] {"mandatoryParam"},
                                        new String[] {"optionalParam"}));
                    }

                    @Override
                    public String getPrefix() {
                        return "testapi";
                    }
                }
                """,
                projectDir.resolve("src/main/java/com/example/TestApi.java"));
    }

    private void createTestOptionsClass() throws Exception {
        createFile(
                """
                package com.example;

                import org.parosproxy.paros.common.AbstractParam;

                public class TestOptions extends AbstractParam {

                    private String testOption = "";

                    public String getTestOption() {
                        return testOption;
                    }

                    public void setTestOption(String value) {
                        this.testOption = value;
                    }

                    @Override
                    protected void parse() {}
                }
                """,
                projectDir.resolve("src/main/java/com/example/TestOptions.java"));
    }

    private void createMessagesWithDescriptions() throws Exception {
        createFile(
                """
                testapi.api.view.testView=Test view description
                testapi.api.view.testView.param.mandatoryParam=Mandatory param description
                testapi.api.view.testView.param.optionalParam=Optional param description
                testapi.api.action.testAction=Test action description
                testapi.api.action.testAction.param.mandatoryParam=Mandatory param description
                testapi.api.action.testAction.param.optionalParam=Optional param description
                testapi.api.view.optionTestOption=Test option description
                testapi.api.action.setOptionTestOption=Test option action description
                testapi.api.action.setOptionTestOption.param.String=String value
                """,
                projectDir.resolve("src/main/resources/com/example/resources/Messages.properties"));
    }

    private Path createBaseDir() throws Exception {
        Path dir = projectDir.resolve("api-output");
        Files.createDirectories(dir);
        return dir;
    }

    private static void createAllLanguageOutputDirs(Path baseDir) throws Exception {
        for (String subdir : ALL_SUBDIRS) {
            createLanguageOutputDir(baseDir, subdir);
        }
    }

    private static void createLanguageOutputDir(Path baseDir, String subdir) throws Exception {
        Files.createDirectories(baseDir.resolve(subdir));
    }

    private static void assertOutputFileExists(Path baseDir, String subdir, String filename) {
        assertThat(baseDir.resolve(subdir).resolve(filename)).exists();
    }

    private static String readOutputFile(Path baseDir, String subdir, String filename)
            throws IOException {
        return Files.readString(baseDir.resolve(subdir).resolve(filename));
    }
}
