package io.vertx.docgen;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom processor.
 * On purpose it transforms source to uppercase.
 */
public class CustomTestGenProcessor extends JavaDocGenProcessor {

  Map<String, String> results = new HashMap<>();

  @Override
  protected DocGenerator generator() {
    return new JavaDocGenerator() {
      @Override
      public String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
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
      public String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
        return "constructor";
      }

      @Override
      public String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
        return "method";
      }

      @Override
      public String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
        switch (elt.getKind()) {
          case ENUM_CONSTANT:
            return "enumConstant";
          case FIELD:
            return "field";
          default:
            return "unsupported";
        }
      }

      @Override
      public String getName() {
        return "custom";
      }

      @Override
      public String renderSource(ExecutableElement elt, String source) {
        return super.renderSource(elt, source).toUpperCase();
      }
    };
  }

  @Override
  protected void write(DocGenerator generator, Doc doc, String content) {
    results.put(doc.id(), content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }

}
