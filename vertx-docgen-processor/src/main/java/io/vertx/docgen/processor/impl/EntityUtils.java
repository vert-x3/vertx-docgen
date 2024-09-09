package io.vertx.docgen.processor.impl;

/**
 * An utility class to handle entities.
 */
public final class EntityUtils {

  private EntityUtils() {
    // Avoid direct instantiation.
  }

  /**
   * Computes the character represented by the given entity. The entity can be given as {@code #xx} or as
   * {@code uXXXX}. Other form are wrapped into `&` and `;`. This wrapping is allowed as Asciidoctor
   * supports HTML entities, so we don't have to maintain a translation table.
   *
   * @param input the entity
   * @return the represented character
   */
  public static String unescapeEntity(String input) {
    if (input == null || input.trim().length() == 0) {
      return "";
    }
    if (input.startsWith("#")) {
      // The reference number can be either in hex or decimal: &#x020AC; or &#8364;.
      String withoutPrefix = input.substring(1);
      if (! withoutPrefix.isEmpty()  && withoutPrefix.startsWith("x")) {
        withoutPrefix = withoutPrefix.substring(1);
        return parseAsHexa(input, withoutPrefix);
      } else {
        return parseAsDecimal(input, withoutPrefix);
      }
    }
    if (input.startsWith("u")) {
      String withoutPrefix = input.substring(1);
      return parseAsHexa(input, withoutPrefix);
    }

    return "&" + input + ";";
  }

  private static String parseAsDecimal(String input, String withoutPrefix) {
    try {
      int parsed = Integer.parseInt(withoutPrefix);
      return Character.toString((char) parsed);
    } catch (NumberFormatException e) {
      // Invalid format - just return the input
      return input;
    }
  }

  private static String parseAsHexa(String input, String withoutPrefix) {
    try {
      int parsed = (int) Long.parseLong(withoutPrefix, 16);
      return Character.toString((char) parsed);
    } catch (NumberFormatException e) {
      // Invalid format - just return the input
      return input;
    }
  }
}
