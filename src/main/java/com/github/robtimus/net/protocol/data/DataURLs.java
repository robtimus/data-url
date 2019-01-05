/*
 * DataURLs.java
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A utility class for data URLs.
 *
 * @author Rob Spoor
 */
public final class DataURLs {

    private static final Handler SHARED_HANDLER = new Handler();

    private DataURLs() {
        throw new InternalError("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Creates a new data URL for the given full URL string.
     *
     * @param spec The string to create a new data URL for.
     * @return The created data URL.
     * @throws MalformedURLException If the given URL string is invalid.
     */
    public static URL create(String spec) throws MalformedURLException {
        return new URL(null, spec, SHARED_HANDLER);
    }

    /**
     * Creates a new data URL builder.
     *
     * @param data The data for created data URLs.
     * @return The created builder.
     */
    public static Builder.FromText builder(String data) {
        Objects.requireNonNull(data);
        return new Builder.FromText(sb -> sb.append(data));
    }

    /**
     * Creates a new data URL builder.
     * <p>
     * Note: the builder's {@link DataURLs.Builder#build()} method will fail with an {@link UncheckedIOException} if an I/O error occurs while
     * reading from the data stream.
     *
     * @param data The data for created data URLs.
     * @return The created builder.
     */
    public static Builder.FromText builder(Reader data) {
        Objects.requireNonNull(data);
        return new Builder.FromText(sb -> copyData(data, sb));
    }

    /**
     * Creates a new data URL builder.
     *
     * @param data The data for created data URLs.
     * @return The created builder.
     */
    public static Builder.FromBytes builder(byte[] data) {
        Objects.requireNonNull(data);
        return new Builder.FromBytes(() -> new ByteArrayInputStream(data));
    }

    /**
     * Creates a new data URL builder.
     * <p>
     * Note: the builder's {@link DataURLs.Builder#build()} method will fail with an {@link UncheckedIOException} if an I/O error occurs while
     * reading from the data stream.
     *
     * @param data The data for created data URLs.
     * @return The created builder.
     */
    public static Builder.FromBytes builder(InputStream data) {
        Objects.requireNonNull(data);
        return new Builder.FromBytes(() -> data);
    }

    private static void copyData(Reader data, StringBuilder dest) {
        char[] buffer = new char[4096];
        int len;
        try {
            while ((len = data.read(buffer)) != -1) {
                dest.append(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyData(InputStream data, OutputStream dest) {
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = data.read(buffer)) != -1) {
                dest.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A class that can be used to build data URLs.
     *
     * @author Rob Spoor
     */
    public abstract static class Builder {

        private Builder() {
        }

        /**
         * Specifies the media type of the data URL.
         *
         * @param mediaType The media type for the data URL.
         * @return This builder object.
         * @throws NullPointerException If the media type is {@code null}.
         * @throws IllegalArgumentException If the media type is invalid.
         */
        public WithMediaType withMediaType(String mediaType) {
            Objects.requireNonNull(mediaType);
            return new WithMediaType(this, MediaType.parse(mediaType));
        }

        /**
         * Creates a new data URL.
         *
         * @return The created data URL.
         */
        public URL build() {
            return build(null);
        }

        abstract URL build(MediaType mediaType);

        final URL createURL(String file) {
            try {
                return new URL(Handler.PROTOCOL, null, -1, file, SHARED_HANDLER);
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * A class that can be used to build data URLs from text.
         *
         * @author Rob Spoor
         */
        public static final class FromText extends Builder {

            private final Consumer<StringBuilder> dataAppender;

            private FromText(Consumer<StringBuilder> dataAppender) {
                this.dataAppender = dataAppender;
            }

            @Override
            URL build(MediaType mediaType) {
                StringBuilder file = new StringBuilder();
                if (mediaType != null) {
                    file.append(mediaType);
                }
                file.append(',');
                dataAppender.accept(file);

                return createURL(file.toString());
            }
        }

        /**
         * A class that can be used to build data URLs from bytes.
         *
         * @author Rob Spoor
         */
        public static final class FromBytes extends Builder {

            private final Supplier<InputStream> dataSupplier;
            private boolean base64Data;

            private FromBytes(Supplier<InputStream> dataSupplier) {
                this.dataSupplier = dataSupplier;
                base64Data = true;
            }

            /**
             * Specifies whether or not the data should be base64 encoded. The default is {@code true}.
             *
             * @param base64Data {@code true} to base64 encode the data, or {@code false} otherwise.
             * @return This builder object.
             */
            public Builder withBase64Data(boolean base64Data) {
                this.base64Data = base64Data;
                return this;
            }

            @Override
            URL build(MediaType mediaType) {
                StringBuilder file = new StringBuilder();
                if (mediaType != null) {
                    file.append(mediaType);
                }
                Charset charset = Handler.getCharset(mediaType);
                if (base64Data) {
                    file.append(Handler.BASE64_POSTFIX);
                    file.append(',');
                    try (OutputStream appender = new Base64Appender(file);
                            OutputStream dest = Base64.getEncoder().wrap(appender)) {

                        copyData(dataSupplier.get(), dest);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    file.append(',');
                    Reader data = new InputStreamReader(dataSupplier.get(), charset);
                    copyData(data, file);
                }

                return createURL(file.toString());
            }
        }

        /**
         * A class that can be used to build data URLs with media types.
         *
         * @author Rob Spoor
         */
        public static final class WithMediaType {

            private final Builder parent;

            private final String mimeType;
            private final Map<String, String> parameters;

            private WithMediaType(Builder parent, MediaType mediaType) {
                this.parent = parent;
                this.mimeType = mediaType.getMimeType();
                this.parameters = new LinkedHashMap<>(mediaType.getParameters());
            }

            /**
             * Sets the value for a media type parameter.
             *
             * @param name The parameter name.
             * @param value The parameter value. Use {@code null} to remove an existing parameter.
             * @return This builder object.
             */
            public WithMediaType withMediaTypeParameter(String name, String value) {
                Objects.requireNonNull(name);
                if (value == null) {
                    parameters.remove(name);
                } else {
                    parameters.put(name, value);
                }
                return this;
            }

            /**
             * Sets the charset of the media type.
             * This method is shorthand for {@link #withMediaTypeParameter(String, String) withMediaTypeParameter("charset", charset.name())}.
             *
             * @param charset The charset.
             * @return This builder object.
             * @throws NullPointerException If the charset is {@code null}.
             */
            public WithMediaType withCharset(Charset charset) {
                String value = charset.name();
                return withMediaTypeParameter("charset", value); //$NON-NLS-1$
            }

            /**
             * Creates a new data URL.
             *
             * @return The created data URL.
             */
            public URL build() {
                MediaType mediaType = MediaType.create(mimeType, parameters);
                return parent.build(mediaType);
            }
        }
    }

    static final class Base64Appender extends OutputStream {

        private final StringBuilder dest;
        // lazy initialize the buffer; right now Base64 only uses write(int), but that may change
        private char[] buffer;

        Base64Appender(StringBuilder dest) {
            this.dest = dest;
        }

        @Override
        public void write(int b) throws IOException {
            char c = convert(b);
            dest.append(c);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int offset = off;
            int remaining = len;
            while (remaining > 0) {
                int n = convertToBuffer(b, offset, remaining);
                dest.append(buffer, 0, n);
                offset += n;
                remaining -= n;
            }
        }

        private char convert(int b) {
            return (char) b;
        }

        private int convertToBuffer(byte[] b, int off, int len) {
            if (buffer == null) {
                buffer = new char[1024];
            }
            for (int i = off, j = 0; i < off + len && j < buffer.length; j++, i++) {
                buffer[j] = convert(b[i]);
            }
            return Math.min(buffer.length, len);
        }
    }
}
