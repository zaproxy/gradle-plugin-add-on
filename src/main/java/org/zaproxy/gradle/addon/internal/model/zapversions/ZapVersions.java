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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

@JsonRootName(value = "ZAP")
@JsonIgnoreProperties({"addon", "addOns"})
@JsonSerialize(using = ZapVersionsSerializer.class)
public class ZapVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    public Core core;

    public SortedSet<AddOn> addOns = new TreeSet<>();

    @JsonAnySetter
    public void addAddOn(String name, AddOn addOn) {
        addOn.id = name.substring(6);
        addOns.add(addOn);
    }
}
