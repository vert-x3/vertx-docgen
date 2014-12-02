package io.vertx.docgen;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessorTest {

  @Test
  public void testGeneration() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(), pkg);
      File dir = Files.createTempDirectory("docgen").toFile();
      dir.deleteOnExit();
      compiler.setOption("docgen.output", dir.getAbsolutePath());
      compiler.assertCompile();
      File file = new File(dir, pkg + ".ad");
      assertTrue(file.exists());
      assertTrue(file.isFile());
    }
  }

  @Test
  public void testFileName() throws Exception {
    Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(), "io.vertx.test.filename");
    File dir = Files.createTempDirectory("docgen").toFile();
    dir.deleteOnExit();
    compiler.setOption("docgen.output", dir.getAbsolutePath());
    compiler.assertCompile();
    File f1 = new File(dir, "index.ad");
    assertTrue(f1.exists());
    assertTrue(f1.isFile());
    File f2 = new File(dir, "sub" + File.separator + "index.ad");
    assertTrue(f2.exists());
    assertTrue(f2.isFile());
    assertEquals("sub/index.ad", new String(Files.readAllBytes(f1.toPath())));
  }

  @Test
  public void testExtension() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(), pkg);
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
}
