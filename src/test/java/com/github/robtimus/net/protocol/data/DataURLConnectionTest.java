/*
 * DataURLConnectionTest.java
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class DataURLConnectionTest {

    private static URL testURL;

    @BeforeAll
    static void initTestURL() throws MalformedURLException {
        testURL = new URL(null, "data:,", new Handler());
    }

    @Test
    void testConnect() {
        DataURLConnection connection = new DataURLConnection(testURL, MediaType.DEFAULT, new byte[0]);

        connection.connect();

        // can't call setDoInput on connected URLConnections
        assertThrows(IllegalStateException.class, () -> connection.setDoInput(true));
    }

    @Test
    void testGetContentLength() {
        int length = 1024;

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.DEFAULT, new byte[length]);

        assertEquals(length, connection.getContentLength());
        assertEquals(length, connection.getContentLengthLong());
    }

    @Test
    void testGetContentType() {
        String contentType = "application/json; charset=UTF-8";

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.parse(contentType), new byte[0]);

        assertEquals(contentType, connection.getContentType());
    }

    @Test
    void testGetContentTypeDefaultValue() {
        DataURLConnection connection = new Handler().openConnection(testURL);

        assertEquals(MediaType.DEFAULT.toString(), connection.getContentType());
    }

    @Test
    void testGetContentEncoding() {
        String contentType = "application/json; charset=UTF-8";

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.parse(contentType), new byte[0]);

        assertEquals("UTF-8", connection.getContentEncoding());
    }

    @Test
    void testGetContentEncodingNotSet() {
        String contentType = "application/json";

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.parse(contentType), new byte[0]);

        assertNull(connection.getContentEncoding());
    }

    @Test
    void testGetContentEncodingDefaultValue() {
        DataURLConnection connection = new Handler().openConnection(testURL);

        assertEquals("US-ASCII", connection.getContentEncoding());
    }

    @Test
    void testGetInputStream() throws IOException {
        int length = 1024;

        byte[] data = new byte[length];
        new Random().nextBytes(data);

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.DEFAULT, data);

        assertArrayEquals(data, readData(connection));
    }

    @Test
    void testGetInputStreamNoDoInput() {
        int length = 1024;

        byte[] data = new byte[length];
        new Random().nextBytes(data);

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.DEFAULT, data);
        connection.setDoInput(false);

        assertThrows(ProtocolException.class, connection::getInputStream);
    }

    @Test
    void testGetInputStreamTwice() throws IOException {
        int length = 1024;

        byte[] data = new byte[length];
        new Random().nextBytes(data);

        DataURLConnection connection = new DataURLConnection(testURL, MediaType.DEFAULT, data);

        try (InputStream first = connection.getInputStream();
                InputStream second = connection.getInputStream()) {

            assertSame(first, second);
        }
    }

    private byte[] readData(DataURLConnection connection) throws IOException {
        try (InputStream input = connection.getInputStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            output.flush();
            return output.toByteArray();
        }
    }
}
