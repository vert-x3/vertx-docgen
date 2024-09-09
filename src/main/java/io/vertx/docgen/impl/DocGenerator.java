package io.vertx.docgen.impl;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * A doc generator plugin.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface DocGenerator {

  /**
   * Init the generator.
   *
   * @param env the processor environment
   */
  void init(ProcessingEnvironment env);

  /**
   * @return the generator name
   */
  String getName();

  /**
   * Resolve the relative file name of a document, the default implementation returns the {@literal relativeFileName}
   * parameter.
   *
   * @param docElt the doc element
   * @param relativeFileName the relative file name original value
   * @return the relative file name
   */
  default String resolveRelativeFileName(PackageElement docElt, String relativeFileName) {
    return relativeFileName;
  }

  /**
   * Render the source code of the {@code elt} argument.
   *
   * @param elt the element to render
   * @param source the Java source of the element
   * @return the rendered source
   */
  String renderSource(ExecutableElement elt, String source);

  /**
   * Resolve a constructor link.
   *
   * @param elt the element linked to
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveTypeLink(TypeElement elt);

  /**
   * Resolve a constructor link.
   *
   * @param elt the element linked to
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveConstructorLink(ExecutableElement elt);

  /**
   * Resolve a method link.
   *
   * @param elt the element linked to
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveMethodLink(ExecutableElement elt);

  /**
   * Resolve a field link.
   *
   * @param elt the element linked to
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveFieldLink(VariableElement elt);

  /**
   * Resolve a label.
   *
   * @param elt the labelled element
   * @param defaultLabel the default label
   * @return the resolved label or null if a label cannot be resolved
   */
  String resolveLabel(Element elt, String defaultLabel);
}
