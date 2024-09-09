package io.vertx.docgen;

import io.vertx.docgen.impl.DocGenerator;
import io.vertx.docgen.impl.JavaDocGenerator;

import javax.lang.model.element.ExecutableElement;
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
      public String resolveTypeLink(TypeElement elt) {
        return TestGenProcessor.this.resolveTypeLink(elt);
      }
      @Override
      public String resolveConstructorLink(ExecutableElement elt) {
        return TestGenProcessor.this.resolveConstructorLink(elt);
      }
      @Override
      public String resolveMethodLink(ExecutableElement elt) {
        return TestGenProcessor.this.resolveMethodLink(elt);
      }
      @Override
      public String resolveFieldLink(VariableElement elt) {
        return TestGenProcessor.this.resolveFieldLink(elt);
      }
    };
  }

  protected String getName() {
    return "java";
  }

  protected String resolveTypeLink(TypeElement elt) {
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

  protected String resolveConstructorLink(ExecutableElement elt) {
    return "constructor";
  }

  protected String resolveMethodLink(ExecutableElement elt) {
    return "method";
  }

  protected String resolveFieldLink(VariableElement elt) {
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
  protected void write(DocGenerator generator, Doc doc, String content) {
    results.put(doc.id(), content);
    super.write(generator, doc, content);
  }

  public String getDoc(String name) {
    return results.get(name);
  }

}
