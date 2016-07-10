package io.vertx.docgen;

import java.util.Collections;

/**
 * Processor specialized for Java.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JavaDocGenProcessor extends BaseProcessor {

  @Override
  protected Iterable<DocGenerator> generators() {
    return Collections.singleton(generator());
  }

  protected DocGenerator generator() {
    return new JavaDocGenerator();
  }
}
