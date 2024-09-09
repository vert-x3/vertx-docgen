package io.vertx.docgen.processor.impl;

enum Syntax {

  ASCIIDOC() {
    @Override
    void writeLink(String link, String s, DocWriter writer) {
      writer.append("`link:").append(link).append("[").append(s).append("]`");
    }
  }, MARKDOWN() {
    @Override
    void writeLink(String link, String s, DocWriter writer) {
      writer.append("[`").append(s).append("`](").append(link).append(")");
    }
  };

  abstract void writeLink(String link, String s, DocWriter writer);

}
