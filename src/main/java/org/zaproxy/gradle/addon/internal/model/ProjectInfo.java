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

import java.io.File;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;

public abstract class ProjectInfo {

    private static final String FILE_NAME_OUTPUT = "release_state_last_commit.json";

    @Input
    public abstract Property<String> getPropertiesPath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public static ProjectInfo from(Project project) {
        ProjectInfo projectInfo = project.getObjects().newInstance(ProjectInfo.class);
        projectInfo.getOutputFile().set(new File(project.getBuildDir(), FILE_NAME_OUTPUT));
        Path gradleProperties = project.getProjectDir().toPath().resolve(Project.GRADLE_PROPERTIES);
        projectInfo
                .getPropertiesPath()
                .set(project.getRootProject().relativePath(gradleProperties));
        return projectInfo;
    }
}
