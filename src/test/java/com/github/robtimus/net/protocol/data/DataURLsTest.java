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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.io.input.BrokenReader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.robtimus.net.protocol.data.DataURLs.Base64Appender;

@SuppressWarnings("nls")
class DataURLsTest {

    private static final Random RANDOM = new Random();

    private static final String DEFAULT_CONTENT_TYPE = MediaType.DEFAULT.toString();

    @Nested
    class CreateTest {

        @Test
        void testNoDataProtocol() {
            String spec = "http://www.google.com/";

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }

        @Test
        void testNoCommaPresent() {
            String path = "hello+world";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }

        @Test
        void testInvalidCharset() {
            String path = "text/plain;charset=something+invalid,hello+world";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }

        @Test
        void testNoMediaType() throws MalformedURLException {
            String path = ",hello+world";
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, "hello world");
        }

        @Test
        void testBase64NoMediaType() throws MalformedURLException {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String path = ";base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, bytes);
        }

        @Test
        void testInvalidBase64NoMediaType() {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String path = ";base64," + Base64.getEncoder().encodeToString(bytes) + "%";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }

        @Test
        void testMediaTypeNoParameters() throws MalformedURLException {
            String mediaType = "text/plain";
            String path = mediaType + ",hello+world";
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testBase64MediaTypeNoParameters() throws MalformedURLException {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String mediaType = "application/octet-stream";
            String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, mediaType, bytes);
        }

        @Test
        void testInvalidBase64MediaTypeNoParameters() {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String path = "application/octect-stream;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }

        @Test
        void testMediaTypeWithParameters() throws MalformedURLException {
            String mediaType = "text/plain;charset=UTF-8";
            String path = mediaType + ",hello+world";
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testBase64MediaTypeWithParameters() throws MalformedURLException {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String mediaType = "application/octet-stream;charset=UTF-8";
            String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            String spec = "data:" + path;

            URL url = DataURLs.create(spec);

            assertURL(url, path);
            assertURLConnection(url, mediaType, bytes);
        }

        @Test
        void testInvalidBase64MediaTypeWithParameters() {
            byte[] bytes = new byte[1024];
            RANDOM.nextBytes(bytes);
            String path = "application/octect-stream;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(bytes) + "%";
            String spec = "data:" + path;

            MalformedURLException exception = assertThrows(MalformedURLException.class, () -> DataURLs.create(spec));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        }
    }

    @Nested
    class BuilderFromStringTest {

        @Test
        void testBare() {
            String data = "hello+world";
            String path = "," + data;

            URL url = DataURLs.builder(data)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, "hello world");
        }

        @Test
        void testWithMediaType() {
            String mediaType = "application/json";
            String data = "hello+world";
            String path = mediaType + "," + data;

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testWithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }
    }

    @Nested
    class BuilderFromReaderTest {

        @Test
        void testBare() {
            String data = "hello+world";
            String path = "," + data;

            URL url = DataURLs.builder(new StringReader(data))
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, "hello world");
        }

        @Test
        void testWithMediaType() {
            String mediaType = "application/json";
            String data = "hello+world";
            String path = mediaType + "," + data;

            URL url = DataURLs.builder(new StringReader(data))
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testWithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(new StringReader(data))
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }

