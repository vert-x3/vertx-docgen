package io.vertx.docgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a scope for source code inclusion: any link ref in this scope should include the content instead
 * of linking to it.
 *
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Target({ElementType.TYPE,ElementType.PACKAGE,ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Source {

  /**
   * Enables or disables the translation of the annotated example.
   */
  boolean translate() default true;
}
