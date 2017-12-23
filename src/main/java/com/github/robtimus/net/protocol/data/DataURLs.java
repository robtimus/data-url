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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
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
        return new Builder.FromText(consumer(data));
    }

    private static Consumer<StringBuilder> consumer(Reader data) {
        return sb -> {
            char[] buffer = new char[4096];
            int len;
            try {
                while ((len = data.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    /**
     * Creates a new data URL builder.
     *
     * @param data The data for created data URLs.
     * @return The created builder.
     */
    public static Builder.FromBytes builder(byte[] data) {
        Objects.requireNonNull(data);

        Supplier<byte[]> dataSupplier = () -> data;
        BiConsumer<StringBuilder, Charset> dataAppender = (sb, charset) -> sb.append(new String(data, charset));

        return new Builder.FromBytes(dataSupplier, dataAppender);
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

        class ByteArrayOutputStreamWithCharset extends ByteArrayOutputStream {
            public String toString(Charset charset) {
                return new String(buf, 0, count, charset);
            }
        }

        Supplier<ByteArrayOutputStreamWithCharset> streamSupplier = () -> {
            byte[] buffer = new byte[4096];
            int len;
            ByteArrayOutputStreamWithCharset output = new ByteArrayOutputStreamWithCharset();
            try {
                while ((len = data.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return output;
        };

        Supplier<byte[]> dataSupplier = () -> streamSupplier.get().toByteArray();
        BiConsumer<StringBuilder, Charset> dataAppender = (sb, charset) -> sb.append(streamSupplier.get().toString(charset));

        return new Builder.FromBytes(dataSupplier, dataAppender);
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

            private final Supplier<byte[]> dataSupplier;
            private final BiConsumer<StringBuilder, Charset> dataAppender;
            private boolean base64Data;

            private FromBytes(Supplier<byte[]> dataSupplier, BiConsumer<StringBuilder, Charset> dataAppender) {
                this.dataSupplier = dataSupplier;
                this.dataAppender = dataAppender;
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
                    file.append(new String(Base64.getEncoder().encode(dataSupplier.get()), charset));
                } else {
                    file.append(',');
                    dataAppender.accept(file, charset);
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
}
