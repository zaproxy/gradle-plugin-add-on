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
package org.zaproxy.gradle.addon.internal.model.zapversions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import org.zaproxy.gradle.addon.internal.model.Dependencies;

public class AddOn implements Comparable<AddOn>, Serializable {

    private static final long serialVersionUID = 1L;

    @JsonIgnore public String id;

    public String name;
    public String description;
    public String author;
    public String version;

    @JsonInclude(value = Include.NON_EMPTY)
    public String semver;

    public String file;
    public String status;

    @JsonInclude(value = Include.NON_EMPTY)
    public String changes;

    public String url;
    public String hash;

    @JsonInclude(value = Include.NON_EMPTY)
    public String info;

    public String date;
    public String size;

    @JsonProperty(value = "not-before-version")
    @JsonInclude(value = Include.NON_EMPTY)
    public String notBeforeVersion;

    @JsonProperty(value = "not-from-version")
    @JsonInclude(value = Include.NON_EMPTY)
    public String notFromVersion;

    @JsonProperty(value = "dependencies")
    @JsonInclude(value = Include.NON_NULL)
    public Dependencies dependencies;

    @Override
    public int compareTo(AddOn other) {
        return id.compareTo(other.id);
    }
}
