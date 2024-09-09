package io.vertx.tests;

import io.vertx.docgen.processor.impl.JavaDocGenerator;
import io.vertx.docgen.processor.DocGenProcessor;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessorTest {

  @Test
  public void testGeneration() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(new JavaDocGenerator()), pkg);
      File dir = Files.createTempDirectory("docgen").toFile();
      dir.deleteOnExit();
      compiler.setOption("docgen.output", dir.getAbsolutePath());
      compiler.assertCompile();
      File file = new File(dir, pkg + ".adoc");
      assertTrue(file.exists());
      assertTrue(file.isFile());
    }
  }

  @Test
  public void testLinkGenerationWithAnnotation() throws Exception {
    String pkg = "io.vertx.test.linktomethod";
    Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(new JavaDocGenerator()), pkg);
    File dir = Files.createTempDirectory("docgen").toFile();
    dir.deleteOnExit();
    compiler.setOption("docgen.output", dir.getAbsolutePath());
    compiler.assertCompile();
    File file = new File(dir, pkg + ".adoc");
    List<String> lines = Files.readAllLines(file.toPath());
    for (String line : lines) {
      // The :: is used when the annotated type is used.
      assertFalse(line.contains("::"));
    }
  }

  @Test
  public void testFileName() throws Exception {
    Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(new JavaDocGenerator()), "io.vertx.test.filename");
    File dir = Files.createTempDirectory("docgen").toFile();
    dir.deleteOnExit();
    compiler.setOption("docgen.output", dir.getAbsolutePath());
    compiler.assertCompile();
    File f1 = new File(dir, "index.adoc");
    assertTrue(f1.exists());
    assertTrue(f1.isFile());
    File f2 = new File(dir, "sub" + File.separator + "index.adoc");
    assertTrue(f2.exists());
    assertTrue(f2.isFile());
    assertEquals("sub/index.adoc", new String(Files.readAllBytes(f1.toPath())));
  }

  @Test
  public void testExtension() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(new JavaDocGenerator()), pkg);
      File dir = Files.createTempDirectory("docgen").toFile();
      dir.deleteOnExit();
      compiler.setOption("docgen.output", dir.getAbsolutePath());
      compiler.setOption("docgen.extension", ".ad.txt");
      compiler.assertCompile();
      File file = new File(dir, pkg + ".ad.txt");
      assertTrue(file.exists());
      assertTrue(file.isFile());
    }
  }

  @Test
  public void testOutputInterpolation() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(new JavaDocGenerator()), pkg);
      File dir = Files.createTempDirectory("docgen").toFile();
      dir.deleteOnExit();
      compiler.setOption("docgen.output", new File(dir, "$lang").getAbsolutePath());
      compiler.setOption("docgen.extension", ".ad.txt");
      compiler.assertCompile();
      File file = new File(new File(dir, "java"), pkg + ".ad.txt");
      assertTrue(file.exists());
      assertTrue(file.isFile());
    }
  }
}
