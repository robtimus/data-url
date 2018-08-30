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

class MediaType {

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
        validateMimeType(mimeType);

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
        int index = type.indexOf(';');
        if (index == -1) {
            validateMimeType(type);
            return new MediaType(type, type, Collections.<String, String>emptyMap());
        }

        String mimeType = type.substring(0, index).trim();
        String paramString = type.substring(index + 1).trim();

        validateMimeType(mimeType);
        Map<String, String> parameters = parseParameters(paramString);
        return new MediaType(type, mimeType, parameters);
    }

    private static final String TOKEN = "[\u0021-\u007e&&[^()<>@,;:\\\\\"/\\[\\]?=]]"; //$NON-NLS-1$
    private static final Pattern MIME_TYPE_PATTERN = Pattern.compile(TOKEN + "+/" + TOKEN + "+"); //$NON-NLS-1$ //$NON-NLS-2$

    static void validateMimeType(String mimeType) {
        if (!MIME_TYPE_PATTERN.matcher(mimeType).matches()) {
            throw new IllegalArgumentException(Messages.mediaType.invalidMimeType.get(mimeType));
        }
    }

    private static Map<String, String> parseParameters(String paramString) {
        if (paramString.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> parameters = new LinkedHashMap<>();

        int start = 0;
        while (start < paramString.length())
        {
           start = parseNextParameter(paramString, start, parameters);
        }

        return parameters;
    }

    private static int parseNextParameter(String paramString, int start, Map<String, String> parameters) {
        boolean quote = false;
        boolean backslash = false;

        int end = getNameEnd(paramString, start);
        String name = paramString.substring(start, end).trim();
        if (end < paramString.length() && paramString.charAt(end) == '=') {
            end++;
        }

        StringBuilder value = new StringBuilder(paramString.length() - end);
        for (int i = end; i < paramString.length(); i++) {
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
                    parameters.put(name, value.toString().trim());
                    return i + 1;
                }
                value.append(c);
                break;
            default:
                value.append(c);
                break;
            }
        }
        parameters.put(name, value.toString().trim());
        return paramString.length();
    }

    private static int getNameEnd(String params, int start) {
        int indexOfEquals = params.indexOf('=', start);
        int indexOfSemicolon = params.indexOf(';', start);
        if (indexOfEquals == -1 && indexOfSemicolon == -1) {
            return params.length();
        }
        if (indexOfEquals == -1) {
            return indexOfSemicolon;
        }
        if (indexOfSemicolon == -1) {
            return indexOfEquals;
        }
        return Math.min(indexOfEquals, indexOfSemicolon);
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