/**
 * This document checks that code are not stripped by post-processors.
 *
 * [source,java]
 * ----
 * System.out.println("Hello");
 * ----
 *
 * [source]
 * ----
 *   System.out.println("Bye");
 * ----
 */
@Document() package io.vertx.test.postprocessors.code;

import io.vertx.docgen.impl.Document;
