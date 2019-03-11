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
package org.zaproxy.gradle.addon.wiki;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class WikiGenExtension {

    private final Property<String> wikiFilesPrefix;
    private final DirectoryProperty wikiDir;

    @Inject
    public WikiGenExtension(Project project) {
        ObjectFactory objects = project.getObjects();
        wikiFilesPrefix = objects.property(String.class);
        wikiDir = objects.directoryProperty();
    }

    public Property<String> getWikiFilesPrefix() {
        return wikiFilesPrefix;
    }

    public DirectoryProperty getWikiDir() {
        return wikiDir;
    }
}
