package io.vertx.test.linktomethod;

import io.vertx.docgen.FooAnnotation;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TheClass {

  public void m1() {}
  public void m2(String arg) {}
  public void m3(List<String> arg) {}
  public void m4(boolean arg) {}
  public void m5(byte arg) {}
  public void m6(short arg) {}
  public void m7(int arg) {}
  public void m8(long arg) {}
  public void m9(float arg) {}
  public void m10(double arg) {}
  public void m11(char arg) {}
  public <T> void m12(T arg) {}
  public <T extends List<String>> void m13(T arg) {}
  public <T extends U, U extends List<String>> void m14(T arg) {}
  public void m15(String[] arg) {}
  public void m16(String[][] arg) {}
  public <T> void m17(T[] arg) {}
  public void m18(@FooAnnotation Object arg) {}

}
