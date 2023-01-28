# data-url
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/data-url)](https://search.maven.org/artifact/com.github.robtimus/data-url)
[![Build Status](https://github.com/robtimus/data-url/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/data-url/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Adata-url&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Adata-url)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Adata-url&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Adata-url)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/data-url/badge.svg)](https://snyk.io/test/github/robtimus/data-url)

The `data-url` library adds support for the `data` protocol as specified in [RFC 2397](https://www.ietf.org/rfc/rfc2397.txt).

There are several ways to create data URLs. Most are described by the [URL](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#URL-java.lang.String-java.lang.String-int-java.lang.String-) class.

1. Make use of a shared [URLStreamHandlerFactory](https://docs.oracle.com/javase/8/docs/api/java/net/URLStreamHandlerFactory.html) set on the [URL](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#setURLStreamHandlerFactory-java.net.URLStreamHandlerFactory-) class. This must return an instance of [Handler](https://robtimus.github.io/data-url/apidocs/com/github/robtimus/net/protocol/data/Handler.html) for the `data` protocol.
2. Add package `com.github.robtimus.net.protocol` to system property `java.protocol.handler.pkgs`.
3. Use [this](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#URL-java.net.URL-java.lang.String-java.net.URLStreamHandler-) URL constructor, and provide an instance of [Handler](https://robtimus.github.io/data-url/apidocs/com/github/robtimus/net/protocol/data/Handler.html) as the [URLStreamHandler](https://docs.oracle.com/javase/8/docs/api/java/net/URLStreamHandler.html). The `context` argument can remain `null`.
    * It's ill-advised to use [this](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#URL-java.lang.String-java.lang.String-int-java.lang.String-java.net.URLStreamHandler-) constructor, because it does not ensure the data URL is correctly formatted.
4. Use utility class [DataURLs](https://robtimus.github.io/data-url/apidocs/com/github/robtimus/net/protocol/data/DataURLs.html).

Note that class [Handler](https://robtimus.github.io/data-url/apidocs/com/github/robtimus/net/protocol/data/Handler.html) is stateless, and therefore instances can be shared among multiple threads.
