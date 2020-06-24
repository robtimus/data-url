/*
 * HandlerTest.java
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class HandlerTest {

    private Properties originalProperties;

    @BeforeEach
    void storeOriginalProperties() {
        originalProperties = System.getProperties();
        Properties newProperties = new Properties();
        newProperties.putAll(originalProperties);
        System.setProperties(newProperties);
    }

    @AfterEach
    void restoreOriginalProperties() {
        System.setProperties(originalProperties);
    }

    @Test
    void testRegistration() throws MalformedURLException {
        String packages = System.getProperty("java.protocol.handler.pkgs");
        packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
        System.setProperty("java.protocol.handler.pkgs", packages);

        String path = "text/plain,hello+world";
        String spec = "data:" + path;

        URL url = new URL(spec);

        assertURL(url, path);
    }

    @Test
    void testDataURLWithDataURLContext() throws MalformedURLException {
        String packages = System.getProperty("java.protocol.handler.pkgs");
        packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
        System.setProperty("java.protocol.handler.pkgs", packages);

        URL context = new URL("data:,hello+world");
        URL url = new URL(context, "data:,foo+bar");

        assertURL(context, ",hello+world");
        assertURL(url, ",foo+bar");
    }

    @Test
    void testDataURLWithHttpURLContext() throws MalformedURLException {
        String packages = System.getProperty("java.protocol.handler.pkgs");
        packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
        System.setProperty("java.protocol.handler.pkgs", packages);

        URL context = new URL("http://www.google.com/");
        URL url = new URL(context, "data:,foo+bar");

        assertURL(url, ",foo+bar");
    }

    @Test
    void testHttpURLWithDataURLContext() throws MalformedURLException {
        String packages = System.getProperty("java.protocol.handler.pkgs");
        packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
        System.setProperty("java.protocol.handler.pkgs", packages);

        URL context = new URL("data:,hello+world");
        URL url = new URL(context, "http://www.google.com/");

        assertURL(context, ",hello+world");
        assertEquals("http", url.getProtocol());
        assertEquals("www.google.com", url.getHost());
        assertEquals(-1, url.getPort());
        assertEquals("/", url.getPath());
    }

    @Test
    void testOpenConnectionNoDataProtocol() throws MalformedURLException {
        Handler handler = spy(new Handler());

        URL url = new URL("http://www.google.com/");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
        assertEquals(Messages.handler.invalidProtocol.get(Handler.PROTOCOL, url.getProtocol()), exception.getMessage());
    }

    @Test
    void testOpenConnectionNoCommaPresent() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "hello+world";
        URL url = createDataURL(path);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
        assertEquals(Messages.handler.missingComma.get("data:" + path), exception.getMessage());
    }

    @Test
    void testOpenConnectionInvalidCharset() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain;charset=something+invalid,hello+world";
        URL url = createDataURL(path);

        UnsupportedCharsetException exception = assertThrows(UnsupportedCharsetException.class, () -> handler.openConnection(url));
        assertEquals("something+invalid", exception.getMessage());
    }

    @Test
    void testOpenConnectionNoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = ",hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionBase64NoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionInvalidBase64NoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes) + "%";

        URL url = createDataURL(path);

        assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
    }

    @Test
    void testOpenConnectionMediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain,hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionBase64MediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionInvalidBase64MediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;base64," + Base64.getEncoder().encodeToString(bytes) + "%";

        URL url = createDataURL(path);

        assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
    }

    @Test
    void testOpenConnectionMediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain;charset=UTF-8,hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionBase64MediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url);
        assertSame(url, connection.getURL());
    }

    @Test
    void testOpenConnectionInvalidBase64MediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";

        URL url = createDataURL(path);

        assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
    }

    @Test
    void testOpenConnectionWithProxy() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = ",hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        DataURLConnection connection = handler.openConnection(url, Proxy.NO_PROXY);
        assertSame(url, connection.getURL());

        verify(handler).openConnection(url, Proxy.NO_PROXY);
        verify(handler).openConnection(url);
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLNoDataProtocol() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String spec = "http://www.google.com/";

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLCommaPresentInAnchor() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "hello+world";
        String spec = "data:" + path;
        String specWithAnchor = spec + "#anchor,";

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, specWithAnchor, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(specWithAnchor), eq(5), eq(spec.length()));
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLNoCommaPresent() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "hello+world";
        String spec = "data:" + path;

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLInvalidCharset() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain;charset=something+invalid,hello+world";
        String spec = "data:" + path;

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @Test
    void testParseURLNoMediaTypeWithAnchor() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = ",hello+world";
        String spec = "data:" + path;
        String specWithAnchor = spec + "#anchor";

        URL url = new URL(null, specWithAnchor, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, specWithAnchor, 5, spec.length());
    }

    @Test
    void testParseURLNoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = ",hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @Test
    void testParseURLBase64NoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLInvalidBase64NoMediaType() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = ";base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @Test
    void testParseURLMediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain,hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @Test
    void testParseURLBase64MediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLInvalidBase64MediaTypeNoParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @Test
    void testParseURLMediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        String path = "text/plain;charset=UTF-8,hello+world";
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @Test
    void testParseURLBase64MediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octet-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes);
        String spec = "data:" + path;

        URL url = new URL(null, spec, handler);

        assertURL(url, path);
        verify(handler).parseURL(url, spec, 5, spec.length());
    }

    @SuppressWarnings("unused")
    @Test
    void testParseURLInvalidBase64MediaTypeWithParameters() throws MalformedURLException {
        Handler handler = spy(new Handler());

        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
        String spec = "data:" + path;

        MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
    }

    @Test
    void testGetCharsetNoMediaType() {
        assertEquals(StandardCharsets.US_ASCII, Handler.getCharset(null));
    }

    @Test
    void testGetCharsetMediaTypeNoCharset() {
        MediaType mediaType = MediaType.parse("application/json");

        assertEquals(StandardCharsets.US_ASCII, Handler.getCharset(mediaType));
    }

    @Test
    void testGetCharsetMediaTypeWithCharset() {
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");

        assertEquals(StandardCharsets.UTF_8, Handler.getCharset(mediaType));
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

    private URL createDataURL(String path) throws MalformedURLException {
        return new URL(Handler.PROTOCOL, null, -1, path, new Handler());
    }
}
