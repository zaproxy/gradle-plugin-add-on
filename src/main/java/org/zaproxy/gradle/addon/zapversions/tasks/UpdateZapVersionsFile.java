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
package org.zaproxy.gradle.addon.zapversions.tasks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;
import org.zaproxy.gradle.addon.internal.DefaultIndenter;
import org.zaproxy.gradle.addon.internal.model.zapversions.AddOn;
import org.zaproxy.gradle.addon.internal.model.zapversions.ZapVersions;

/**
 * A task that updates {@code ZapVersions.xml} files with a {@code ZapVersions.xml} from an add-on.
 */
public class UpdateZapVersionsFile extends DefaultTask {

    private final WorkerExecutor workerExecutor;
    private final RegularFileProperty from;
    private final ConfigurableFileCollection into;

    @Inject
    public UpdateZapVersionsFile(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
        this.from = getProject().getObjects().fileProperty();
        this.into = getProject().files();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getFrom() {
        return from;
    }

    @Internal
    @SkipWhenEmpty
    public ConfigurableFileCollection getInto() {
        return into;
    }

    @TaskAction
    public void update() throws IOException {
        ZapVersions zapVersions =
                createXmlMapper().readValue(from.get().getAsFile(), ZapVersions.class);
        for (File file : into.getFiles()) {
            if (Files.notExists(file.toPath())) {
                throw new IOException("The file " + file.toPath() + " does not exist.");
            }

            AddOn addOn = zapVersions.addOns.first();
            workerExecutor.submit(
                    UpdateZapVersionsEntry.class,
                    config -> {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(addOn, file);
                    });
        }
    }

    protected static XmlMapper createXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        return mapper;
    }

    public static class UpdateZapVersionsEntry implements Runnable {

        private final AddOn addOn;
        private final File file;

        @Inject
        public UpdateZapVersionsEntry(AddOn addOn, File file) {
            this.addOn = addOn;
            this.file = file;
        }

        @Override
        public void run() {
            try {
                XmlMapper mapper = createXmlMapper();
                ZapVersions zapVersions = mapper.readValue(file, ZapVersions.class);
                zapVersions.addOns.remove(addOn);
                zapVersions.addOns.add(addOn);

                DefaultXmlPrettyPrinter.Indenter indenter = new DefaultIndenter();
                DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinter();
                printer.indentObjectsWith(indenter);
                printer.indentArraysWith(indenter);
                mapper.writer(printer).writeValue(file, zapVersions);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to update: " + file, e);
            }
        }
    }
}
