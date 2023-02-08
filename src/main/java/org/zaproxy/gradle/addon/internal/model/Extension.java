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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Extension {

    @JacksonXmlProperty(localName = "v", isAttribute = true)
    @JsonInclude(value = Include.NON_EMPTY)
    public String version;

    @JsonProperty public String classname;

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public Classnames classnames;

    @JsonProperty
    @JsonInclude(value = Include.NON_NULL)
    public DependenciesExt dependencies;

    public Extension() {}

    public Extension(String classname) {
        this.classname = classname;
    }
}
