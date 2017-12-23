/*
 * DataURLConnection.java
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A connection to a {@code data:} URL as specified in <a href="https://www.ietf.org/rfc/rfc2397.txt">RFC 2397</a>.
 *
 * @author Rob Spoor
 */
public class DataURLConnection extends URLConnection {

    private final MediaType mediaType;
    private final byte[] data;

    private InputStream inputStream;

    DataURLConnection(URL url, MediaType mediaType, byte[] data) {
        super(url);
        this.mediaType = mediaType;
        this.data = data;
    }

    /** {@inheritDoc} */
    @Override
    public void connect() {
        connected = true;
    }

    /**
     * Returns the length of the decoded data part of the data URL.
     *
     * @return The length of the decoded data part of the data URL.
     */
    @Override
    public int getContentLength() {
        return data.length;
    }

    /**
     * Returns the length of the decoded data part of the data URL.
     *
     * @return The length of the decoded data part of the data URL.
     */
    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    /**
     * Returns the media type of the data URL. If none is specified it will be {@code text/plain;charset=US-ASCII}, as specified in
     * <a href="https://www.ietf.org/rfc/rfc2397.txt">RFC 2397</a>.
     *
     * @return The media type of the data URL.
     */
    @Override
    public String getContentType() {
        return mediaType.toString();
    }

    /**
     * Returns the content encoding of the data URL. This is taken from the data URL's media type.
     * <p>
     * If no media type is specified, it is assumed to be {@code text/plain;charset=US-ASCII}, as specified in
     * <a href="https://www.ietf.org/rfc/rfc2397.txt">RFC 2397</a>. As a result, the content encoding will then be {@code US-ASCII}.
     *
     * @return The content encoding of the data URL.
     */
    @Override
    public String getContentEncoding() {
        return mediaType.getCharset();
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException(Messages.dataURLConnection.getInputStream.falseDoInput.get());
        }
        if (inputStream == null) {
            inputStream = new ByteArrayInputStream(data);
        }
        return inputStream;
    }
}
