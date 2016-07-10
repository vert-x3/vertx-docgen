package io.vertx.docgen;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Post processor interface.
 */
public interface PostProcessor {

  String EMPTY_CONTENT = "";

  Pattern BLOCK_DECLARATION = Pattern.compile("\\[.+\\]");

  String getName();

  String process(String name, String content, String... args);

  static boolean isBlockDeclaration(String line) {
    return BLOCK_DECLARATION.matcher(line).matches();
  }

  static String getProcessorName(String line) {
    // The line starts and ends with "[" and "]".
    int indexOfComma = line.indexOf(",");
    return line.substring(1, indexOfComma != -1 ? indexOfComma : line.length() - 1).trim();
  }

  static String[] getProcessorAttributes(String line) {
    // The line starts and ends with "[" and "]".
    int indexOfComma = line.indexOf(",");
    if (indexOfComma == -1) {
      return new String[0];
    } else {
      return Arrays.stream(line.substring(indexOfComma + 1, line.length() - 1).split(",")).map(String::trim)
          .toArray(String[]::new);
    }
  }

  static String getBlockContent(Iterator<String> iterator) {
    StringBuilder content = new StringBuilder();
    boolean startOfBlock = false;
    boolean endOfBlock = false;
    while (iterator.hasNext() && !endOfBlock) {
      String line = iterator.next().trim();
      if (line.equals("----") && !startOfBlock) {
        startOfBlock = true;
      } else if (line.equals("\\----") && startOfBlock) {
        // Escaped nested block. If the content is using a nested block such as [source], it must "escape" it with
        // \---- instead of ----.
        content.append("----").append("\n");
      } else if (line.equals("----") && startOfBlock) {
        endOfBlock = true;
      } else if (!line.equals("----") && !startOfBlock) {
        // Block without delimiters (1 line only)
        return line;
      } else {
        content.append(line).append("\n");
      }
    }
    return content.toString();
  }
}
