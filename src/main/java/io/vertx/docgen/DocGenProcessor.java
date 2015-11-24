package io.vertx.docgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A doc gen processor that runs multiple generators at once.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessor extends BaseProcessor {

  private List<DocGenerator> generators = new ArrayList<>();
  private DocGenerator current;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    // Init scripted generators
    ScriptEngineManager manager = new ScriptEngineManager();
    try {
      List<URL> docgenUrls = Collections.list(DocGenProcessor.class.getClassLoader().getResources("docgen.json"));
      for (URL docgenUrl : docgenUrls) {
        try (InputStream docgenIn = docgenUrl.openStream()) {
          ScriptEngine engine = manager.getEngineByName("nashorn");
          engine.put("processingEnv", processingEnv);
          engine.put("typeUtils", processingEnv.getTypeUtils());
          engine.put("elementUtils", processingEnv.getElementUtils());
          ObjectMapper parser = new ObjectMapper();
          JsonNode obj = parser.readTree(docgenIn);
          String name = obj.get("name").asText();
          String scripts = obj.get("scripts").asText();
          try (InputStream scriptsIn = DocGenProcessor.class.getClassLoader().getResourceAsStream(scripts)) {
            engine.eval(new InputStreamReader(scriptsIn));
          }
          generators.add(new Generator(name, engine));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Service loader generators
    Iterator<DocGenerator> it = ServiceLoader.load(DocGenerator.class, DocGenProcessor.class.getClassLoader()).iterator();
    while (it.hasNext()) {
      try {
        generators.add(it.next());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected String getName() {
    return current.getName();
  }

  @Override
  protected String resolveRelativeFileName(PackageElement docElt) {
    String relativeFileName = super.resolveRelativeFileName(docElt);
    relativeFileName = current.resolveRelativeFileName(docElt, relativeFileName);
    return relativeFileName;
  }

  @Override
  protected void handleGen(PackageElement docElt) {
    for (DocGenerator generator : generators) {
      current = generator;
      current.init(processingEnv);
      StringWriter buffer = new StringWriter();
      process(buffer, docElt);
      write(docElt, buffer.toString());
    }
  }

  @Override
  protected String renderSource(ExecutableElement elt, String source) {
    return current.renderSource(elt, source);
  }

  @Override
  protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
    return current.resolveTypeLink(elt, coordinate);
  }

  @Override
  protected String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
    return current.resolveConstructorLink(elt, coordinate);
  }

  @Override
  protected String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
    return current.resolveMethodLink(elt, coordinate);
  }

  @Override
  protected String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
    return current.resolveFieldLink(elt, coordinate);
  }

  @Override
  protected String resolveLabel(Element elt) {
    String label = DocGenProcessor.super.resolveLabel(elt);
    return current.resolveLabel(elt, label);
  }

  class Generator implements DocGenerator {

    final String name;
    final ScriptEngine engine;

    public Generator(String name, ScriptEngine engine) {
      this.name = name;
      this.engine = engine;
    }

    private String eval(String functionName, Object... args) {
      Thread currentThread = Thread.currentThread();
      ClassLoader prev = currentThread.getContextClassLoader();
      try {
        ScriptObjectMirror function = (ScriptObjectMirror) engine.eval(functionName);
        currentThread.setContextClassLoader(DocGenProcessor.class.getClassLoader());
        return (String) function.call(DocGenProcessor.this, args);
      } catch (ScriptException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } finally {
        currentThread.setContextClassLoader(prev);
      }
    }

    @Override
    public void init(ProcessingEnvironment env) {
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String renderSource(ExecutableElement elt, String source) {
      return eval("renderSource", elt, source);
    }

    @Override
    public String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
      return eval("toTypeLink", elt, coordinate);
    }

    @Override
    public String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
      return eval("toConstructorLink", elt, coordinate);
    }

    @Override
    public String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
      return eval("toMethodLink", elt, coordinate);
    }

    @Override
    public String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
      return eval("toFieldLink", elt, coordinate);
    }

    @Override
    public String resolveLabel(Element elt, String defaultLabel) {
      String s = eval("resolveLabel", elt, defaultLabel);
      if (s != null) {
        defaultLabel = s;
      }
      return defaultLabel;
    }
  }
}
