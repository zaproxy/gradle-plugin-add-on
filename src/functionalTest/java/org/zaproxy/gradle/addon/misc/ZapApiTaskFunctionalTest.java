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

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zaproxy.gradle.addon.FunctionalTest;
import org.zaproxy.gradle.addon.HTTPDTestServer;
import org.zaproxy.gradle.addon.NanoServerHandler;

class ZapApiTaskFunctionalTest extends FunctionalTest {

    private static final String UNINSTALL_ADD_ON_TASK = ":uninstallZapAddOn";
    private static final String ADD_ON_ID = "testaddon";
    private static final String LOCAL_ADDONS_URI = "http://zap/xml/autoupdate/view/localAddons/";
    private static final String EMPTY_LOCAL_ADDONS = "<localAddons type=\"list\"></localAddons>";

    private HTTPDTestServer zapServer;
    private int zapPort;

    @BeforeEach
    void startZapServer() throws Exception {
        zapServer = new HTTPDTestServer(0);
        zapServer.start();
        zapPort = zapServer.getListeningPort();
    }

    @AfterEach
    void stopZapServer() {
        zapServer.stop();
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
                version = "1"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                """
                                .formatted(ADD_ON_ID)
                        + content);
    }

    private void addEmptyLocalAddonsHandler() {
        zapServer.addHandler(
                new NanoServerHandler(LOCAL_ADDONS_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", EMPTY_LOCAL_ADDONS);
                    }
                });
    }

    @Test
    void shouldUsePortAsTaskProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        createFile(
                """
                import org.zaproxy.gradle.addon.misc.UninstallAddOn
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "1"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                tasks.named<UninstallAddOn>("uninstallZapAddOn") {
                    port.set(%d)
                }
                """
                        .formatted(ADD_ON_ID, zapPort),
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK);

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldUsePortFromProjectProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK, "-Pzap.api.port=" + zapPort);

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldUsePortFromCommandLineArgument() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldUseAddressAsTaskProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        createFile(
                """
                import org.zaproxy.gradle.addon.misc.UninstallAddOn
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "1"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                tasks.named<UninstallAddOn>("uninstallZapAddOn") {
                    address.set("127.0.0.1")
                }
                """
                        .formatted(ADD_ON_ID),
                projectDir.resolve("build.gradle.kts"));

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldUseAddressFromProjectProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        BuildResult result =
                build(
                        UNINSTALL_ADD_ON_TASK,
                        "-Pzap.api.address=127.0.0.1",
                        "-Pzap.api.port=" + zapPort);

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldUseAddressFromCommandLineArgument() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        BuildResult result =
                build(
                        UNINSTALL_ADD_ON_TASK,
                        "--address",
                        "127.0.0.1",
                        "--port",
                        String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
    }

    @Test
    void shouldSendApiKeyAsTaskProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        createFile(
                """
                import org.zaproxy.gradle.addon.misc.UninstallAddOn
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                version = "1"
                zapAddOn {
                    addOnId.set("%s")
                    addOnName.set("Test Add-On")
                }
                tasks.named<UninstallAddOn>("uninstallZapAddOn") {
                    apiKey.set("my-secret-key")
                }
                """
                        .formatted(ADD_ON_ID),
                projectDir.resolve("build.gradle.kts"));

        // When
        build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertThat(zapServer.getRequests())
                .anySatisfy(
                        request ->
                                assertThat(request.headers())
                                        .containsEntry("x-zap-api-key", "my-secret-key"));
    }

    @Test
    void shouldSendApiKeyFromProjectProperty() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        build(
                UNINSTALL_ADD_ON_TASK,
                "-Pzap.api.key=my-secret-key",
                "--port",
                String.valueOf(zapPort));

        // Then
        assertThat(zapServer.getRequests())
                .anySatisfy(
                        request ->
                                assertThat(request.headers())
                                        .containsEntry("x-zap-api-key", "my-secret-key"));
    }

    @Test
    void shouldSendApiKeyFromCommandLineArgument() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        build(
                UNINSTALL_ADD_ON_TASK,
                "--port",
                String.valueOf(zapPort),
                "--api-key",
                "my-secret-key");

        // Then
        assertThat(zapServer.getRequests())
                .anySatisfy(
                        request ->
                                assertThat(request.headers())
                                        .containsEntry("x-zap-api-key", "my-secret-key"));
    }

    @Test
    void shouldNotSendApiKeyWhenNotSpecified() throws Exception {
        // Given
        addEmptyLocalAddonsHandler();
        buildFile("");

        // When
        build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertThat(zapServer.getRequests())
                .allSatisfy(
                        request ->
                                assertThat(request.headers()).doesNotContainKey("x-zap-api-key"));
    }

    @ParameterizedTest
    @MethodSource("invalidPorts")
    void shouldFailWhenPortIsInvalid(String port) throws Exception {
        // Given
        buildFile("");

        // When
        BuildResult result = buildAndFail(UNINSTALL_ADD_ON_TASK, "--port", port);

        // Then
        assertThat(result.getOutput()).contains("is not valid");
    }

    static Stream<Arguments> invalidPorts() {
        return Stream.of(Arguments.of("0"), Arguments.of("65536"), Arguments.of("notAPort"));
    }

    @Test
    void shouldFailWhenServerReturnsHttpError() throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(LOCAL_ADDONS_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                "text/html",
                                "Internal Server Error");
                    }
                });
        buildFile("");

        // When
        BuildResult result = buildAndFail(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskFailed(result, UNINSTALL_ADD_ON_TASK);
        assertThat(result.getOutput())
                .contains("Failed to uninstall the old version of the add-on");
    }

    @Test
    void shouldFailWhenServerReturnsZapApiError() throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(LOCAL_ADDONS_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK,
                                "text/xml",
                                "<error type=\"exception\" code=\"401\">Unauthorized</error>");
                    }
                });
        buildFile("");

        // When
        BuildResult result = buildAndFail(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskFailed(result, UNINSTALL_ADD_ON_TASK);
        assertThat(result.getOutput())
                .contains("Failed to uninstall the old version of the add-on");
    }
}
