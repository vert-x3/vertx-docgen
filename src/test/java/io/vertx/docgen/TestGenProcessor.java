package io.vertx.docgen;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestGenProcessor extends JavaDocGenProcessor {

  Map<String, String> results = new HashMap<>();

  @Override
  protected String resolveLinkToPackageDoc(PackageElement elt) {
    return "package[" + elt.getQualifiedName() + "]";
  }

  @Override
  protected String toTypeLink(TypeElement elt) {
    switch (elt.getKind()) {
      case INTERFACE:
      case CLASS:
        return "type";
      case ENUM:
        return "enum";
      default:
        return "unsupported";
    }
  }

  @Override
  protected String toConstructorLink(ExecutableElement elt) {
    return "constructor";
  }

  @Override
  protected String toMethodLink(ExecutableElement elt) {
    return "method";
  }

  @Override
  protected String toFieldLink(VariableElement elt) {
    return "field";
  }

  @Override
  protected void handleGen(PackageElement docElt) {
    StringWriter buffer = new StringWriter();
    process(buffer, docElt);
    String content = buffer.toString();
    results.put(docElt.getQualifiedName().toString(), content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }
}
