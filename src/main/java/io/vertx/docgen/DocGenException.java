package io.vertx.docgen;

import javax.lang.model.element.Element;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocGenException extends RuntimeException {

  private final Element element;

  public DocGenException(Element element, String msg) {
    super(msg);
    this.element = element;
  }

  public Element getElement() {
    return element;
  }
}
