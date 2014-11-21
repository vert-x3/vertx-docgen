package io.vertx.docgen;

import javax.lang.model.element.Element;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocException extends RuntimeException {

  final Element element;
  final String msg;

  public DocException(Element element, String msg) {
    super(msg);
    this.element = element;
    this.msg = msg;
  }
}
