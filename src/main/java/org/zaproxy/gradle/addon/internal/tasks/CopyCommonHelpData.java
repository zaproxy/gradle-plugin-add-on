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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.zaproxy.gradle.addon.AddOnPluginException;
import org.zaproxy.gradle.addon.internal.Constants;

/** A task to copy common help data for translated languages. */
public abstract class CopyCommonHelpData extends DefaultTask {

    public CopyCommonHelpData() {
        setGroup(LifecycleBasePlugin.BUILD_GROUP);
        setDescription("Copies the common help data for translated languages.");

        getFileNameHelpSetPattern().convention(Constants.HELPSET_LOCALE_PATTERN);
        getCommonData().convention(Arrays.asList("map.jhm", "contents/images/"));
    }

    @Input
    public abstract Property<String> getFileNameHelpSetPattern();

    @InputFiles
    public abstract ListProperty<String> getCommonData();

    @InputFiles
    public abstract ConfigurableFileCollection getHelpSets();

    @TaskAction
    void copy() {
        Set<File> helpSetFiles = getHelpSets().getFiles();
        if (helpSetFiles.isEmpty()) {
            return;
        }

        Pattern helpSetFilePattern = Pattern.compile(getFileNameHelpSetPattern().get());
        Path mainHelpSetDir = findMainHelpSetDir(helpSetFiles, helpSetFilePattern);

        Set<Path> commonData =
                getCommonData().get().stream()
                        .map(e -> mainHelpSetDir.resolve(e))
                        .filter(Files::exists)
                        .collect(Collectors.toSet());
        if (commonData.isEmpty()) {
            return;
        }

        helpSetFiles.stream()
                .filter(e -> helpSetFilePattern.matcher(e.getName()).matches())
                .map(File::toPath)
                .map(Path::getParent)
                .forEach(e -> copy(commonData, mainHelpSetDir, e));
    }

    private Path findMainHelpSetDir(Set<File> helpSetFiles, Pattern helpSetFilePattern) {
        Optional<Path> helpSetFile =
                helpSetFiles.stream()
                        .filter(e -> !helpSetFilePattern.matcher(e.getName()).matches())
                        .map(File::toPath)
                        .findFirst();
        if (!helpSetFile.isPresent()) {
            throw new AddOnPluginException(
                    "Failed to find main help directory, main HelpSet file not found.");
        }
        return helpSetFile.get().getParent();
    }

    private void copy(Set<Path> commonData, Path from, Path to) {
        commonData.forEach(
                data -> {
                    getProject()
                            .copy(
                                    spec -> {
                                        spec.from(data);
                                        if (Files.isDirectory(data)) {
                                            spec.into(to.resolve(from.relativize(data)));
                                        } else {
                                            spec.into(to);
                                        }
                                    });
                });
    }
}
