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
    assertEquals("link:type[`TheClass`]", assertDoc("io.vertx.test.linktoclass"));
  }

  @Test
  public void testLinkToField() throws Exception {
    assertEquals("link:field[`f1`]", assertDoc("io.vertx.test.linktofield"));
  }

  @Test
  public void testLinkToMethodMember() throws Exception {
    assertEquals("link:method[`m`]", assertDoc("io.vertx.test.linktomethodmember"));
  }

  @Test
  public void testLinkToMethod() throws Exception {
    assertEquals(
        "link:method[`m1`]\n" +
        "link:method[`m1`]\n" +
        "link:method[`m2`]\n" +
        "link:method[`m2`]\n" +
        "link:method[`m2`]\n" +
        "link:method[`m3`]\n" +
        "link:method[`m3`]\n" +
        "link:method[`m3`]\n" +
        "link:method[`m4`]\n" +
        "link:method[`m5`]\n" +
        "link:method[`m6`]\n" +
        "link:method[`m7`]\n" +
        "link:method[`m8`]\n" +
        "link:method[`m9`]\n" +
        "link:method[`m10`]\n" +
        "link:method[`m11`]\n" +
        "link:method[`m12`]\n" +
        "link:method[`m13`]\n" +
        "link:method[`m14`]\n" +
        "link:method[`m15`]\n" +
        "link:method[`m16`]\n" +
        "link:method[`m17`]" +
        "", assertDoc("io.vertx.test.linktomethod"));
  }

  @Test
  public void testLinkToMethodWithSimpleTypeName() throws Exception {
    assertEquals(
        "link:method[`m1`]\n" +
        "link:method[`m2`]\n" +
        "link:method[`m3`]" +
        "", assertDoc("io.vertx.test.linktomethodwithsimpletypename"));
  }

  @Test
  public void testLinkToMethodWithUnresolvableType() throws Exception {
    assertTrue(failDoc("io.vertx.test.linktomethodwithunresolvabletype").containsKey("io.vertx.test.linktomethodwithunresolvabletype"));
  }

  @Test
  public void testLinkToConstructor() throws Exception {
    assertEquals("link:constructor[`<init>`]\nlink:constructor[`<init>`]", assertDoc("io.vertx.test.linktoconstructor"));
  }

  @Test
  public void testLinkToSameNameFieldAndMethod() throws Exception {
    assertEquals("link:field[`member`]\nlink:method[`member`]", assertDoc("io.vertx.test.linktosamenamefieldandmethod"));
  }

  @Test
  public void testLinkToSameNameConstructorAndMethod() throws Exception {
    assertEquals("link:constructor[`<init>`]\nlink:constructor[`<init>`]\nlink:constructor[`<init>`]", assertDoc("io.vertx.test.linktosamenameconstructorandmethod"));
  }

  @Test
  public void testLinkWithLabel() throws Exception {
    assertEquals("link:method[`the label value`]", assertDoc("io.vertx.test.linkwithlabel"));
  }

  @Test
  public void testMargin() throws Exception {
    assertEquals("A\nB\nC", assertDoc("io.vertx.test.margin"));
  }

  @Test
  public void testCommentStructure() throws Exception {
    assertEquals("the_first_sentence\n\nthe_body", assertDoc("io.vertx.test.commentstructure"));
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
