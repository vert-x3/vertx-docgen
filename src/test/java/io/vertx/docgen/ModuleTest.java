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
    assertEquals("abc[`TheClass`]", assertDoc("io.vertx.test.linktoclass"));
  }

  @Test
  public void testLinkToMethodMember() throws Exception {
    assertEquals("def[`m`]", assertDoc("io.vertx.test.linktomethodmember"));
  }

  @Test
  public void testLinkToMethod() throws Exception {
    assertEquals(
        "def[`m1`]\n" +
        "def[`m1`]\n" +
        "def[`m2`]\n" +
        "def[`m2`]\n" +
        "def[`m2`]\n" +
        "def[`m3`]\n" +
        "def[`m3`]\n" +
        "def[`m3`]\n" +
        "def[`m4`]\n" +
        "def[`m5`]\n" +
        "def[`m6`]\n" +
        "def[`m7`]\n" +
        "def[`m8`]\n" +
        "def[`m9`]\n" +
        "def[`m10`]\n" +
        "def[`m11`]\n" +
        "def[`m12`]\n" +
        "def[`m13`]\n" +
        "def[`m14`]\n" +
        "def[`m15`]\n" +
        "def[`m16`]\n" +
        "def[`m17`]" +
        "", assertDoc("io.vertx.test.linktomethod"));
  }

  @Test
  public void testLinkToMethodWithSimpleTypeName() throws Exception {
    assertEquals(
        "def[`m1`]\n" +
        "def[`m2`]\n" +
        "def[`m3`]" +
        "", assertDoc("io.vertx.test.linktomethodwithsimpletypename"));
  }

  @Test
  public void testLinkToMethodWithUnresolvableType() throws Exception {
    assertTrue(failDoc("io.vertx.test.linktomethodwithunresolvabletype").containsKey("io.vertx.test.linktomethodwithunresolvabletype"));
  }

  @Test
  public void testLinkWithLabel() throws Exception {
    assertEquals("def[`the label value`]", assertDoc("io.vertx.test.linkwithlabel"));
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
