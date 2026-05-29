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

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

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

class UninstallAddOnFunctionalTest extends FunctionalTest {

    private static final String UNINSTALL_ADD_ON_TASK = ":uninstallZapAddOn";
    private static final String ADD_ON_ID = "testaddon";
    private static final String LOCAL_ADDONS_URI = "http://zap/xml/autoupdate/view/localAddons/";
    private static final String UNINSTALL_ADDON_URI =
            "http://zap/xml/autoupdate/action/uninstallAddon/";

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

    @Test
    void shouldUninstallAddOnWhenInstalled() throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(LOCAL_ADDONS_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK,
                                "text/xml",
                                "<localAddons type=\"list\"><addon type=\"set\"><id>"
                                        + ADD_ON_ID
                                        + "</id></addon></localAddons>");
                    }
                });
        zapServer.addHandler(
                new NanoServerHandler(UNINSTALL_ADDON_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", "<Result>OK</Result>");
                    }
                });
        buildFile("");

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
        assertThat(zapServer.getRequests())
                .hasSize(2)
                .satisfies(
                        request -> assertThat(request.uri()).endsWith("/localAddons/"), atIndex(0))
                .satisfies(
                        request -> {
                            assertThat(request.uri()).endsWith("/uninstallAddon/");
                            assertThat(request.parameters().get("id")).containsExactly(ADD_ON_ID);
                        },
                        atIndex(1));
    }

    @ParameterizedTest
    @MethodSource("localAddonsWithoutTestAddon")
    void shouldSucceedWhenAddOnIsNotInstalled(String localAddonsXml) throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(LOCAL_ADDONS_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", localAddonsXml);
                    }
                });
        zapServer.addHandler(
                new NanoServerHandler(UNINSTALL_ADDON_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", "<Result>OK</Result>");
                    }
                });
        buildFile("");

        // When
        BuildResult result = build(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, UNINSTALL_ADD_ON_TASK);
        assertThat(zapServer.getRequests())
                .singleElement()
                .extracting(HTTPDTestServer.Request::uri, as(STRING))
                .endsWith("/localAddons/");
    }

    static Stream<Arguments> localAddonsWithoutTestAddon() {
        return Stream.of(
                Arguments.of("<localAddons type=\"list\"></localAddons>"),
                Arguments.of(
                        "<localAddons type=\"list\"><addon type=\"set\"><id>otheraddon</id></addon></localAddons>"));
    }

    @Test
    void shouldFailWhenZapIsNotAvailable() throws Exception {
        // Given
        zapServer.stop();
        buildFile("");

        // When
        BuildResult result = buildAndFail(UNINSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskFailed(result, UNINSTALL_ADD_ON_TASK);
        assertThat(result.getOutput())
                .contains("Failed to uninstall the old version of the add-on");
        assertThat(zapServer.getRequests()).isEmpty();
    }
}
