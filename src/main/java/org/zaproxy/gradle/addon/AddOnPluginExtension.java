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
package org.zaproxy.gradle.addon;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/** The entry extension of the plugin. */
public class AddOnPluginExtension {

    private static final String DEFAULT_ZAP_VERSION = "2.7.0";

    private final Property<String> addOnId;
    private final Property<String> addOnName;
    private final Property<AddOnStatus> addOnStatus;
    private final Property<String> addOnVersion;
    private final Property<String> zapVersion;

    @Inject
    public AddOnPluginExtension(Project project) {
        this.addOnId = project.getObjects().property(String.class);
        this.addOnId.set(project.getName());
        this.addOnName = project.getObjects().property(String.class);
        this.addOnStatus =
                project.getObjects().property(AddOnStatus.class).value(AddOnStatus.ALPHA);
        this.addOnVersion = project.getObjects().property(String.class);
        this.addOnVersion.set(project.provider(() -> project.getVersion().toString()));
        this.zapVersion = project.getObjects().property(String.class).value(DEFAULT_ZAP_VERSION);
    }

    public Property<String> getAddOnId() {
        return addOnId;
    }

    public Property<String> getAddOnName() {
        return addOnName;
    }

    public Property<AddOnStatus> getAddOnStatus() {
        return addOnStatus;
    }

    public Property<String> getAddOnVersion() {
        return addOnVersion;
    }

    public Property<String> getZapVersion() {
        return zapVersion;
    }
}
