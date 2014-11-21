package io.vertx.docgen;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@SupportedAnnotationTypes({
    "io.vertx.docgen.Document"
})
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class TestGenProcessor extends BaseProcessor {

  Map<String, String> results = new HashMap<>();

  @Override
  protected String resolveLinkTypeDoc(TypeElement elt) {
    return "type";
  }

  @Override
  protected String resolveLinkConstructorDoc(ExecutableElement elt) {
    return "constructor";
  }

  @Override
  protected String resolveLinkMethodDoc(ExecutableElement elt) {
    return "method";
  }

  @Override
  protected String resolveLinkFieldDoc(VariableElement elt) {
    return "field";
  }

  @Override
  protected void handleGen(PackageElement moduleElt, String content) {
    results.put(moduleElt.getQualifiedName().toString(), content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }
}
