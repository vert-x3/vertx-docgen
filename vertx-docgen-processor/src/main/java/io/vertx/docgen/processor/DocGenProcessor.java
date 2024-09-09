package io.vertx.docgen.processor;

import io.vertx.docgen.processor.impl.BaseProcessor;
import io.vertx.docgen.processor.impl.DocGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A doc gen processor that runs multiple generators at once.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessor extends BaseProcessor {

  private List<DocGenerator> generators;

  public DocGenProcessor() {
  }

  public DocGenProcessor(DocGenerator... generators) {
    this.generators = Arrays.asList(generators.clone());
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    // Service loader generators
    if (generators == null) {
      generators = new ArrayList<>();
      Iterator<DocGenerator> it = ServiceLoader.load(DocGenerator.class, DocGenProcessor.class.getClassLoader()).iterator();
      while (it.hasNext()) {
        try {
          generators.add(it.next());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  protected Iterable<DocGenerator> generators() {
    return generators;
  }
}
