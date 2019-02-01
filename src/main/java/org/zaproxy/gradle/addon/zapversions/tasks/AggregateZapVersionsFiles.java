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
import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.zaproxy.gradle.addon.internal.DefaultIndenter;
import org.zaproxy.gradle.addon.internal.model.zapversions.ZapVersions;

/** A task that merges {@code ZapVersions.xml} from add-ons into a single file. */
public class AggregateZapVersionsFiles extends DefaultTask {

    private final ConfigurableFileCollection from;
    private final RegularFileProperty into;

    public AggregateZapVersionsFiles() {
        this.from = getProject().files();
        this.into = getProject().getObjects().fileProperty();
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NONE)
    public ConfigurableFileCollection getFrom() {
        return from;
    }

    @OutputFile
    public RegularFileProperty getInto() {
        return into;
    }

    @TaskAction
    public void aggregate() throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

        ZapVersions aggregatedZapVersions = new ZapVersions();

        from.getFiles()
                .forEach(
                        f -> {
                            ZapVersions zapVersions;
                            try {
                                zapVersions = mapper.readValue(f, ZapVersions.class);
                                aggregatedZapVersions.addOns.add(zapVersions.addOns.first());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

        DefaultXmlPrettyPrinter.Indenter indenter = new DefaultIndenter();
        DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        mapper.writer(printer).writeValue(getInto().get().getAsFile(), aggregatedZapVersions);
    }
}