        void testIOException() {
            IOException cause = new IOException();

            @SuppressWarnings("resource")
            Reader data = new BrokenReader(cause);
            DataURLs.Builder builder = DataURLs.builder(data);

            UncheckedIOException exception = assertThrows(UncheckedIOException.class, builder::build);
            assertSame(cause, exception.getCause());
        }
    }

    @Nested
    class BuilderFromBytesTest {

        @Test
        void testBase64Bare() {
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String path = ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(data)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, data);
        }

        @Test
        void testBase64WithMediaType() {
            String mediaType = "application/json";
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, data);
        }

        @Test
        void testBase64WithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, data);
        }

        @Test
        void testNoBase64Bare() {
            String data = "hello+world";
            String path = "," + data;

            URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                    .withBase64Data(false)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, "hello world");
        }

        @Test
        void testNoBase64WithMediaType() {
            String mediaType = "application/json";
            String data = "hello+world";
            String path = mediaType + "," + data;

            URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                    .withBase64Data(false)
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testNoBase64WithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(data.getBytes(StandardCharsets.US_ASCII))
                    .withBase64Data(false)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }
    }

    @Nested
    class BuilderFromStreamTest {

        @Test
        void testBase64Bare() {
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String path = ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(new ByteArrayInputStream(data))
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, data);
        }

        @Test
        void testBase64WithMediaType() {
            String mediaType = "application/json";
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String path = mediaType + ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(new ByteArrayInputStream(data))
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, data);
        }

        @Test
        void testBase64WithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            byte[] data = new byte[1024];
            RANDOM.nextBytes(data);
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + ";base64," + Base64.getEncoder().encodeToString(data);

            URL url = DataURLs.builder(new ByteArrayInputStream(data))
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, data);
        }

        @Test
        void testNoBase64Bare() {
            String data = "hello+world";
            String path = "," + data;

            URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                    .withBase64Data(false)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, DEFAULT_CONTENT_TYPE, "hello world");
        }

        @Test
        void testNoBase64WithMediaType() {
            String mediaType = "application/json";
            String data = "hello+world";
            String path = mediaType + "," + data;

            URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                    .withBase64Data(false)
                    .withMediaType(mediaType)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, mediaType, "hello world");
        }

        @Test
        void testNoBase64WithMediaTypeAndParams() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = mediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII)))
                    .withBase64Data(false)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }

        @ParameterizedTest(name = "base64: {0}")
        @ValueSource(booleans = { true, false })
        void testIOException(boolean base64) {
            IOException cause = new IOException();

            @SuppressWarnings("resource")
            InputStream data = new BrokenInputStream(cause);
            DataURLs.Builder builder = DataURLs.builder(data)
                    .withBase64Data(base64);

            UncheckedIOException exception = assertThrows(UncheckedIOException.class, builder::build);
            assertSame(cause, exception.getCause());
        }
    }

    @Nested
    class BuilderWithMediaTypeTest {

        @Test
        void testWithNullMediaTypeParameter() {
            String bareMediaType = "application/json";
            String mediaType = bareMediaType + ";charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = bareMediaType + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .withCharset(StandardCharsets.US_ASCII)
                    .withMediaTypeParameter("charset", null)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }

        @Test
        void testWithCharset() {
            String mediaType = "application/json;charset=UTF-8";
            String paramName = "last-modified";
            String paramValue = "0";
            String data = "hello+world";
            String fullMediaType = mediaType.replace("UTF-8", "US-ASCII") + ";" + paramName + "=" + paramValue;
            String path = fullMediaType + "," + data;

            URL url = DataURLs.builder(data)
                    .withMediaType(mediaType)
                    .withMediaTypeParameter(paramName, paramValue)
                    .withCharset(StandardCharsets.US_ASCII)
                    .build();

            assertURL(url, path);
            assertURLConnection(url, fullMediaType, "hello world");
        }
    }

    private void assertURL(URL url, String expectedPath) {
        assertEquals(Handler.PROTOCOL, url.getProtocol());
        assertEquals(expectedPath, url.getPath());
        assertEquals(expectedPath, url.getFile());

        assertNull(url.getQuery());
        assertNull(url.getUserInfo());
        assertNull(url.getAuthority());
        assertEquals(-1, url.getPort());
        assertEquals(-1, url.getDefaultPort());
        assertNull(url.getHost());
        assertNull(url.getRef());
    }

    private void assertURLConnection(URL url, String expectedContentType, String expectedContent) {
        assertURLConnection(url, expectedContentType, expectedContent.getBytes(StandardCharsets.UTF_8));
    }

    private void assertURLConnection(URL url, String expectedContentType, byte[] expectedContent) {
        URLConnection connection = assertDoesNotThrow(() -> url.openConnection());
        assertEquals(expectedContentType, connection.getContentType());
        assertEquals(expectedContent.length, connection.getContentLength());

        byte[] content = assertDoesNotThrow(() -> IOUtils.toByteArray(connection));
        assertArrayEquals(expectedContent, content);
    }

    @Nested
    class Base64AppenderTest {

        @Test
        void testWriteByte() throws IOException {
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
        void testWriteArray() throws IOException {
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

        private void fill(byte[] buffer, StringBuilder expected) {
            for (int i = 0; i < buffer.length; i++) {
                int c = RANDOM.nextInt(26) + 'A';
                buffer[i] = (byte) c;
                expected.append((char) c);
            }
        }
    }
}
