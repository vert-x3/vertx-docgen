import javax.annotation.processing.Processor;

module io.vertx.docgen.processor {
  requires jdk.compiler;
  requires io.vertx.docgen;
  exports io.vertx.docgen.processor;
  // provides Processor with JavaDocGenProcessor;
}
