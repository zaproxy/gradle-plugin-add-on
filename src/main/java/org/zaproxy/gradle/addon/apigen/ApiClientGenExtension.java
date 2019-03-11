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
package org.zaproxy.gradle.addon.apigen;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class ApiClientGenExtension {

    private final Property<String> api;
    private final Property<String> options;
    private final RegularFileProperty messages;
    private final DirectoryProperty baseDir;
    private final ConfigurableFileCollection classpath;

    @Inject
    public ApiClientGenExtension(Project project) {
        ObjectFactory objects = project.getObjects();
        api = objects.property(String.class);
        options = objects.property(String.class);
        messages = objects.fileProperty();
        baseDir = objects.directoryProperty();
        baseDir.set(project.getRootDir().getParentFile());
        classpath = project.files();
    }

    public Property<String> getApi() {
        return api;
    }

    public Property<String> getOptions() {
        return options;
    }

    public RegularFileProperty getMessages() {
        return messages;
    }

    public DirectoryProperty getBaseDir() {
        return baseDir;
    }

    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }
}
