/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2019 The ZAP Development Team
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

import java.util.Locale;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;
import org.zaproxy.clientapi.core.ClientApi;

/** A task that accesses the ZAP API. */
public class ZapApiTask extends DefaultTask {

    private enum Default {
        ADDRESS("127.0.0.1"),
        PORT("8080"),
        KEY(null);

        private final String propertyKey;
        private final String defaultValue;

        Default(String defaultValue) {
            this.propertyKey = "zap.api." + name().toLowerCase(Locale.ROOT);
            this.defaultValue = defaultValue;
        }

        String getValue(Project project) {
            String value = null;
            if (project.hasProperty(propertyKey)) {
                value = (String) project.property(propertyKey);
            }
            if (value == null || value.isEmpty()) {
                value = defaultValue;
            }
            return value;
        }
    }

    private final Property<String> address;
    private final Property<Integer> port;
    private final Property<String> apiKey;

    public ZapApiTask() {
        ObjectFactory objects = getProject().getObjects();
        address = objects.property(String.class).value(Default.ADDRESS.getValue(getProject()));
        port = objects.property(Integer.class);
        optionPort(Default.PORT.getValue(getProject()));
        apiKey = objects.property(String.class).value(Default.KEY.getValue(getProject()));
    }

    @Input
    public Property<String> getAddress() {
        return address;
    }

    @Option(option = "address", description = "The ZAP address.")
    public void optionAddress(String address) {
        this.address.set(address);
    }

    @Input
    public Property<Integer> getPort() {
        return port;
    }

    @Option(option = "port", description = "The port where ZAP is or will be listening.")
    public void optionPort(String port) {
        try {
            getPort().set(Integer.parseInt(port));
        } catch (NumberFormatException e) {
            throwInvalidPort(port);
        }
    }

    @Input
    @Optional
    public Property<String> getApiKey() {
        return apiKey;
    }

    @Option(option = "api-key", description = "The ZAP API key.")
    public void optionApiKey(String apiKey) {
        this.apiKey.set(apiKey);
    }

    protected ClientApi createClient() {
        validatePort(port.get());

        return new ClientApi(address.get(), port.get(), apiKey.getOrNull());
    }

    private static void validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throwInvalidPort(port);
        }
    }

    private static void throwInvalidPort(Object port) {
        throw new IllegalArgumentException(
                String.format(
                        "The specified port '%1s' is not valid, it should be > 0 and <= 65535.",
                        port));
    }
}
