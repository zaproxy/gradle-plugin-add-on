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
package org.zaproxy.gradle.addon.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter.Indenter;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.stax2.XMLStreamWriter2;

public class DefaultIndenter implements Indenter, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final int NUMBER_OF_SPACES = 4;

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public void writeIndentation(XMLStreamWriter2 sw, int level) throws XMLStreamException {
        sw.writeRaw("\n");
        sw.writeRaw(StringUtils.repeat(' ', NUMBER_OF_SPACES * level));
    }

    @Override
    public void writeIndentation(JsonGenerator jg, int level) throws IOException {
        jg.writeRaw("\n");
        jg.writeRaw(StringUtils.repeat(' ', NUMBER_OF_SPACES * level));
    }
}
