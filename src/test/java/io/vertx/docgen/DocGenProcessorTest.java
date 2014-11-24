package io.vertx.docgen;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessorTest {

  @Test
  public void testGeneration() throws Exception {
    for (String pkg : Arrays.asList("io.vertx.test.linktoclass",
        "io.vertx.test.linktoconstructor", "io.vertx.test.linktomethod", "io.vertx.test.linktofield")) {
      Compiler<DocGenProcessor> compiler = BaseProcessorTest.buildCompiler(new DocGenProcessor(), pkg);
      compiler.assertCompile();
    }
  }
}
