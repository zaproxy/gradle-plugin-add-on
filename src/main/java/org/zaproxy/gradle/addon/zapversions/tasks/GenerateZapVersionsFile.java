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
import java.io.InputStream;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.zaproxy.gradle.addon.internal.Constants;
import org.zaproxy.gradle.addon.internal.DefaultIndenter;
import org.zaproxy.gradle.addon.internal.model.Manifest;
import org.zaproxy.gradle.addon.internal.model.zapversions.AddOn;
import org.zaproxy.gradle.addon.internal.model.zapversions.ZapVersions;

/** A task that generates a {@code ZapVersions.xml} file for a given add-on. */
public class GenerateZapVersionsFile extends DefaultTask {

    public static final String ZAP_VERSIONS_XML = "ZapVersions.xml";

    private final Property<String> addOnId;
    private final RegularFileProperty addOn;
    private final Property<String> downloadUrl;
    private final Property<String> checksumAlgorithm;
    private final RegularFileProperty file;

    public GenerateZapVersionsFile() {
        ObjectFactory objects = getProject().getObjects();
        this.addOnId = objects.property(String.class);
        this.addOn = objects.fileProperty();
        this.downloadUrl = objects.property(String.class);
        this.checksumAlgorithm = objects.property(String.class).value("SHA1");
        this.file = objects.fileProperty();
    }

    @Input
    public Property<String> getAddOnId() {
        return addOnId;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getAddOn() {
        return addOn;
    }

    @Input
    public Property<String> getDownloadUrl() {
        return downloadUrl;
    }

    @Input
    public Property<String> getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    @OutputFile
    public RegularFileProperty getFile() {
        return file;
    }

    @TaskAction
    public void generate() throws IOException {
        File addOnFile = getAddOn().get().getAsFile();

        Manifest manifest;
        try (ZipFile addOnZip = new ZipFile(addOnFile)) {
            ZipEntry manifestEntry = addOnZip.getEntry(Constants.ADD_ON_MANIFEST_FILE_NAME);
            if (manifestEntry == null) {
                throw new IllegalArgumentException(
                        "The specified add-on does not have the manifest: " + addOnFile);
            }
            try (InputStream is = addOnZip.getInputStream(manifestEntry)) {
                XmlMapper mapper = new XmlMapper();
                mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
                manifest = mapper.readValue(is, Manifest.class);
            }
        }

        AddOn addOnEntry = new AddOn();
        addOnEntry.id = getAddOnId().get();
        addOnEntry.name = manifest.name;
        addOnEntry.description = manifest.description;
        addOnEntry.author = manifest.author;
        addOnEntry.version = manifest.version;
        addOnEntry.semver = manifest.semver;
        addOnEntry.file = addOnFile.getName();
        addOnEntry.status = manifest.status;
        addOnEntry.changes = manifest.changes;
        addOnEntry.url = getDownloadUrl().get() + "/" + addOnFile.getName();
        addOnEntry.hash = createChecksum(checksumAlgorithm.get(), addOnFile);
        addOnEntry.info = manifest.url;
        addOnEntry.date = LocalDate.now().toString();
        addOnEntry.size = String.valueOf(addOnFile.length());
        addOnEntry.notBeforeVersion = manifest.notBeforeVersion;
        addOnEntry.notFromVersion = manifest.notFromVersion;
        addOnEntry.dependencies = manifest.dependencies;

        ZapVersions versions = new ZapVersions();
        versions.addOns.add(addOnEntry);

        DefaultXmlPrettyPrinter.Indenter indenter = new DefaultIndenter();
        DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        XmlMapper mapper = new XmlMapper();
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        mapper.writer(printer).writeValue(getFile().get().getAsFile(), versions);
    }

    private static String createChecksum(String algorithm, File addOnFile) throws IOException {
        return algorithm + ":" + new DigestUtils(algorithm).digestAsHex(addOnFile);
    }
}
