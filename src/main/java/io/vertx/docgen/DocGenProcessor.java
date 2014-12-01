package io.vertx.docgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
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
@SupportedAnnotationTypes({"io.vertx.docgen.Document"})
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class DocGenProcessor extends BaseProcessor {

  private List<ScriptEngine> engines = new ArrayList<>();
  private ScriptEngine current;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    ScriptEngineManager manager = new ScriptEngineManager();
    try {
      List<URL> abc = Collections.list(DocGenProcessor.class.getClassLoader().getResources("docgen.json"));
      for (URL url : abc) {
        try (InputStream in = url.openStream()) {
          ScriptEngine engine = manager.getEngineByName("nashorn");
          engine.put("processingEnv", processingEnv);
          engine.put("typeUtils", processingEnv.getTypeUtils());
          engine.put("elementUtils", processingEnv.getElementUtils());
          ObjectMapper parser = new ObjectMapper();
          JsonNode obj = parser.readTree(in);
          String scripts = obj.get("scripts").asText();
          try (InputStream in2 = DocGenProcessor.class.getClassLoader().getResourceAsStream(scripts)) {
            engine.eval(new InputStreamReader(in2));
          }
          engines.add(engine);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void handleGen(PackageElement docElt) {
    for (ScriptEngine engine : engines) {
      current = engine;
      StringWriter buffer = new StringWriter();
      process(buffer, docElt);
      write(docElt, buffer.toString());
    }
  }

  @Override
  protected String renderSource(ExecutableElement elt, String source) {
    return eval("renderSource", elt, source);
  }

  protected String toTypeLink(TypeElement elt) {
    return eval("toTypeLink", elt);
  }

  @Override
  protected String toConstructorLink(ExecutableElement elt) {
    return eval("toConstructorLink", elt);
  }

  protected String toMethodLink(ExecutableElement elt) {
    return eval("toMethodLink", elt);
  }

  @Override
  protected String toFieldLink(VariableElement elt) {
    return eval("toFieldLink", elt);
  }

  private String eval(String functionName, Object... args) {
    Thread currentThread = Thread.currentThread();
    ClassLoader prev = currentThread.getContextClassLoader();
    try {
      ScriptObjectMirror function = (ScriptObjectMirror) current.eval(functionName);
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
