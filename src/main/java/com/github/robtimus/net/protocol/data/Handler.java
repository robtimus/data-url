/*
 * Handler.java
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

import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A stream protocol handler for the {@code data:} protocol as specified in <a href="https://www.ietf.org/rfc/rfc2397.txt">RFC 2397</a>.
 *
 * @author Rob Spoor
 */
public class Handler extends URLStreamHandler {

    /** The protocol name. */
    public static final String PROTOCOL = "data"; //$NON-NLS-1$

    static final String BASE64_POSTFIX = ";base64"; //$NON-NLS-1$
    private static final int BASE64_POSTFIX_LENGTH = BASE64_POSTFIX.length();

    /** {@inheritDoc} */
    @Override
    protected DataURLConnection openConnection(URL u) {
        validateProtocol(u);

        String spec = u.toExternalForm();
        int start = PROTOCOL.length() + 1;
        int limit = spec.length();

        int indexOfComma = validateCommaPresent(spec, start, limit);

        MediaType mediaType = getMediaType(spec, start, indexOfComma);
        boolean base64Data = isBase64Data(spec, indexOfComma);

        // ignore the data
        byte[] data = getData(spec, indexOfComma, limit, mediaType, base64Data);

        return new DataURLConnection(u, mediaType, data);
    }

    /** {@inheritDoc} */
    @Override
    protected DataURLConnection openConnection(URL u, Proxy p) {
        return openConnection(u);
    }

    /** {@inheritDoc} */
    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
        validateProtocol(u);

        int indexOfComma = validateCommaPresent(spec, start, limit);

        MediaType mediaType = getMediaType(spec, start, indexOfComma);
        boolean base64Data = isBase64Data(spec, indexOfComma);

        // ignore the data
        getData(spec, indexOfComma, limit, mediaType, base64Data);

        String path = spec.substring(start, limit);
        setURL(u, u.getProtocol(), null, -1, null, null, path, null, null);
    }

    private void validateProtocol(URL u) {
        if (!PROTOCOL.equals(u.getProtocol())) {
            throw new IllegalArgumentException(Messages.handler.invalidProtocol.get(PROTOCOL, u.getProtocol()));
        }
    }

    private int validateCommaPresent(String spec, int start, int limit) {
        int indexOfComma = spec.indexOf(',', start);
        if (indexOfComma == -1 || indexOfComma > limit) {
            throw new IllegalArgumentException(Messages.handler.missingComma.get(spec));
        }
        return indexOfComma;
    }

    private MediaType getMediaType(String spec, int start, int indexOfComma) {
        int end = indexOfComma;
        if (isBase64Data(spec, indexOfComma)) {
            end -= BASE64_POSTFIX_LENGTH;
        }
        return start == end ? MediaType.DEFAULT : MediaType.parse(spec, start, end);
    }

    private boolean isBase64Data(String spec, int indexOfComma) {
        return spec.regionMatches(indexOfComma - BASE64_POSTFIX_LENGTH, BASE64_POSTFIX, 0, BASE64_POSTFIX_LENGTH);
    }

    private byte[] getData(String spec, int indexOfComma, int limit, MediaType mediaType, boolean base64Data) {
        String dataPart = spec.substring(indexOfComma + 1, limit);

        if (base64Data) {
            String s = dataPart.replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
            return Base64.getDecoder().decode(s);
        }

        try {
            Charset charset = getCharset(mediaType);
            String s = URLDecoder.decode(dataPart, charset.name());
            return s.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    static Charset getCharset(MediaType mediaType) {
        String encoding = mediaType == null ? null : mediaType.getCharset();
        return encoding == null ? StandardCharsets.US_ASCII : Charset.forName(encoding);
    }
}
