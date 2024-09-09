package io.vertx.docgen.processor.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a document to process.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
public @interface Document {

  /**
   * @return the file name of this document, when no name is specified, the name will be generated
   *         using the annotated package element.
   */
  String fileName() default "";

}
