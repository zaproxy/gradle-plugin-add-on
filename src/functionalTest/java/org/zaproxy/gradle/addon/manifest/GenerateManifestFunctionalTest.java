/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2026 The ZAP Development Team
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
package org.zaproxy.gradle.addon.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.zaproxy.gradle.addon.AddOnStatus;
import org.zaproxy.gradle.addon.FunctionalTest;

class GenerateManifestFunctionalTest extends FunctionalTest {

    private static final String GENERATE_MANIFEST_TASK = ":generateZapAddOnManifest";
    private static final String MANIFEST_PATH = "build/zapAddOn/ZapAddOn.xml";

    @Override
    protected void buildFile(String content) throws Exception {
        super.buildFile(
                """
                plugins {
                    java
                    id("org.zaproxy.add-on")
                }
                repositories {
                    mavenCentral()
                }
                """
                        + content);
    }

    @Test
    void shouldGenerateManifestWithMinimalConfiguration() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Path manifest = projectDir.resolve(MANIFEST_PATH);
        assertThat(manifest).exists();
        Document doc = parseManifest(manifest);
        assertThat(xpath(doc, "/zapaddon/name")).isEqualTo("Test Add-On");
        assertThat(xpath(doc, "/zapaddon/version")).isEqualTo("1");
        assertThat(xpath(doc, "/zapaddon/status")).isEqualTo("alpha");
    }

    @ParameterizedTest
    @EnumSource(AddOnStatus.class)
    void shouldGenerateManifestWithStatus(AddOnStatus status) throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    addOnStatus.set(org.zaproxy.gradle.addon.AddOnStatus.%s)
                }
                """
                        .formatted(status.name()));

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/status")).isEqualTo(status.toString());
    }

    @Test
    void shouldGenerateManifestWithMetadata() throws Exception {
        // Given
        buildFile(
                """
                version = "2"
                zapAddOn {
                    addOnName.set("My Add-On")
                    manifest {
                        semVer.set("2.0.0")
                        description.set("A test add-on.")
                        author.set("Test Author")
                        url.set("https://example.com")
                        changes.set("Initial release.")
                        repo.set("https://github.com/example/test")
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/name")).isEqualTo("My Add-On");
        assertThat(xpath(doc, "/zapaddon/version")).isEqualTo("2");
        assertThat(xpath(doc, "/zapaddon/semver")).isEqualTo("2.0.0");
        assertThat(xpath(doc, "/zapaddon/description")).isEqualTo("A test add-on.");
        assertThat(xpath(doc, "/zapaddon/author")).isEqualTo("Test Author");
        assertThat(xpath(doc, "/zapaddon/url")).isEqualTo("https://example.com");
        assertThat(xpath(doc, "/zapaddon/changes")).isEqualTo("Initial release.");
        assertThat(xpath(doc, "/zapaddon/repo")).isEqualTo("https://github.com/example/test");
    }

    @Test
    void shouldGenerateManifestWithDependencies() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        dependencies {
                            javaVersion.set("11")
                            addOns {
                                register("requiredaddon") {
                                    version.set("1.0.0")
                                    semVer.set("1.0.0")
                                    notBeforeVersion.set(5)
                                    notFromVersion.set(10)
                                }
                                register("anotheraddon")
                            }
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/dependencies/javaversion")).isEqualTo("11");
        assertThat(xpathCount(doc, "/zapaddon/dependencies/addons/addon")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[1]/id"))
                .isEqualTo("anotheraddon");
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[2]/id"))
                .isEqualTo("requiredaddon");
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[2]/version")).isEqualTo("1.0.0");
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[2]/semver")).isEqualTo("1.0.0");
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[2]/not-before-version"))
                .isEqualTo("5");
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[2]/not-from-version"))
                .isEqualTo("10");
    }

    @Test
    void shouldGenerateManifestWithJavaVersionDependencyOnly() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        dependencies {
                            javaVersion.set("17")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/dependencies/javaversion")).isEqualTo("17");
        assertThat(xpathCount(doc, "/zapaddon/dependencies/addons/addon")).isEqualTo(0);
    }

    @Test
    void shouldGenerateManifestWithNotBeforeVersionAndNotFromVersion() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        notBeforeVersion.set("10")
                        notFromVersion.set("20")
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/not-before-version")).isEqualTo("10");
        assertThat(xpath(doc, "/zapaddon/not-from-version")).isEqualTo("20");
    }

    @Test
    void shouldNotWriteZeroVersionConstraintsForAddOnDependencies() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        dependencies {
                            addOns {
                                register("testaddon") {
                                    notBeforeVersion.set(0)
                                    notFromVersion.set(0)
                                }
                            }
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[1]/not-before-version"))
                .isEmpty();
        assertThat(xpath(doc, "/zapaddon/dependencies/addons/addon[1]/not-from-version")).isEmpty();
    }

    @Test
    void shouldGenerateManifestWithBundle() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        bundle {
                            baseName.set("Messages")
                            prefix.set("org.example")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/bundle")).isEqualTo("Messages");
        assertThat(xpath(doc, "/zapaddon/bundle/@prefix")).isEqualTo("org.example");
    }

    @Test
    void shouldGenerateManifestWithHelpSet() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        helpSet {
                            baseName.set("help/helpset.hs")
                            localeToken.set("%%locale%%")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/helpset")).isEqualTo("help/helpset.hs");
        assertThat(xpath(doc, "/zapaddon/helpset/@localetoken")).isEqualTo("%%locale%%");
    }

    @Test
    void shouldGenerateManifestWithClassnames() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        classnames {
                            allowed.set(listOf("com.example.AllowedClass"))
                            restricted.set(listOf("com.example.RestrictedClass"))
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/classnames/allowed")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/classnames/allowed"))
                .isEqualTo("com.example.AllowedClass");
        assertThat(xpathCount(doc, "/zapaddon/classnames/restricted")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/classnames/restricted"))
                .isEqualTo("com.example.RestrictedClass");
    }

    @Test
    void shouldGenerateManifestWithBundledLibs() throws Exception {
        // Given
        createFile("", projectDir.resolve("mylib.jar"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        bundledLibs {
                            libs.from(files("mylib.jar"))
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/libs/lib")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/libs/lib[1]")).isEqualTo("libs/mylib.jar");
    }

    @Test
    void shouldGenerateManifestWithBundledLibsFromConfiguration() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                val customConfiguration by configurations.creating
                dependencies {
                    customConfiguration("commons-codec:commons-codec:1.17.1")
                }
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        bundledLibs {
                            libs.from(customConfiguration)
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/libs/lib")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/libs/lib[1]")).isEqualTo("libs/commons-codec-1.17.1.jar");
    }

    @Test
    void shouldGenerateManifestWithBundledLibsInCustomDir() throws Exception {
        // Given
        createFile("", projectDir.resolve("mylib.jar"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        bundledLibs {
                            dirName.set("thirdparty")
                            libs.from(files("mylib.jar"))
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/libs/lib")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/libs/lib[1]")).isEqualTo("thirdparty/mylib.jar");
    }

    @Test
    void shouldGenerateManifestWithMultipleBundledLibsSorted() throws Exception {
        // Given
        createFile("", projectDir.resolve("zlib.jar"));
        createFile("", projectDir.resolve("alib.jar"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        bundledLibs {
                            libs.from(files("zlib.jar", "alib.jar"))
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/libs/lib")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/libs/lib[1]")).isEqualTo("libs/alib.jar");
        assertThat(xpath(doc, "/zapaddon/libs/lib[2]")).isEqualTo("libs/zlib.jar");
    }

    @Test
    void shouldGenerateManifestWithChangesFromFile() throws Exception {
        // Given
        createFile("Fix: resolved the issue.", projectDir.resolve("CHANGES.md"));
        buildFile(
                """
                version = "3"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        changesFile.set(file("CHANGES.md"))
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpath(doc, "/zapaddon/changes")).isEqualTo("Fix: resolved the issue.");
    }

    @Test
    void shouldFailWhenVersionIsUnspecified() throws Exception {
        // Given
        buildFile(
                """
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = buildAndFail(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskFailed(result, GENERATE_MANIFEST_TASK);
        assertThat(result.getOutput()).contains("No version specified for the add-on.");
    }

    @Test
    void shouldFailWhenBothChangesAndChangesFileAreSet() throws Exception {
        // Given
        createFile("Changes content.", projectDir.resolve("CHANGES.md"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        changes.set("Changes text.")
                        changesFile.set(file("CHANGES.md"))
                    }
                }
                """);

        // When
        BuildResult result = buildAndFail(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskFailed(result, GENERATE_MANIFEST_TASK);
        assertThat(result.getOutput()).contains("Only one type of changes property must be set.");
    }

    @Test
    void shouldIncludeExtensionsInManifest() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        extensions {
                            register("com.example.ExtensionAlpha")
                            register("com.example.ExtensionBeta")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]"))
                .isEqualTo("com.example.ExtensionAlpha");
        assertThat(xpath(doc, "/zapaddon/extensions/extension[2]"))
                .isEqualTo("com.example.ExtensionBeta");
    }

    @Test
    void shouldIncludeExtensionWithDependenciesInManifest() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        extensions {
                            register("com.example.MyExtension") {
                                classnames {
                                    allowed.set(listOf("com.example.Allowed"))
                                    restricted.set(listOf("com.example.Restricted"))
                                }
                                dependencies {
                                    addOns {
                                        register("requiredaddon") {
                                            version.set("1.0.0")
                                            semVer.set("1.0.0")
                                            notBeforeVersion.set(5)
                                            notFromVersion.set(10)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]/@v")).isEqualTo("1");
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]/classname"))
                .isEqualTo("com.example.MyExtension");
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]/classnames/allowed"))
                .isEqualTo("com.example.Allowed");
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]/classnames/restricted"))
                .isEqualTo("com.example.Restricted");
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]/dependencies/addons/addon[1]/id"))
                .isEqualTo("requiredaddon");
        assertThat(
                        xpath(
                                doc,
                                "/zapaddon/extensions/extension[1]/dependencies/addons/addon[1]/version"))
                .isEqualTo("1.0.0");
        assertThat(
                        xpath(
                                doc,
                                "/zapaddon/extensions/extension[1]/dependencies/addons/addon[1]/semver"))
                .isEqualTo("1.0.0");
        assertThat(
                        xpath(
                                doc,
                                "/zapaddon/extensions/extension[1]/dependencies/addons/addon[1]/not-before-version"))
                .isEqualTo("5");
        assertThat(
                        xpath(
                                doc,
                                "/zapaddon/extensions/extension[1]/dependencies/addons/addon[1]/not-from-version"))
                .isEqualTo("10");
    }

    @Test
    void shouldIncludeExtensionWithExtensionDependenciesInManifest() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        extensions {
                            register("com.example.MyExtension") {
                                dependencies {
                                    extensions {
                                        register("com.example.RequiredExtension")
                                    }
                                }
                            }
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(1);
        assertThat(
                        xpath(
                                doc,
                                "/zapaddon/extensions/extension[1]/dependencies/extensions/extension[1]"))
                .isEqualTo("com.example.RequiredExtension");
    }

    @Test
    void shouldIncludeActiveScanRulesInManifest() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        ascanrules {
                            register("com.example.ActiveRuleAlpha")
                            register("com.example.ActiveRuleBeta")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.ActiveRuleAlpha");
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[2]"))
                .isEqualTo("com.example.ActiveRuleBeta");
    }

    @Test
    void shouldIncludePassiveScanRulesInManifest() throws Exception {
        // Given
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        pscanrules {
                            register("com.example.PassiveRuleAlpha")
                            register("com.example.PassiveRuleBeta")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/pscanrules/pscanrule")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/pscanrules/pscanrule[1]"))
                .isEqualTo("com.example.PassiveRuleAlpha");
        assertThat(xpath(doc, "/zapaddon/pscanrules/pscanrule[2]"))
                .isEqualTo("com.example.PassiveRuleBeta");
    }

    @Test
    void shouldDiscoverExtensionInManifest() throws Exception {
        // Given
        createFile(
                """
                package com.example;
                import org.parosproxy.paros.extension.ExtensionAdaptor;
                public class MyExtension extends ExtensionAdaptor {
                    @Override public String getAuthor() { return ""; }
                }
                """,
                projectDir.resolve("src/main/java/com/example/MyExtension.java"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]"))
                .isEqualTo("com.example.MyExtension");
    }

    @Test
    void shouldNotDiscoverNonQualifyingExtensionsInManifest() throws Exception {
        // Given
        createFile(
                """
                package com.example;
                import org.parosproxy.paros.extension.ExtensionAdaptor;
                public class ValidExtension extends ExtensionAdaptor {
                    @Override public String getAuthor() { return ""; }
                }
                """,
                projectDir.resolve("src/main/java/com/example/ValidExtension.java"));
        createFile(
                """
                package com.example;
                public class PrivateConstructorExtension extends ValidExtension {
                    private PrivateConstructorExtension() {}
                }
                """,
                projectDir.resolve("src/main/java/com/example/PrivateConstructorExtension.java"));
        createFile(
                """
                package com.example;
                class PackagePrivateExtension extends ValidExtension {}
                """,
                projectDir.resolve("src/main/java/com/example/PackagePrivateExtension.java"));
        createFile(
                """
                package com.example;
                public abstract class AbstractExtension extends ValidExtension {}
                """,
                projectDir.resolve("src/main/java/com/example/AbstractExtension.java"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]"))
                .isEqualTo("com.example.ValidExtension");
    }

    @Test
    void shouldDiscoverActiveScanRuleInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyActiveScanRule",
                "AbstractPlugin",
                "org.parosproxy.paros.core.scanner.AbstractPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyActiveScanRule");
    }

    @Test
    void shouldNotDiscoverNonQualifyingActiveScanRulesInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "ValidActiveScanRule",
                "AbstractPlugin",
                "org.parosproxy.paros.core.scanner.AbstractPlugin");
        createFile(
                """
                package com.example;
                public class PrivateConstructorScanRule extends ValidActiveScanRule {
                    private PrivateConstructorScanRule() {}
                }
                """,
                projectDir.resolve("src/main/java/com/example/PrivateConstructorScanRule.java"));
        createFile(
                """
                package com.example;
                class PackagePrivateScanRule extends ValidActiveScanRule {}
                """,
                projectDir.resolve("src/main/java/com/example/PackagePrivateScanRule.java"));
        createFile(
                """
                package com.example;
                public abstract class AbstractScanRule extends ValidActiveScanRule {}
                """,
                projectDir.resolve("src/main/java/com/example/AbstractScanRule.java"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.ValidActiveScanRule");
    }

    @Test
    void shouldDiscoverPassiveScanRuleInManifest() throws Exception {
        // Given
        writePassiveScanRule("MyPassiveScanRule");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/pscanrules/pscanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/pscanrules/pscanrule[1]"))
                .isEqualTo("com.example.MyPassiveScanRule");
    }

    @Test
    void shouldNotDiscoverNonQualifyingPassiveScanRulesInManifest() throws Exception {
        // Given
        writePassiveScanRule("ValidPassiveScanRule");
        createFile(
                """
                package com.example;
                public class PrivateConstructorPassiveScanRule extends ValidPassiveScanRule {
                    private PrivateConstructorPassiveScanRule() {}
                }
                """,
                projectDir.resolve(
                        "src/main/java/com/example/PrivateConstructorPassiveScanRule.java"));
        createFile(
                """
                package com.example;
                class PackagePrivatePassiveScanRule extends ValidPassiveScanRule {}
                """,
                projectDir.resolve("src/main/java/com/example/PackagePrivatePassiveScanRule.java"));
        createFile(
                """
                package com.example;
                public abstract class AbstractPassiveScanRule extends ValidPassiveScanRule {}
                """,
                projectDir.resolve("src/main/java/com/example/AbstractPassiveScanRule.java"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/pscanrules/pscanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/pscanrules/pscanrule[1]"))
                .isEqualTo("com.example.ValidPassiveScanRule");
    }

    @Test
    void shouldNotDuplicateDiscoveredPassiveScanRuleAlreadyRegisteredInManifest() throws Exception {
        // Given
        writePassiveScanRule("MyPassiveScanRule");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        pscanrules {
                            register("com.example.MyPassiveScanRule")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/pscanrules/pscanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/pscanrules/pscanrule[1]"))
                .isEqualTo("com.example.MyPassiveScanRule");
    }

    @Test
    void shouldDiscoverActiveScanRuleExtendingAbstractPluginInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyAbstractPluginScanRule",
                "AbstractPlugin",
                "org.parosproxy.paros.core.scanner.AbstractPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyAbstractPluginScanRule");
    }

    @Test
    void shouldDiscoverActiveScanRuleExtendingAbstractHostPluginInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyHostPluginScanRule",
                "AbstractHostPlugin",
                "org.parosproxy.paros.core.scanner.AbstractHostPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyHostPluginScanRule");
    }

    @Test
    void shouldDiscoverActiveScanRuleExtendingAbstractAppPluginInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyAppPluginScanRule",
                "AbstractAppPlugin",
                "org.parosproxy.paros.core.scanner.AbstractAppPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyAppPluginScanRule");
    }

    @Test
    void shouldDiscoverActiveScanRuleExtendingAbstractAppParamPluginInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyParamPluginScanRule",
                "AbstractAppParamPlugin",
                "org.parosproxy.paros.core.scanner.AbstractAppParamPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyParamPluginScanRule");
    }

    @Test
    void shouldDiscoverActiveScanRuleWithMultipleLevelClassHierarchyInManifest() throws Exception {
        // Given
        createFile(
                """
                package com.example;
                import org.parosproxy.paros.core.scanner.AbstractHostPlugin;
                public abstract class BaseCustomScanRule extends AbstractHostPlugin {}
                """,
                projectDir.resolve("src/main/java/com/example/BaseCustomScanRule.java"));
        writeActiveScanRule("MyConcreteScanRule", "BaseCustomScanRule", null);
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyConcreteScanRule");
    }

    @Test
    void shouldNotDuplicateDiscoveredExtensionAlreadyRegisteredInManifest() throws Exception {
        // Given
        createFile(
                """
                package com.example;
                import org.parosproxy.paros.extension.ExtensionAdaptor;
                public class MyExtension extends ExtensionAdaptor {
                    @Override public String getAuthor() { return ""; }
                }
                """,
                projectDir.resolve("src/main/java/com/example/MyExtension.java"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        extensions {
                            register("com.example.MyExtension")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/extensions/extension")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/extensions/extension[1]"))
                .isEqualTo("com.example.MyExtension");
    }

    @Test
    void shouldNotDuplicateDiscoveredActiveScanRuleAlreadyRegisteredInManifest() throws Exception {
        // Given
        writeActiveScanRule(
                "MyActiveScanRule",
                "AbstractPlugin",
                "org.parosproxy.paros.core.scanner.AbstractPlugin");
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                    manifest {
                        ascanrules {
                            register("com.example.MyActiveScanRule")
                        }
                    }
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/ascanrules/ascanrule")).isEqualTo(1);
        assertThat(xpath(doc, "/zapaddon/ascanrules/ascanrule[1]"))
                .isEqualTo("com.example.MyActiveScanRule");
    }

    @Test
    void shouldIncludeFilesInManifest() throws Exception {
        // Given
        createFile("", projectDir.resolve("src/main/zapHomeFiles/config.xml"));
        createFile("", projectDir.resolve("src/main/zapHomeFiles/scripts/myscript.js"));
        buildFile(
                """
                version = "1"
                zapAddOn {
                    addOnName.set("Test Add-On")
                }
                """);

        // When
        BuildResult result = build(GENERATE_MANIFEST_TASK);

        // Then
        assertTaskSuccess(result, GENERATE_MANIFEST_TASK);
        Document doc = parseManifest(projectDir.resolve(MANIFEST_PATH));
        assertThat(xpathCount(doc, "/zapaddon/files/file")).isEqualTo(2);
        assertThat(xpath(doc, "/zapaddon/files/file[1]")).isEqualTo("config.xml");
        assertThat(xpath(doc, "/zapaddon/files/file[2]")).isEqualTo("scripts/myscript.js");
    }

    private void writeActiveScanRule(String className, String superClass, String superImport)
            throws Exception {
        String importStatement = superImport != null ? "import " + superImport + ";" : "";
        createFile(
                """
                package com.example;
                import org.parosproxy.paros.core.scanner.HostProcess;
                import org.parosproxy.paros.network.HttpMessage;
                %s
                public class %s extends %s {
                    @Override public int getId() { return 0; }
                    @Override public String getName() { return ""; }
                    @Override public String[] getDependency() { return null; }
                    @Override public String getDescription() { return ""; }
                    @Override public int getCategory() { return 0; }
                    @Override public String getSolution() { return ""; }
                    @Override public String getReference() { return ""; }
                    @Override public void scan() {}
                    public void scan(HttpMessage msg, String param, String value) {}
                    @Override public void notifyPluginCompleted(HostProcess hp) {}
                    @Override public int getRisk() { return 0; }
                    @Override public int getCweId() { return 0; }
                    @Override public int getWascId() { return 0; }
                }
                """
                        .formatted(importStatement, className, superClass),
                projectDir.resolve("src/main/java/com/example/" + className + ".java"));
    }

    private void writePassiveScanRule(String className) throws Exception {
        createFile(
                """
                package com.example;
                import net.htmlparser.jericho.Source;
                import org.parosproxy.paros.network.HttpMessage;
                import org.zaproxy.zap.extension.pscan.PassiveScanThread;
                import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;
                public class %s extends PluginPassiveScanner {
                    @Override public String getName() { return ""; }
                    @Override public void scanHttpRequestSend(HttpMessage msg, int id) {}
                    @Override public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {}
                    @Override public void setParent(PassiveScanThread parent) {}
                }
                """
                        .formatted(className),
                projectDir.resolve("src/main/java/com/example/" + className + ".java"));
    }

    private static Document parseManifest(Path manifestFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(manifestFile.toFile());
    }

    private static String xpath(Document doc, String expression) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return xpath.evaluate(expression, doc);
    }

    private static int xpathCount(Document doc, String expression) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        return nodes.getLength();
    }
}
