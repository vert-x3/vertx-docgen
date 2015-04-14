package io.vertx.docgen;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
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
   * @param coordinate the optional coordinate of the jar containing the element
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveTypeLink(TypeElement elt, Coordinate coordinate);

  /**
   * Resolve a constructor link.
   *
   * @param elt the element linked to
   * @param coordinate the optional coordinate of the jar containing the element
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate);

  /**
   * Resolve a method link.
   *
   * @param elt the element linked to
   * @param coordinate the optional coordinate of the jar containing the element
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveMethodLink(ExecutableElement elt, Coordinate coordinate);

  /**
   * Resolve a field link.
   *
   * @param elt the element linked to
   * @param coordinate the optional coordinate of the jar containing the element
   * @return the resolved http link or null if the link cannot be resolved
   */
  String resolveFieldLink(VariableElement elt, Coordinate coordinate);

  /**
   * Resolve a label.
   *
   * @param elt the labelled element
   * @param defaultLabel the default label
   * @return the resolved label or null if a label cannot be resolved
   */
  String resolveLabel(Element elt, String defaultLabel);
}
