/**
 * This documentation is intended to use <em>entities</em>:
 * <p/>
 * <p>Foo &amp; Bar.</p>
 * <p>10 \u0024</p>
 * <p>10 &#8364;</p>
 * <p>Stra\u00DFe</p>
 * <p>Straßen</p>
 * <p>&#92;u00DF</p>
 * <p/>
 * In code:
 *
 * [source,java]
 * ----
 * JsonObject json = new JsonObject();
 * json.put("key", "&#92;u0000&#92;u0001&#92;u0080&#92;u009f&#92;u00a0&#92;u00ff");
 * json.put("key", "&#92;u00c3&#92;u00bc");
 * ----
 */
@Document package io.vertx.test.entities;

import io.vertx.docgen.processor.impl.Document;
