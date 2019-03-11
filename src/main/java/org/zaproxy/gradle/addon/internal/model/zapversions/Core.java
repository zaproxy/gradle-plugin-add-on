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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class Core implements Serializable {

    private static final long serialVersionUID = 1L;

    public String version;

    @JsonProperty(value = "daily-version")
    public String dailyVersion;

    @JsonProperty(value = "daily")
    public Package daily;

    @JsonProperty(value = "windows")
    public Package windows;

    @JsonProperty(value = "linux")
    public Package linux;

    @JsonProperty(value = "mac")
    public Package mac;

    @JsonProperty(value = "relnotes")
    public String relnotes;

    @JsonProperty(value = "relnotes-url")
    public String relnotesUrl;
}
