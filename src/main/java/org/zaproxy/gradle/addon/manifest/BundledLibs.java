/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2020 The ZAP Development Team
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
package org.zaproxy.gradle.addon.manifest;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

public class BundledLibs {

    private static final String DEFAULT_DIR_NAME = "libs";

    private final Property<String> dirName;
    private final ConfigurableFileCollection libs;

    @Inject
    public BundledLibs(Project project) {
        this.dirName = project.getObjects().property(String.class).convention(DEFAULT_DIR_NAME);
        this.libs = project.files();
    }

    @Input
    public Property<String> getDirName() {
        return dirName;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public ConfigurableFileCollection getLibs() {
        return libs;
    }
}
