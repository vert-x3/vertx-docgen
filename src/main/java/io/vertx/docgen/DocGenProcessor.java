package io.vertx.docgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.annotation.processing.ProcessingEnvironment;
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
import java.util.List;

/**
 * A scriptable doc gen processor.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenProcessor extends BaseProcessor {

  private List<Generator> generators = new ArrayList<>();
  private Generator current;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
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
  }

  @Override
  protected String getName() {
    return current.name;
  }

  @Override
  protected void handleGen(PackageElement docElt) {
    for (Generator generator : generators) {
      current = generator;
      StringWriter buffer = new StringWriter();
      process(buffer, docElt);
      write(docElt, buffer.toString());
    }
  }

  @Override
  protected String renderSource(ExecutableElement elt, String source) {
    return current.eval("renderSource", elt, source);
  }

  @Override
  protected String toTypeLink(TypeElement elt) {
    return current.eval("toTypeLink", elt);
  }

  @Override
  protected String toConstructorLink(ExecutableElement elt) {
    return current.eval("toConstructorLink", elt);
  }

  @Override
  protected String toMethodLink(ExecutableElement elt) {
    return current.eval("toMethodLink", elt);
  }

  @Override
  protected String toFieldLink(VariableElement elt) {
    return current.eval("toFieldLink", elt);
  }

  static class Generator {

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
        return (String) function.call(this, args);
      } catch (ScriptException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } finally {
        currentThread.setContextClassLoader(prev);
      }
    }
  }
}
