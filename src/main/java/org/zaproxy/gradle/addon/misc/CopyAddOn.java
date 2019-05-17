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
package org.zaproxy.gradle.addon.misc;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;
import org.zaproxy.gradle.addon.internal.Constants;

/**
 * A task to copy the add-on to a directory.
 *
 * <p>Defaults to {@code $rootDir/../zaproxy/zap/src/main/dist/plugin/}.
 *
 * <p>Existing add-ons with the same ID are removed from the destination directory before copying
 * the add-on, to ensure ZAP uses the copied add-on.
 */
public class CopyAddOn extends Copy {

    private static final String DEFAULT_DIR_PATH = "../zaproxy/zap/src/main/dist/plugin/";

    private final Property<String> addOnId;

    public CopyAddOn() {
        Project project = getProject();
        addOnId = project.getObjects().property(String.class);

        from(project.getTasks().named("jarZapAddOn"));
        into(new File(project.getRootDir(), DEFAULT_DIR_PATH));

        getOutputs()
                .upToDateWhen(
                        task -> {
                            Path dir = getDestinationDir().toPath();
                            if (!Files.exists(dir)) {
                                return true;
                            }
                            Pattern addOnIdPattern =
                                    Pattern.compile(
                                            Pattern.quote(getAddOnId().get())
                                                    + "-.*\\."
                                                    + Constants.ADD_ON_FILE_EXTENSION);
                            try (Stream<Path> stream =
                                    Files.find(
                                            dir,
                                            1,
                                            (p, a) ->
                                                    addOnIdPattern
                                                            .matcher(p.getFileName().toString())
                                                            .matches())) {
                                return stream.count() <= 1;
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
    }

    @Input
    public Property<String> getAddOnId() {
        return addOnId;
    }

    @Option(option = "into", description = "The file system path to the directory.")
    public void optionInto(String dir) {
        into(getProject().file(dir));
    }

    @Override
    public void copy() {
        Project project = getProject();
        project.delete(
                project.fileTree(
                                getDestinationDir(),
                                tree -> {
                                    tree.include(
                                            getAddOnId().get()
                                                    + "-*."
                                                    + Constants.ADD_ON_FILE_EXTENSION);
                                })
                        .getFiles());
        super.copy();
    }
}
