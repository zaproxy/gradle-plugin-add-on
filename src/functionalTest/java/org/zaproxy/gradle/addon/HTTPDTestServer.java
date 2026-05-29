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
package org.zaproxy.gradle.addon;

import fi.iki.elonen.NanoHTTPD;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HTTPDTestServer extends NanoHTTPD {

    private List<NanoServerHandler> handlers = new ArrayList<>();
    private List<Request> requests = new ArrayList<>();

    private NanoServerHandler handler404 =
            new NanoServerHandler("") {
                @Override
                protected Response serve(IHTTPSession session) {
                    consumeBody(session);
                    return newFixedLengthResponse(
                            Response.Status.NOT_FOUND,
                            MIME_HTML,
                            "<html><head><title>404</title></head><body>404 Not Found</body></html>");
                }
            };

    public HTTPDTestServer(int port) {
        super(port);
    }

    public List<Request> getRequests() {
        return requests;
    }

    @Override
    public Response serve(IHTTPSession session) {
        requests.add(
                new Request(
                        session.getUri(),
                        session.getMethod().toString(),
                        session.getParameters(),
                        session.getHeaders(),
                        NanoServerHandler.getBody(session)));

        for (NanoServerHandler handler : handlers) {
            if (handler.handles(session)) {
                return handler.serve(session);
            }
        }
        return handler404.serve(session);
    }

    public void addHandler(NanoServerHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(NanoServerHandler handler) {
        this.handlers.remove(handler);
    }

    public void setHandler404(NanoServerHandler handler) {
        this.handler404 = handler;
    }

    public static record Request(
            String uri,
            String method,
            Map<String, List<String>> parameters,
            Map<String, String> headers,
            String body) {}
}
