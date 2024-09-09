package io.vertx.docgen.impl;

/**
 * A post processor filtering out content not matching the current processor language.
 */
public class LanguageFilterPostProcessor implements PostProcessor {
  @Override
  public String getName() {
    return "language";
  }

  @Override
  public String process(String name, String content, String... args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("The post-processor '" + getName() + "' requires at least one argument");
    }
    if (matches(name, args)) {
      return content;
    }
    return EMPTY_CONTENT;
  }

  private boolean matches(String lang, String[] args) {
    for (String arg : args) {
      if (lang.equalsIgnoreCase(arg)) {
        return true;
      }
    }
    return false;
  }
}
