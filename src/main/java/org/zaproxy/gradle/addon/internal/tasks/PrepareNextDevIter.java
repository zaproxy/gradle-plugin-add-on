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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import java.io.IOException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.zaproxy.gradle.addon.AddOnPluginExtension;

public abstract class PrepareNextDevIter extends UpdateChangelogNextDevIter {

    private static final String VERSION_PROPERTY = "version";
    private static final String RELEASE_PROPERTY = "release";

    public PrepareNextDevIter() {
        getPropertiesFile().set(getProject().file(Project.GRADLE_PROPERTIES));
        getVersionProperty().set(VERSION_PROPERTY);
        getReleaseProperty().set(RELEASE_PROPERTY);

        AddOnPluginExtension extension =
                getProject().getExtensions().findByType(AddOnPluginExtension.class);
        if (extension != null) {
            getChangelog().convention(extension.getChangelog());
            getCurrentVersion().convention(extension.getAddOnVersion());
            getUnreleasedLink().convention(extension.getUnreleasedLink());
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPropertiesFile();

    @Input
    public abstract Property<String> getVersionProperty();

    @Input
    public abstract Property<String> getReleaseProperty();

    @Override
    public void prepare() throws IOException {
        super.prepare();

        ProjectProperties properties =
                new ProjectProperties(getPropertiesFile().get().getAsFile().toPath());
        String versionProperty = getVersionProperty().get();
        properties.setProperty(
                versionProperty, bumpVersion(properties.getProperty(versionProperty)));
        properties.setProperty(getReleaseProperty().get(), "false");
        properties.store();
    }

    private static String bumpVersion(String version) {
        try {
            int currentVersion = Integer.parseInt(version);
            return Integer.toString(++currentVersion);
        } catch (NumberFormatException e) {
            // Ignore, not an integer version.
        }

        try {
            return Version.valueOf(version).incrementMinorVersion().toString();
        } catch (IllegalArgumentException | ParseException e) {
            throw new InvalidUserDataException(
                    "Failed to parse the current version: " + version, e);
        }
    }
}
