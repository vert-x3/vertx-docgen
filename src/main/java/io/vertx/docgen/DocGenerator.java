package io.vertx.docgen;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface DocGenerator {

  void init(ProcessingEnvironment env);

  String getName();

  String renderSource(ExecutableElement elt, String source);

  String toTypeLink(TypeElement elt, Coordinate coordinate);

  String toConstructorLink(ExecutableElement elt, Coordinate coordinate);

  String toMethodLink(ExecutableElement elt, Coordinate coordinate);

  String toFieldLink(VariableElement elt, Coordinate coordinate);

  String resolveLabel(Element elt);
}
