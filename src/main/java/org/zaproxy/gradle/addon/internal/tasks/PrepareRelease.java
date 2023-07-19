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
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.zaproxy.gradle.addon.AddOnPluginExtension;
import org.zaproxy.gradle.addon.misc.PrepareAddOnRelease;

public abstract class PrepareRelease extends PrepareAddOnRelease {

    private static final String RELEASE_PROPERTY = "release";

    public PrepareRelease() {
        getPropertiesFile().set(getProject().file(Project.GRADLE_PROPERTIES));
        getReleaseProperty().set(RELEASE_PROPERTY);

        AddOnPluginExtension extension =
                getProject().getExtensions().findByType(AddOnPluginExtension.class);
        if (extension != null) {
            getChangelog().convention(extension.getChangelog());
            getCurrentVersion().convention(extension.getAddOnVersion());
            getReleaseLink().convention(extension.getReleaseLink());
        }

        setGroup(null);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPropertiesFile();

    @Input
    public abstract Property<String> getReleaseProperty();

    @Override
    public void prepare() throws IOException {
        super.prepare();

        ProjectProperties properties =
                new ProjectProperties(getPropertiesFile().get().getAsFile().toPath());
        properties.setProperty(getReleaseProperty().get(), "true");
        properties.store();
    }
}
