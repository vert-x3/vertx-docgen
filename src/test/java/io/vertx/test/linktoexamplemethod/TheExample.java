package io.vertx.test.linktoexamplemethod;

import io.vertx.docgen.Example;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Example
public class TheExample {

  public void someMethod() {
    Map<String, String> map = new HashMap<>();
    // Some comment
//
    map.put("abc", "def");
    map.get("abc"); // Beyond last statement
  }

}
