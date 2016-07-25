package io.vertx.test.includeannotatedannotation;

import io.vertx.docgen.Source;

@Source
public @interface TheExample {

  String value() default "";

}
