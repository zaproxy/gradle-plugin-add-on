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
package org.zaproxy.gradle.addon.internal.model;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.zaproxy.gradle.addon.AddOnPlugin;
import org.zaproxy.gradle.addon.AddOnPluginExtension;
import org.zaproxy.gradle.addon.internal.GitHubReleaseExtension;

public abstract class AddOnRelease {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getAddOn();

    @Input
    public abstract Property<String> getDownloadUrl();

    public static AddOnRelease from(Project project) {
        TaskProvider<Jar> jarZapAddOn =
                project.getTasks().named(AddOnPlugin.JAR_ZAP_ADD_ON_TASK_NAME, Jar.class);
        Provider<RegularFile> jarFile = jarZapAddOn.flatMap(Jar::getArchiveFile);
        AddOnPluginExtension extension =
                project.getExtensions().getByType(AddOnPluginExtension.class);
        GitHubReleaseExtension gitHubReleaseExtension =
                ((ExtensionAware) extension)
                        .getExtensions()
                        .getByType(GitHubReleaseExtension.class);

        AddOnRelease addOnRelease = project.getObjects().newInstance(AddOnRelease.class);
        addOnRelease.getAddOn().set(jarZapAddOn.flatMap(Jar::getArchiveFile));
        addOnRelease
                .getDownloadUrl()
                .set(
                        jarFile.map(
                                f ->
                                        String.format(
                                                "https://github.com/%s/releases/download/v%s/%s",
                                                gitHubReleaseExtension.getRepo().get(),
                                                extension.getAddOnVersion().get(),
                                                f.getAsFile().getName())));
        return addOnRelease;
    }
}
