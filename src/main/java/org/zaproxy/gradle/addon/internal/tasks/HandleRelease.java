/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.zaproxy.gradle.addon.internal.BuildException;
import org.zaproxy.gradle.addon.internal.model.AddOnRelease;

/**
 * Task that handles the release.
 *
 * <p>Sends a repository dispatch to update the marketplace.
 */
public abstract class HandleRelease extends SendRepositoryDispatch {

    private static final String ADD_ON_RELEASE_EVENT = "add-on-release";
    private static final String DEFAULT_CHECKSUM_ALGORITHM = "SHA-256";
    private static final String HTTPS_SCHEME = "HTTPS";

    private Map<String, List<Map<String, String>>> payloadData;

    public HandleRelease() {
        getEventType().set(ADD_ON_RELEASE_EVENT);
        getChecksumAlgorithm().set(DEFAULT_CHECKSUM_ALGORITHM);
        getClientPayload()
                .set(
                        getProject()
                                .provider(
                                        () -> {
                                            if (payloadData == null) {
                                                createPayloadData();
                                            }
                                            return payloadData;
                                        }));
    }

    @Nested
    public abstract ListProperty<AddOnRelease> getAddOns();

    @Input
    public abstract Property<String> getChecksumAlgorithm();

    @Override
    public void send() {
        try {
            for (AddOnRelease addOnRelease : getAddOns().get()) {
                String urlString = addOnRelease.getDownloadUrl().get();
                URL url = new URL(urlString);
                if (!HTTPS_SCHEME.equalsIgnoreCase(url.getProtocol())) {
                    throw new IllegalArgumentException(
                            "The provided URL does not use HTTPS scheme: " + url.getProtocol());
                }
            }
        } catch (MalformedURLException e) {
            throw new BuildException("Failed to parse download URL.", e);
        }

        super.send();
    }

    private void createPayloadData() {
        List<Map<String, String>> addOns = new ArrayList<>();
        for (AddOnRelease addOnRelease : getAddOns().get()) {
            String checksum;
            try {
                checksum =
                        createChecksum(
                                getChecksumAlgorithm().get(),
                                addOnRelease.getAddOn().getAsFile().get().toPath());
            } catch (IOException e) {
                throw new BuildException(e);
            }

            Map<String, String> addOnData = new HashMap<>();
            addOnData.put("url", addOnRelease.getDownloadUrl().get());
            addOnData.put("checksum", checksum);
            addOns.add(addOnData);
        }

        payloadData = new HashMap<>();
        payloadData.put("addons", addOns);
    }

    private static String createChecksum(String algorithm, Path file) throws IOException {
        return new DigestUtils(algorithm).digestAsHex(file.toFile());
    }
}
