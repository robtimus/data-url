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
import org.junit.jupiter.api.Nested;
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

    @Nested
    class DataURL {

        @Test
        void testWithDataURLContext() throws MalformedURLException {
            String packages = System.getProperty("java.protocol.handler.pkgs");
            packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
            System.setProperty("java.protocol.handler.pkgs", packages);

            URL context = new URL("data:,hello+world");
            URL url = new URL(context, "data:,foo+bar");

            assertURL(context, ",hello+world");
            assertURL(url, ",foo+bar");
        }

        @Test
        void testWithHttpURLContext() throws MalformedURLException {
            String packages = System.getProperty("java.protocol.handler.pkgs");
            packages = (packages == null ? "" : packages + "|") + "com.github.robtimus.net.protocol";
            System.setProperty("java.protocol.handler.pkgs", packages);

            URL context = new URL("http://www.google.com/");
            URL url = new URL(context, "data:,foo+bar");

            assertURL(url, ",foo+bar");
        }
    }

    @Nested
    class HttpURL {

        @Test
        void testWithDataURLContext() throws MalformedURLException {
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
    }

    @Nested
    class OpenConnection {

        @Test
        void testNoDataProtocol() throws MalformedURLException {
            Handler handler = spy(new Handler());

            URL url = new URL("http://www.google.com/");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
            assertEquals(Messages.handler.invalidProtocol(Handler.PROTOCOL, url.getProtocol()), exception.getMessage());
        }

        @Test
        void testNoCommaPresent() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "hello+world";
            URL url = createDataURL(path);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
            assertEquals(Messages.handler.missingComma("data:" + path), exception.getMessage());
        }

        @Test
        void testInvalidCharset() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "text/plain;charset=something+invalid,hello+world";
            URL url = createDataURL(path);

            UnsupportedCharsetException exception = assertThrows(UnsupportedCharsetException.class, () -> handler.openConnection(url));
            assertEquals("something+invalid", exception.getMessage());
        }

        @Test
        void testNoMediaType() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = ",hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            DataURLConnection connection = handler.openConnection(url);
            assertSame(url, connection.getURL());
        }

        @Test
        void testBase64NoMediaType() throws MalformedURLException {
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
        void testInvalidBase64NoMediaType() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = ";base64," + Base64.getEncoder().encodeToString(bytes) + "%";

            URL url = createDataURL(path);

            assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
        }

        @Test
        void testMediaTypeNoParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "text/plain,hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            DataURLConnection connection = handler.openConnection(url);
            assertSame(url, connection.getURL());
        }

        @Test
        void testBase64MediaTypeNoParameters() throws MalformedURLException {
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
        void testInvalidBase64MediaTypeNoParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = "application/octect-stream;base64," + Base64.getEncoder().encodeToString(bytes) + "%";

            URL url = createDataURL(path);

            assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
        }

        @Test
        void testMediaTypeWithParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "text/plain;charset=UTF-8,hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            DataURLConnection connection = handler.openConnection(url);
            assertSame(url, connection.getURL());
        }

        @Test
        void testBase64MediaTypeWithParameters() throws MalformedURLException {
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
        void testInvalidBase64MediaTypeWithParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";

            URL url = createDataURL(path);

            assertThrows(IllegalArgumentException.class, () -> handler.openConnection(url));
        }

        @Test
        void testWithProxy() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = ",hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            DataURLConnection connection = handler.openConnection(url, Proxy.NO_PROXY);
            assertSame(url, connection.getURL());

            verify(handler).openConnection(url, Proxy.NO_PROXY);
            verify(handler).openConnection(url);
        }
    }

    @Nested
    class ParseURL {

        @Test
        void testNoDataProtocol() {
            Handler handler = spy(new Handler());

            String spec = "http://www.google.com/";

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
        }

        @Test
        void testCommaPresentInAnchor() {
            Handler handler = spy(new Handler());

            String path = "hello+world";
            String spec = "data:" + path;
            String specWithAnchor = spec + "#anchor,";

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, specWithAnchor, handler));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            verify(handler).parseURL(any(URL.class), eq(specWithAnchor), eq(5), eq(spec.length()));
        }

        @Test
        void testNoCommaPresent() {
            Handler handler = spy(new Handler());

            String path = "hello+world";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
        }

        @Test
        void testInvalidCharset() {
            Handler handler = spy(new Handler());

            String path = "text/plain;charset=something+invalid,hello+world";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
        }

        @Test
        void testNoMediaTypeWithAnchor() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = ",hello+world";
            String spec = "data:" + path;
            String specWithAnchor = spec + "#anchor";

            URL url = new URL(null, specWithAnchor, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, specWithAnchor, 5, spec.length());
        }

        @Test
        void testNoMediaType() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = ",hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testBase64NoMediaType() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = ";base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testInvalidBase64NoMediaType() {
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
        void testMediaTypeNoParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "text/plain,hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testBase64MediaTypeNoParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = "application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testInvalidBase64MediaTypeNoParameters() {
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
        void testMediaTypeWithParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            String path = "text/plain;charset=UTF-8,hello+world";
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testBase64MediaTypeWithParameters() throws MalformedURLException {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = "application/octet-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = new URL(null, spec, handler);

            assertURL(url, path);
            verify(handler).parseURL(url, spec, 5, spec.length());
        }

        @Test
        void testInvalidBase64MediaTypeWithParameters() {
            Handler handler = spy(new Handler());

            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> new URL(null, spec, handler));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            verify(handler).parseURL(any(URL.class), eq(spec), eq(5), eq(spec.length()));
        }
    }

    @Nested
    class GetCharset {

        @Test
        void testNoMediaType() {
            assertEquals(StandardCharsets.US_ASCII, Handler.getCharset(null));
        }

        @Test
        void testMediaTypeNoCharset() {
            MediaType mediaType = MediaType.parse("application/json");

            assertEquals(StandardCharsets.US_ASCII, Handler.getCharset(mediaType));
        }

        @Test
        void testMediaTypeWithCharset() {
            MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");

            assertEquals(StandardCharsets.UTF_8, Handler.getCharset(mediaType));
        }
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
