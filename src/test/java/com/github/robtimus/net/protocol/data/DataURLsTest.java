/*
 * DataURLsTest.java
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import com.github.robtimus.net.protocol.data.DataURLs.Base64Appender;

@SuppressWarnings({ "nls", "javadoc" })
public class DataURLsTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testCreateNoDataProtocol() throws MalformedURLException {
        String spec = "http://www.google.com/";

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testCreateNoCommaPresent() throws MalformedURLException {
        String path = "hello+world";
        String spec = "data:" + path;

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testCreateInvalidCharset() throws MalformedURLException {
        String path = "text/plain;charset=something+invalid,hello+world";
        String spec = "data:" + path;

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testCreateNoMediaType() throws MalformedURLException {
        String path = ",hello+world";
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateBase64NoMediaType() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateInvalidBase64NoMediaType() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testCreateMediaTypeNoParameters() throws MalformedURLException {
        String path = "text/plain,hello+world";
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateBase64MediaTypeNoParameters() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateInvalidBase64MediaTypeNoParameters() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testCreateMediaTypeWithParameters() throws MalformedURLException {
        String path = "text/plain;charset=UTF-8,hello+world";
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateBase64MediaTypeWithParameters() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = DataURLs.create(spec);

        assertURL(url, path);
    }

    @Test
    public void testCreateInvalidBase64MediaTypeWithParameters() throws MalformedURLException {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        expectedException.expect(MalformedURLException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        DataURLs.create(spec);
    }

    @Test
    public void testBuilderFromStringBare() {
        String data = "hello+world";
        String path = "," + data;

        URL url = DataURLs.builder(data)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStringWithMediaType() {
        String mediaType = "application/json";
        String data = "hello+world";
        String path = mediaType + "," + data;

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStringWithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = mediaType + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromReaderBare() {
        String data = "hello+world";
        String path = "," + data;

        URL url = DataURLs.builder(new StringReader(data))
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromReaderWithMediaType() {
        String mediaType = "application/json";
        String data = "hello+world";
        String path = mediaType + "," + data;

        URL url = DataURLs.builder(new StringReader(data))
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromReaderWithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = mediaType + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(new StringReader(data))
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesBase64Bare() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(data)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesBase64WithMediaType() {
        String mediaType = "application/json";
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesBase64WithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = mediaType + ";" + paramName + "=" + paramValue + ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesNoBase64Bare() {
        String data = "hello+world";
        String path = "," + data;

        URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                .withBase64Data(false)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesNoBase64WithMediaType() {
        String mediaType = "application/json";
        String data = "hello+world";
        String path = mediaType + "," + data;

        URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                .withBase64Data(false)
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromBytesNoBase64WithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = mediaType + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                .withBase64Data(false)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamBase64Bare() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(new ByteArrayInputStream(data))
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamBase64WithMediaType() {
        String mediaType = "application/json";
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(new ByteArrayInputStream(data))
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamBase64WithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String path = mediaType + ";" + paramName + "=" + paramValue + ";base64," + Base64.getEncoder().encodeToString(data);

        URL url = DataURLs.builder(new ByteArrayInputStream(data))
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamNoBase64Bare() {
        String data = "hello+world";
        String path = "," + data;

        URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                .withBase64Data(false)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamNoBase64WithMediaType() {
        String mediaType = "application/json";
        String data = "hello+world";
        String path = mediaType + "," + data;

        URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                .withBase64Data(false)
                .withMediaType(mediaType)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderFromStreamNoBase64WithMediaTypeAndParams() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = mediaType + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                .withBase64Data(false)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderWithMediaTypeWithNullMediaTypeParameter() {
        String bareMediaType = "application/json";
        String mediaType = bareMediaType + ";charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = bareMediaType + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .withCharset(StandardCharsets.US_ASCII)
                .withMediaTypeParameter("charset", null)
                .build();

        assertURL(url, path);
    }

    @Test
    public void testBuilderWithMediaTypeWithCharset() {
        String mediaType = "application/json;charset=UTF-8";
        String paramName = "last-modified";
        String paramValue = "0";
        String data = "hello+world";
        String path = mediaType.replace("UTF-8", "US-ASCII") + ";" + paramName + "=" + paramValue + "," + data;

        URL url = DataURLs.builder(data)
                .withMediaType(mediaType)
                .withMediaTypeParameter(paramName, paramValue)
                .withCharset(StandardCharsets.US_ASCII)
                .build();

        assertURL(url, path);
    }

    private void assertURL(URL url, String path) {
        assertEquals(Handler.PROTOCOL, url.getProtocol());
        assertEquals(path, url.getPath());
        assertEquals(path, url.getFile());

        assertNull(url.getQuery());
        assertNull(url.getUserInfo());
        assertNull(url.getAuthority());
        assertEquals(-1, url.getPort());
        assertEquals(-1, url.getDefaultPort());
        assertNull(url.getHost());
        assertNull(url.getRef());
    }

    @Test
    public void testBase64AppenderWriteByte() throws IOException {
        StringBuilder sb = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        try (OutputStream appender = new Base64Appender(sb)) {
            for (byte b = 'A'; b <= 'Z'; b++) {
                appender.write(b);
                expected.append((char) b);
            }
        }
        assertEquals(expected.toString(), sb.toString());
    }

    @Test
    public void testBase64AppenderWriteArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        try (OutputStream appender = new Base64Appender(sb)) {
            // equal buffer sizes
            byte[] buffer = new byte[1024];
            fill(buffer, expected);
            appender.write(buffer);

            // smaller buffer size
            buffer = new byte[512];
            fill(buffer, expected);
            appender.write(buffer);

            // larger buffer size
            buffer = new byte[5632];
            fill(buffer, expected);
            appender.write(buffer);
        }
        assertEquals(expected.toString(), sb.toString());
    }

    private static final Random RANDOM = new Random();

    private void fill(byte[] buffer, StringBuilder expected) {
        for (int i = 0; i < buffer.length; i++) {
            int c = RANDOM.nextInt(26) + 'A';
            buffer[i] = (byte) c;
            expected.append((char) c);
        }
    }
}
