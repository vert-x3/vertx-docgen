package io.vertx.docgen;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestGenProcessor extends JavaDocGenProcessor {

  Map<String, String> results = new HashMap<>();

  @Override
  protected DocGenerator generator() {
    return new JavaDocGenerator() {
      @Override
      public String getName() {
        return TestGenProcessor.this.getName();
      }
      @Override
      public String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
        return TestGenProcessor.this.resolveTypeLink(elt, coordinate);
      }
      @Override
      public String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
        return TestGenProcessor.this.resolveConstructorLink(elt, coordinate);
      }
      @Override
      public String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
        return TestGenProcessor.this.resolveMethodLink(elt, coordinate);
      }
      @Override
      public String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
        return TestGenProcessor.this.resolveFieldLink(elt, coordinate);
      }
    };
  }

  protected String getName() {
    return "java";
  }

  protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
    switch (elt.getKind()) {
      case ANNOTATION_TYPE:
        return "annotation";
      case INTERFACE:
      case CLASS:
        return "type";
      case ENUM:
        return "enum";
      default:
        return "unsupported";
    }
  }

  protected String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
    return "constructor";
  }

  protected String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
    return "method";
  }

  protected String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
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
  protected void write(DocGenerator generator, PackageElement docElt, String content) {
    results.put(docElt.getQualifiedName().toString(), content);
    super.write(generator, docElt, content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }

}
