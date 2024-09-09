package io.vertx.test.includeannotatedclass;

import io.vertx.docgen.Source;

@Source
public class TheExample {

  // Some comment
  private String f;

  public void someMethod() {
    System.out.println(f);
  }
}
