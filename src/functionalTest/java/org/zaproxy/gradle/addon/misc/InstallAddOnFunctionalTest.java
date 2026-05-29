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
import static org.assertj.core.api.InstanceOfAssertFactories.map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.gradle.addon.FunctionalTest;
import org.zaproxy.gradle.addon.HTTPDTestServer;
import org.zaproxy.gradle.addon.NanoServerHandler;

class InstallAddOnFunctionalTest extends FunctionalTest {

    private static final String INSTALL_ADD_ON_TASK = ":installZapAddOn";
    private static final String ADD_ON_ID = "testaddon";
    private static final String INSTALL_LOCAL_ADDON_URI =
            "http://zap/xml/autoupdate/action/installLocalAddon/";

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
    void shouldInstallAddOn() throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(INSTALL_LOCAL_ADDON_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", "<Result>OK</Result>");
                    }
                });
        buildFile("");

        // When
        BuildResult result = build(INSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskSuccess(result, INSTALL_ADD_ON_TASK);
        assertApiRequest();
    }

    @Test
    void shouldFailWhenZapResponseIsNotOk() throws Exception {
        // Given
        zapServer.addHandler(
                new NanoServerHandler(INSTALL_LOCAL_ADDON_URI) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        consumeBody(session);
                        return NanoHTTPD.newFixedLengthResponse(
                                Response.Status.OK, "text/xml", "<Result>FAIL</Result>");
                    }
                });
        buildFile("");

        // When
        BuildResult result = buildAndFail(INSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskFailed(result, INSTALL_ADD_ON_TASK);
        assertThat(result.getOutput()).contains("Failed to install the add-on");
        assertApiRequest();
    }

    @Test
    void shouldFailWhenZapIsNotAvailable() throws Exception {
        // Given
        zapServer.stop();
        buildFile("");

        // When
        BuildResult result = buildAndFail(INSTALL_ADD_ON_TASK, "--port", String.valueOf(zapPort));

        // Then
        assertTaskFailed(result, INSTALL_ADD_ON_TASK);
        assertThat(result.getOutput()).contains("An error occurred while installing the add-on");
        assertThat(zapServer.getRequests()).isEmpty();
    }

    private void assertApiRequest() {
        assertThat(zapServer.getRequests())
                .singleElement()
                .extracting(HTTPDTestServer.Request::parameters, as(map(String.class, List.class)))
                .hasEntrySatisfying("file", e -> e.contains(ADD_ON_ID));
    }
}
