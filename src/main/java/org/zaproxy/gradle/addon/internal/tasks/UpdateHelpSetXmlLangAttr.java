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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.xml.sax.SAXException;
import org.zaproxy.gradle.addon.AddOnPluginException;
import org.zaproxy.gradle.addon.internal.Constants;

/**
 * A task to update {@code HelpSet}'s XML {@code lang} attribute based on the locale in the file
 * name.
 */
public abstract class UpdateHelpSetXmlLangAttr extends DefaultTask {

    private static final XPathExpression HELP_SET_NODE_XPATH_EXPRESSION;

    static {
        String expression = "/helpset";
        try {
            HELP_SET_NODE_XPATH_EXPRESSION =
                    XPathFactory.newInstance().newXPath().compile(expression);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(
                    "Failed to compile valid XPath expression: " + expression, e);
        }
    }

    public UpdateHelpSetXmlLangAttr() {
        setGroup(LifecycleBasePlugin.BUILD_GROUP);
        setDescription("Updates the XML lang attribute of HelpSet files to match their locale.");

        getFileNameLocalePattern().convention(Constants.HELPSET_LOCALE_PATTERN);
    }

    @Input
    public abstract Property<String> getFileNameLocalePattern();

    @InputFiles
    public abstract ConfigurableFileCollection getHelpSets();

    @TaskAction
    void update() {
        Pattern extractLocalePattern = Pattern.compile(getFileNameLocalePattern().get());
        getHelpSets()
                .getFiles()
                .forEach(e -> updateHelpSetLangAttribute(extractLocalePattern, e.toPath()));
    }

    private static void updateHelpSetLangAttribute(Pattern extractLocalePattern, Path helpSetFile) {
        String language =
                extractLanguage(extractLocalePattern, helpSetFile.getFileName().toString());
        if (language == null) {
            return;
        }
        updateHelpSetFileLangAttribute(helpSetFile, language);
    }

    private static String extractLanguage(Pattern extractLocalePattern, String helpSetFileName) {
        Matcher matcher = extractLocalePattern.matcher(helpSetFileName);
        if (matcher.matches()) {
            return matcher.group(1).replace('_', '-');
        }
        return null;
    }

    private static void updateHelpSetFileLangAttribute(Path helpSetFile, String language) {
        if (!Files.exists(helpSetFile)) {
            throw new AddOnPluginException(
                    "Specified HelpSet file does not exist: "
                            + helpSetFile.toAbsolutePath().toString());
        }

        Document doc;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(helpSetFile))) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);
            builderFactory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.parse(inputStream);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new AddOnPluginException("Failed to parse HelpSet file: " + e.getMessage(), e);
        }

        Node node;
        try {
            node = (Node) HELP_SET_NODE_XPATH_EXPRESSION.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new AddOnPluginException("Failed to get helpset element: " + e.getMessage(), e);
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            throw new AddOnPluginException("Element helpset does not contain attributes.");
        }

        Node langAttribute = attributes.getNamedItem("xml:lang");
        if (langAttribute == null) {
            throw new AddOnPluginException(
                    "Required xml:lang attribute not found in helpset element.");
        }

        langAttribute.setNodeValue(language);

        DOMImplementationLS domImpLS =
                (DOMImplementationLS) doc.getImplementation().getFeature("LS", "3.0");
        try (OutputStream outputStream = Files.newOutputStream(helpSetFile)) {
            LSOutput lsOutput = domImpLS.createLSOutput();
            lsOutput.setByteStream(outputStream);
            domImpLS.createLSSerializer().write(doc, lsOutput);
        } catch (IOException e) {
            throw new AddOnPluginException(
                    "Failed to change xml:lang attribute:" + e.getMessage(), e);
        }
    }
}
