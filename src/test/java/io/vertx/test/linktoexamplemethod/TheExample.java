package io.vertx.test.linktoexamplemethod;

import io.vertx.docgen.Source;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Source
public class TheExample {

  public void someMethod() {
    Map<String, String> map = new HashMap<>();
    // Some comment
//
    map.put("abc", "def");
    map.get("abc"); // Beyond last statement
  }

}
