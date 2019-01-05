/*
 * MediaType.java
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class MediaType {

    private static final String DEFAULT_MIME_TYPE = "text/plain"; //$NON-NLS-1$
    private static final String DEFAULT_CHARSET = "US-ASCII"; //$NON-NLS-1$
    private static final String DEFAULT_MEDIA_TYPE = DEFAULT_MIME_TYPE + ";charset=" + DEFAULT_CHARSET; //$NON-NLS-1$
    private static final Map<String, String> DEFAULT_PARAMETERS = Collections.singletonMap("charset", DEFAULT_CHARSET); //$NON-NLS-1$

    static final MediaType DEFAULT = new MediaType(DEFAULT_MEDIA_TYPE, DEFAULT_MIME_TYPE, DEFAULT_PARAMETERS);

    private final String mediaType;
    private final String mimeType;
    private final Map<String, String> parameters;
    private final Map<String, String> parameterLookup;

    private MediaType(String mediaType, String mimeType, Map<String, String> parameters) {
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.parameters = Collections.unmodifiableMap(parameters);
        parameterLookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        parameterLookup.putAll(parameters);
    }

    static MediaType create(String mimeType, Map<String, String> parameters) {
        validateMimeType(mimeType, 0, mimeType.length());

        String mediaType = buildMediaType(mimeType, parameters);
        return new MediaType(mediaType, mimeType, new LinkedHashMap<>(parameters));
    }

    private static String buildMediaType(String mimeType, Map<String, String> parameters) {

        StringBuilder mediaType = new StringBuilder();
        mediaType.append(mimeType);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            mediaType.append(';').append(entry.getKey()).append('=');

            String value = entry.getValue();
            if (value != null) {
                boolean containsSemicolon = value.indexOf(';') != -1;
                if (containsSemicolon) {
                    mediaType.append('"');
                }
                appendValue(value, mediaType);
                if (containsSemicolon) {
                    mediaType.append('"');
                }
            }
        }
        return mediaType.toString();
    }

    private static void appendValue(String value, StringBuilder mediaType) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                mediaType.append('\\');
            }
            mediaType.append(c);
        }
    }

    static MediaType parse(String type) {
        return parse(type, 0, type.length());
    }

    static MediaType parse(String type, int start, int end) {
        int index = type.indexOf(';', start);
        if (isNotFound(index, end)) {
            validateMimeType(type, start, end);
            String mimeType = type.substring(start, end);
            return new MediaType(mimeType, mimeType, Collections.emptyMap());
        }

        int mimeTypeStart = start;
        int mimeTypeEnd = index;

        validateMimeType(type, mimeTypeStart, mimeTypeEnd);

        int paramStart = skipStartingWhitespace(type, index + 1, end);
        int paramEnd = end;

        Map<String, String> parameters = parseParameters(type, paramStart, paramEnd);
        String mimeType = type.substring(mimeTypeStart, mimeTypeEnd);

        return new MediaType(type, mimeType, parameters);
    }

    private static final String TOKEN = "[\u0021-\u007e&&[^()<>@,;:\\\\\"/\\[\\]?=]]"; //$NON-NLS-1$
    private static final Pattern MIME_TYPE_PATTERN = Pattern.compile(TOKEN + "+/" + TOKEN + "+"); //$NON-NLS-1$ //$NON-NLS-2$

    private static void validateMimeType(String mimeType, int start, int end) {
        // Use a CharSequence wrapper to cap the limits of the mime type, to prevent the matcher from going out of bounds.
        // This wrapper is used instead of mimeType.substring because that copies part of the mime type's contents.
        CharSequence s = start == 0 && end == mimeType.length() ? mimeType : new SubString(mimeType, start, end);
        if (!MIME_TYPE_PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException(Messages.mediaType.invalidMimeType.get(mimeType));
        }
    }

    private static final class SubString implements CharSequence {

        private final String s;
        private final int offset;
        private final int limit;

        private SubString(String s, int offset, int limit) {
            this.s = s;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public int length() {
            return limit - offset;
        }

        @Override
        public char charAt(int index) {
            return s.charAt(index + offset);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return s.substring(start + offset, end - offset);
        }
    }

    private static Map<String, String> parseParameters(String paramString, int start, int end) {
        if (start == end) {
            return Collections.emptyMap();
        }

        Map<String, String> parameters = new LinkedHashMap<>();

        int index = start;
        while (index < end) {
            index = parseNextParameter(paramString, index, end, parameters);
            index = skipStartingWhitespace(paramString, index, end);
        }

        return parameters;
    }

    private static int parseNextParameter(String paramString, int start, int end, Map<String, String> parameters) {
        boolean quote = false;
        boolean backslash = false;

        int nameEnd = getNameEnd(paramString, start, end);
        String name = paramString.substring(start, nameEnd);
        int valueStart = nameEnd;
        if (valueStart < end && paramString.charAt(valueStart) == '=') {
            valueStart++;
        }

        StringBuilder value = new StringBuilder(end - valueStart);
        for (int i = valueStart; i < end; i++) {
            char c = paramString.charAt(i);

            switch (c) {
            case '"':
                if (backslash) {
                    backslash = false;
                    value.append(c);
                } else {
                    quote = !quote;
                }
                break;
            case '\\':
                if (backslash) {
                    backslash = false;
                    value.append(c);
                } else {
                    backslash = true;
                }
                break;
            case ';':
                if (!quote) {
                    parameters.put(name, value.toString());
                    return i + 1;
                }
                value.append(c);
                break;
            default:
                value.append(c);
                break;
            }
        }
        parameters.put(name, value.toString());
        return end;
    }

    private static int getNameEnd(String params, int start, int end) {
        int indexOfEquals = params.indexOf('=', start);
        int indexOfSemicolon = params.indexOf(';', start);
        if (isNotFound(indexOfEquals, end) && isNotFound(indexOfSemicolon, end)) {
            return end;
        }
        if (isNotFound(indexOfEquals, end)) {
            return indexOfSemicolon;
        }
        if (isNotFound(indexOfSemicolon, end)) {
            return indexOfEquals;
        }
        return Math.min(indexOfEquals, indexOfSemicolon);
    }

    private static boolean isNotFound(int index, int end) {
        return index == -1 || index >= end;
    }

    private static int skipStartingWhitespace(String s, int index, int end) {
        int i = index;
        while (i < end && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    String getMimeType() {
        return mimeType;
    }

    Map<String, String> getParameters() {
        return parameters;
    }

    String getCharset() {
        return parameterLookup.get("charset"); //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return mediaType;
    }
}
