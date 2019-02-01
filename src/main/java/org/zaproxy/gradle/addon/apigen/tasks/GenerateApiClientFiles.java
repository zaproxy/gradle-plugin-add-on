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
package org.zaproxy.gradle.addon.apigen.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** A task to generate the API client files of an add-on. */
public class GenerateApiClientFiles extends DefaultTask {

    private final Property<String> api;
    private final Property<String> options;
    private final RegularFileProperty messages;
    private final DirectoryProperty baseDir;
    private final ConfigurableFileCollection classpath;

    public GenerateApiClientFiles() {
        ObjectFactory objects = getProject().getObjects();
        api = objects.property(String.class);
        options = objects.property(String.class);
        messages = objects.fileProperty();
        baseDir = objects.directoryProperty();
        baseDir.set(getProject().getRootDir().getParentFile());
        classpath = getProject().files();
    }

    @Input
    public Property<String> getApi() {
        return api;
    }

    @Input
    @Optional
    public Property<String> getOptions() {
        return options;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getMessages() {
        return messages;
    }

    @Input
    @Optional
    public DirectoryProperty getBaseDir() {
        return baseDir;
    }

    @Classpath
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @TaskAction
    public void generate() throws IOException {
        Path wd = getTemporaryDir().toPath();

        // For compatibility with ZAP <= 2.7.0.
        Path messagesProperties = wd.resolve("resources/lang/Messages.properties");
        Files.createDirectories(messagesProperties.getParent());
        if (messages.isPresent()) {
            Files.copy(
                    messages.get().getAsFile().toPath(),
                    messagesProperties,
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(messagesProperties);
            Files.createFile(messagesProperties);
        }

        Path apiGenJar = wd.resolve("apigen.jar");
        try (InputStream in =
                GenerateApiClientFiles.class
                        .getClassLoader()
                        .getResourceAsStream("org/zaproxy/gradle/addon/apigen/apigen.jar")) {
            Files.copy(in, apiGenJar, StandardCopyOption.REPLACE_EXISTING);
        }

        try (OutputStream out = Files.newOutputStream(wd.resolve("apigen.properties"))) {
            Properties conf = new Properties();
            conf.setProperty("api", api.get());
            conf.setProperty("options", options.isPresent() ? options.get() : "");
            conf.setProperty("basedir", baseDir.get().getAsFile().getAbsolutePath());
            conf.store(out, null);
        }

        getProject()
                .javaexec(
                        spec -> {
                            spec.setClasspath(getProject().files(classpath, apiGenJar.toFile()));
                            spec.setWorkingDir(wd.toFile());
                            spec.setMain("org.zaproxy.zap.extension.api.ApiGenerator");
                        });
    }
}
