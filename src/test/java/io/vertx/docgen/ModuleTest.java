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
    assertEquals("before_includesub_contentsubafter_include", assertDoc("io.vertx.test.includepkg"));
  }

  @Test
  public void testFailInclude() throws Exception {
    assertTrue(failDoc("io.vertx.test.failinclude").containsKey("io.vertx.test.failinclude"));
  }

  @Test
  public void testLinkToClass() throws Exception {
    assertEquals("abc[`TheClass`]", assertDoc("io.vertx.test.linktoclass"));
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
