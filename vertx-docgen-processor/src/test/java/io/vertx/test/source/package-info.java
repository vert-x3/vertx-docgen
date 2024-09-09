/**
 * # 1
 * [source, $lang]
 * ----
 * {@link io.vertx.test.source.Example#hello()}
 * ----
 * # 2
 * [source, $lang]
 * ----
 * {@link io.vertx.test.source.ExampleNotTranslated#hello()}
 * ----
 * # 3
 * [source, $lang]
 * ----
 * {@link io.vertx.test.source.translated.Example#hello()}
 * ----
 * # 4
 * [source, $lang]
 * ----
 * {@link io.vertx.test.source.translated.sub.Example#hello()}
 * ----
 * # 5
 * [source, java]
 * ----
 * {@link io.vertx.test.source.notTranslated.Example#hello()}
 * ----
 * # 6
 * [source, java]
 * ----
 * {@link io.vertx.test.source.notTranslated.sub.Example#hello()}
 * ----
 */
@Document
package io.vertx.test.source;

import io.vertx.docgen.processor.impl.Document;
