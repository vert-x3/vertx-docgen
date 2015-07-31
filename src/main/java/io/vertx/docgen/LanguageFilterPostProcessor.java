package io.vertx.docgen;

/**
 * A post processor filtering out content not matching the current processor language.
 */
public class LanguageFilterPostProcessor implements PostProcessor {
  @Override
  public String getName() {
    return "language";
  }

  @Override
  public String process(BaseProcessor processor, String content, String... args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("The post-processor '" + getName() + "' requires at least one argument");
    }
    if (matches(processor.getName(), args)) {
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
