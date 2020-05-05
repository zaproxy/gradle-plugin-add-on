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
package org.zaproxy.gradle.addon.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/** A task that updates the latest version of the changelog with a change. */
public class UpdateChangelog extends DefaultTask {

    public enum ChangeType {
        ADDED,
        CHANGED,
        DEPRECATED,
        REMOVED,
        FIXED,
        SECURITY;

        Pattern pattern() {
            return Pattern.compile("(?i)### ?" + Pattern.quote(name()) + '\n');
        }

        String markdown() {
            return "### " + StringUtils.capitalize(name().toLowerCase(Locale.ROOT)) + '\n';
        }
    }

    private enum ParseState {
        FIND_VERSION,
        CONSUME_VERSION,
        WRITE
    }

    private static final Pattern VERSION_SECTION_PATTERN = Pattern.compile("^##[^#]");

    private final RegularFileProperty changelog;
    private final Property<ChangeType> changeType;
    private final Property<String> change;
    private final Property<Boolean> duplicate;

    public UpdateChangelog() {
        ObjectFactory objects = getProject().getObjects();
        this.changelog = objects.fileProperty();
        this.changeType = objects.property(ChangeType.class).convention(ChangeType.CHANGED);
        this.change = objects.property(String.class);
        this.duplicate = objects.property(Boolean.class).convention(Boolean.FALSE);

        setGroup("ZAP Add-On Misc");
        setDescription("Updates the latest version of the changelog with a change.");
    }

    @Option(option = "changelog", description = "The path to the changelog file.")
    public void optionChangelog(String path) {
        changelog.set(getProject().file(path));
    }

    @Internal
    public RegularFileProperty getChangelog() {
        return changelog;
    }

    @Option(
            option = "change-type",
            description = "The type of change (case-insensitive), e.g. Added, Changed, Removed...")
    public void optionChangeType(String type) {
        try {
            changeType.set(ChangeType.valueOf(type.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "The specified change type is not in the supported set: "
                            + Arrays.toString(ChangeType.values()));
        }
    }

    @Internal
    public Property<ChangeType> getChangeType() {
        return changeType;
    }

    @Option(option = "change", description = "The change.")
    public void optionChange(String value) {
        change.set(value);
    }

    @Internal
    public Property<String> getChange() {
        return change;
    }

    @Option(
            option = "duplicate",
            description = "If the change should be added even if already present. Default: false.")
    public void optionDuplicate(String value) {
        duplicate.set(Boolean.valueOf(value));
    }

    @Internal
    public Property<Boolean> getDuplicate() {
        return duplicate;
    }

    @TaskAction
    public void update() throws IOException {
        Path changelogPath = changelog.getAsFile().get().toPath();
        Path updatedChangelog = updateChangelog(changelogPath);

        Files.copy(updatedChangelog, changelogPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path updateChangelog(Path changelogPath) throws IOException {
        Path updatedChangelog =
                getTemporaryDir().toPath().resolve("updated-" + changelogPath.getFileName());

        try (BufferedReader reader = Files.newBufferedReader(changelogPath);
                BufferedWriter writer = Files.newBufferedWriter(updatedChangelog)) {

            ParseState parseState = ParseState.FIND_VERSION;
            StringBuilder versionContents = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                switch (parseState) {
                    case FIND_VERSION:
                        writer.append(line).append('\n');
                        if (isVersionStart(line)) {
                            parseState = ParseState.CONSUME_VERSION;
                        }
                        break;
                    case CONSUME_VERSION:
                        if (isVersionStart(line)) {
                            writer.write(addChange(versionContents));
                            writer.append(line).append('\n');
                            parseState = ParseState.WRITE;
                        } else {
                            versionContents.append(line).append('\n');
                        }
                        break;
                    case WRITE:
                    default:
                        writer.append(line).append('\n');
                }
            }

            switch (parseState) {
                case FIND_VERSION:
                    throw new InvalidUserDataException(
                            "Changelog does not have a version section.");
                case CONSUME_VERSION:
                    writer.write(addChange(versionContents));
                    break;
                default:
            }
        }

        return updatedChangelog;
    }

    private static boolean isVersionStart(String line) {
        return VERSION_SECTION_PATTERN.matcher(line).find();
    }

    private String addChange(StringBuilder versionContents) {
        String normalisedChange = change.get().replaceAll("\r\n?", "\n") + '\n';
        if (!isDuplicate()) {
            String contents = versionContents.toString();
            if (contents.contains(normalisedChange)) {
                return contents;
            }
        }

        int insertPosition = 0;
        Pattern pattern = changeType.get().pattern();
        Matcher matcher = pattern.matcher(versionContents);
        if (matcher.find()) {
            insertPosition = matcher.end();
        } else {
            normalisedChange = changeType.get().markdown() + normalisedChange + '\n';
        }
        versionContents.insert(insertPosition, normalisedChange);

        return versionContents.toString();
    }

    private boolean isDuplicate() {
        return duplicate.get();
    }
}
