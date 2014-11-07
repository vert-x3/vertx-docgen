package io.vertx.docgen;

import org.junit.Test;

import java.io.File;
import java.lang.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ModuleTest {

  @Test
  public void testIncludePkg() throws Exception {
    assertEquals("before_includesub_contentafter_include", assertDoc("io.vertx.test.includepkg"));
  }

  @Test
  public void testIncludeNonExisting() throws Exception {
    assertTrue(failDoc("io.vertx.test.includenonexisting").containsKey("io.vertx.test.includenonexisting"));
  }

  @Test
  public void testIncludeCircular() throws Exception {
    assertTrue(failDoc("io.vertx.test.includecircular").containsKey("io.vertx.test.includecircular"));
  }

  @Test
  public void testLinkToClass() throws Exception {
    assertEquals("link:abc[`TheClass`]", assertDoc("io.vertx.test.linktoclass"));
  }

  @Test
  public void testLinkToField() throws Exception {
    assertEquals("link:ghi[`f1`]", assertDoc("io.vertx.test.linktofield"));
  }

  @Test
  public void testLinkToMethodMember() throws Exception {
    assertEquals("link:def[`m`]", assertDoc("io.vertx.test.linktomethodmember"));
  }

  @Test
  public void testLinkToMethod() throws Exception {
    assertEquals(
        "link:def[`m1`]\n" +
        "link:def[`m1`]\n" +
        "link:def[`m2`]\n" +
        "link:def[`m2`]\n" +
        "link:def[`m2`]\n" +
        "link:def[`m3`]\n" +
        "link:def[`m3`]\n" +
        "link:def[`m3`]\n" +
        "link:def[`m4`]\n" +
        "link:def[`m5`]\n" +
        "link:def[`m6`]\n" +
        "link:def[`m7`]\n" +
        "link:def[`m8`]\n" +
        "link:def[`m9`]\n" +
        "link:def[`m10`]\n" +
        "link:def[`m11`]\n" +
        "link:def[`m12`]\n" +
        "link:def[`m13`]\n" +
        "link:def[`m14`]\n" +
        "link:def[`m15`]\n" +
        "link:def[`m16`]\n" +
        "link:def[`m17`]" +
        "", assertDoc("io.vertx.test.linktomethod"));
  }

  @Test
  public void testLinkToMethodWithSimpleTypeName() throws Exception {
    assertEquals(
        "link:def[`m1`]\n" +
        "link:def[`m2`]\n" +
        "link:def[`m3`]" +
        "", assertDoc("io.vertx.test.linktomethodwithsimpletypename"));
  }

  @Test
  public void testLinkToMethodWithUnresolvableType() throws Exception {
    assertTrue(failDoc("io.vertx.test.linktomethodwithunresolvabletype").containsKey("io.vertx.test.linktomethodwithunresolvabletype"));
  }

  @Test
  public void testLinkToSameNameFieldAndMethod() throws Exception {
    assertEquals("link:ghi[`member`]\nlink:def[`member`]", assertDoc("io.vertx.test.linktosamenamefieldandmethod"));
  }

  @Test
  public void testLinkWithLabel() throws Exception {
    assertEquals("link:def[`the label value`]", assertDoc("io.vertx.test.linkwithlabel"));
  }

  private Map<String, String> failDoc(String pkg) throws Exception {
    Compiler compiler = buildCompiler(pkg);
    compiler.failCompile();
    return compiler.processor.failures;
  }

  private String assertDoc(String pkg) throws Exception {
    Compiler compiler = buildCompiler(pkg);
    compiler.assertCompile();
    return compiler.processor.getDoc(pkg);
  }

  private Compiler buildCompiler(String pkg) throws Exception {
    int index = 0;
    File output;
    do {
      output = new File("target/" + pkg + (index == 0 ? "" : "" + index));
      index++;
    }
    while (output.exists());
    ArrayList<File> sources = new ArrayList<>();
    for (URL url : Collections.list(ModuleTest.class.getClassLoader().getResources(pkg.replace('.', '/')))) {
      File root = new File(url.toURI());
      Files.
          find(root.toPath(), 100, (path, attrs) -> path.toString().endsWith(".java")).
          map(Path::toFile).
          forEach(sources::add);
    }
    assertTrue(sources.size() > 0);
    return new Compiler(sources, output);
  }
}
