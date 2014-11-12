package test.proj.foofeature;

import io.vertx.docgen.Source;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import java.lang.UnsupportedOperationException;

@Source
public class Examples {

  private FooApi getFoo() {
    throw new UnsupportedOperationException();
  }

  public void fooExample() {
    FooApi foo = getFoo(); // <1>
    List<Boolean> list = new ArrayList<>();
    Set<Long> set = new HashSet<>();
    foo.myMethod("whatever", 0, list, set); // <2>
  }

}