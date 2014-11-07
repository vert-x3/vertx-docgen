package io.vertx.docgen;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@SupportedAnnotationTypes({
    "io.vertx.codegen.annotations.GenModule"
})
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class TestGenProcessor extends BaseProcessor {

  Map<String, String> results = new HashMap<>();

  protected String resolveLinkTypeDoc(TypeElement elt) {
    return "abc";
  }

  protected String resolveLinkMethodDoc(ExecutableElement elt) {
    return "def";
  }

  @Override
  protected void handleGen(PackageElement moduleElt, String content) {
    results.put(moduleElt.getQualifiedName().toString(), content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }
}
