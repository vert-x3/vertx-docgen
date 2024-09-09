/**
 * This document checks that post-processor blocks can contain nested blocks.
 *
 * [language,java]
 * ----
 * [source,java]
 * \----
 * System.out.println("Hello");
 * \----
 * ----
 *
 */
@Document() package io.vertx.test.postprocessors.nested;

import io.vertx.docgen.processor.impl.Document;
