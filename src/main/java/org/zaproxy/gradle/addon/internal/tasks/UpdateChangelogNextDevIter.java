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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdateChangelogNextDevIter extends DefaultTask {

    private static final String CURRENT_VERSION_TOKEN = "@CURRENT_VERSION@";

    private static final String UNRELEASED_SECTION = "## Unreleased";
    private static final String UNRELEASED_SECTION_LINK = "## [Unreleased]";
    private static final Pattern VERSION_SECTION_PATTERN = Pattern.compile("^## \\[?.+]?.*");
    private static final Pattern VERSION_LINK_PATTERN = Pattern.compile("^\\[.+]:");

    private final Property<String> currentVersionToken;
    private final Property<String> unreleasedLink;

    public UpdateChangelogNextDevIter() {
        ObjectFactory objects = getProject().getObjects();
        this.currentVersionToken = objects.property(String.class).value(CURRENT_VERSION_TOKEN);
        this.unreleasedLink = objects.property(String.class);
    }

    @Input
    public abstract Property<String> getCurrentVersion();

    @Input
    public Property<String> getCurrentVersionToken() {
        return currentVersionToken;
    }

    @Input
    @Optional
    public Property<String> getUnreleasedLink() {
        return unreleasedLink;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getChangelog();

    @TaskAction
    public void prepare() throws IOException {
        Path updatedChangelog = updateChangelog();

        Files.copy(
                updatedChangelog,
                getChangelog().getAsFile().get().toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private Path updateChangelog() throws IOException {
        Path changelogPath = getChangelog().getAsFile().get().toPath();
        Path updatedChangelog =
                getTemporaryDir().toPath().resolve("updated-" + changelogPath.getFileName());

        boolean insertUnreleased = true;
        boolean insertUnreleasedLink = unreleasedLink.isPresent();

        try (BufferedReader reader = Files.newBufferedReader(changelogPath);
                BufferedWriter writer = Files.newBufferedWriter(updatedChangelog)) {
            boolean lastLineEmpty = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (insertUnreleased) {
                    if (line.startsWith(UNRELEASED_SECTION)
                            || line.startsWith(UNRELEASED_SECTION_LINK)) {
                        throw new InvalidUserDataException(
                                "The changelog already contains the unreleased section.");
                    }

                    if (VERSION_SECTION_PATTERN.matcher(line).find()) {
                        writer.write(
                                insertUnreleasedLink
                                        ? UNRELEASED_SECTION_LINK
                                        : UNRELEASED_SECTION);
                        writer.write("\n\n\n");
                        insertUnreleased = false;
                    }
                }
                if (insertUnreleasedLink && VERSION_LINK_PATTERN.matcher(line).find()) {
                    writeUnreleaseLink(writer);
                    insertUnreleasedLink = false;
                }

                writer.write(line);
                writer.write("\n");
                lastLineEmpty = line.isEmpty();
            }

            if (insertUnreleasedLink) {
                if (!lastLineEmpty) {
                    writer.write("\n");
                }
                writeUnreleaseLink(writer);
            }
        }

        if (insertUnreleased) {
            throw new InvalidUserDataException(
                    "Failed to insert the unreleased section, no version section found.");
        }

        return updatedChangelog;
    }

    private void writeUnreleaseLink(Writer writer) throws IOException {
        String link =
                unreleasedLink.get().replace(currentVersionToken.get(), getCurrentVersion().get());
        writer.write("[Unreleased]: " + link + "\n");
    }
}
