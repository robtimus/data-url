/*
 * MediaTypeTest.java
 * Copyright 2017 Rob Spoor
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

package com.github.robtimus.net.protocol.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SuppressWarnings({ "nls", "javadoc" })
public class MediaTypeTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testConstructNoParameters() {
        String type = "application/json";

        MediaType mediaType = MediaType.create(type, Collections.emptyMap());

        assertEquals(type, mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testConstructWithOneParameters() {
        String type = "application/json";

        Map<String, String> parameters = Collections.singletonMap("charset", "UTF-8");

        MediaType mediaType = MediaType.create(type, parameters);

        assertEquals("application/json;charset=UTF-8", mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testConstructWithMultipleParameters() {
        String type = "application/json";

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("CHARSET", "UTF-8;");
        parameters.put("last-modified", "\"\\0");

        MediaType mediaType = MediaType.create(type, parameters);

        assertEquals("application/json;CHARSET=\"UTF-8;\";last-modified=\\\"\\\\0", mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8;", mediaType.getCharset());
    }

    @Test
    public void testConstructWithNullParameterValue() {
        String type = "application/json";

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("CHARSET", "UTF-8;");
        parameters.put("last-modified", null);

        MediaType mediaType = MediaType.create(type, parameters);

        assertEquals("application/json;CHARSET=\"UTF-8;\";last-modified=", mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8;", mediaType.getCharset());
    }

    @Test
    public void testConstructInvalidMimeTypeNoSubType() {
        String type = "application";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(Messages.mediaType.invalidMimeType.get(type));

        MediaType.create(type, Collections.emptyMap());
    }

    @Test
    public void testConstructInvalidMimeTypeInvalidTokenChar() {
        String type = "application/json@";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(Messages.mediaType.invalidMimeType.get(type));

        MediaType.create(type, Collections.emptyMap());
    }

    @Test
    public void testParseNoParameters() {
        String type = "application/json";

        MediaType mediaType = MediaType.parse(type);

        assertEquals(type, mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testParseStartOnly() {
        String type = "application/json";

        MediaType mediaType = MediaType.parse(type + "!", 0, type.length());

        assertEquals(type, mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testParseEndOnly() {
        String type = "application/json";

        MediaType mediaType = MediaType.parse("!" + type, 1, type.length() + 1);

        assertEquals(type, mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testParseMiddleOnly() {
        String type = "application/json";

        MediaType mediaType = MediaType.parse("!" + type + "!", 1, type.length() + 1);

        assertEquals(type, mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testParseEmptyParameters() {
        String type = "application/json";

        MediaType mediaType = MediaType.parse(type + ";");

        assertEquals(type + ";", mediaType.toString());
        assertEquals(type, mediaType.getMimeType());
        assertEquals(Collections.emptyMap(), mediaType.getParameters());
        assertNull(mediaType.getCharset());
    }

    @Test
    public void testParseWithOneParameter() {
        String type = "application/json; CHARSET=UTF-8";

        MediaType mediaType = MediaType.parse(type);

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(Collections.singletonMap("CHARSET", "UTF-8"), mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithMultipleParameter() {
        String type = "application/json; CHARSET=UTF-8; last-modified=0";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("CHARSET", "UTF-8");
        parameters.put("last-modified", "0");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithEscapedAndQuotedParameter() {
        String type = "application/json; CHARSET=\"UTF-8;\"; last-modified=\\\"\\\\0";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("CHARSET", "UTF-8;");
        parameters.put("last-modified", "\"\\0");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8;", mediaType.getCharset());
    }

    @Test
    public void testParseWithEmptyValueParameterInMiddle() {
        String type = "application/json; dummy=; CHARSET=UTF-8";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "");
        parameters.put("CHARSET", "UTF-8");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithEmptyValueParameterAtEnd() {
        String type = "application/json; CHARSET=UTF-8; dummy=";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "");
        parameters.put("CHARSET", "UTF-8");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithNoValueParameterInMiddle() {
        String type = "application/json; dummy; CHARSET=UTF-8";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "");
        parameters.put("CHARSET", "UTF-8");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithNoValueParameterAtEnd() {
        String type = "application/json; CHARSET=UTF-8; dummy";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "");
        parameters.put("CHARSET", "UTF-8");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("UTF-8", mediaType.getCharset());
    }

    @Test
    public void testParseWithOnlyNoParametera() {
        String type = "application/json; CHARSET; dummy";

        MediaType mediaType = MediaType.parse(type);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "");
        parameters.put("CHARSET", "");

        assertEquals(type, mediaType.toString());
        assertEquals("application/json", mediaType.getMimeType());
        assertEquals(parameters, mediaType.getParameters());
        assertEquals("", mediaType.getCharset());
    }

    @Test
    public void testParseInvalidMimeTypeNoSubType() {
        String type = "application";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(Messages.mediaType.invalidMimeType.get(type));

        MediaType.parse(type);
    }

    @Test
    public void testParseInvalidMimeTypeInvalidTokenChar() {
        String type = "application/json@";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(Messages.mediaType.invalidMimeType.get(type));

        MediaType.parse(type);
    }
}
