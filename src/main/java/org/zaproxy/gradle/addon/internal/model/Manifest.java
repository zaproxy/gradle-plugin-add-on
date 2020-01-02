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
package org.zaproxy.gradle.addon.internal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonRootName(value = "zapaddon")
public class Manifest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty public String name;

    @JsonProperty public String version;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String semver;

    @JsonProperty public String status;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String description;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String author;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String url;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String changes;

    @JsonProperty
    @JsonInclude(value = Include.NON_EMPTY)
    public String repo;

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public Classnames classnames;

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public Dependencies dependencies;

    @JacksonXmlElementWrapper(localName = "libs")
    @JacksonXmlProperty(localName = "lib")
    @JsonInclude(value = Include.NON_EMPTY)
    public List<String> libs = new ArrayList<>();

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public Bundle bundle;

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public Helpset helpset;

    @JacksonXmlElementWrapper(localName = "extensions")
    @JacksonXmlProperty(localName = "extension")
    @JsonSerialize(contentConverter = ExtensionConverter.class)
    @JsonInclude(value = Include.NON_EMPTY)
    public List<Extension> extensions = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "ascanrules")
    @JacksonXmlProperty(localName = "ascanrule")
    @JsonInclude(value = Include.NON_EMPTY)
    public List<String> ascanrules = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "pscanrules")
    @JacksonXmlProperty(localName = "pscanrule")
    @JsonInclude(value = Include.NON_EMPTY)
    public List<String> pscanrules = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "files")
    @JacksonXmlProperty(localName = "file")
    @JsonInclude(value = Include.NON_EMPTY)
    public List<String> files = new ArrayList<>();

    @JacksonXmlProperty(localName = "not-before-version")
    @JsonInclude(value = Include.NON_EMPTY)
    public String notBeforeVersion;

    @JacksonXmlProperty(localName = "not-from-version")
    @JsonInclude(value = Include.NON_EMPTY)
    public String notFromVersion;
}
