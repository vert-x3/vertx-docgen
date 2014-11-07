package io.vertx.docgen;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocWriterTest {

  @Test
  public void testFormat() throws IOException {
    assertText("abc", "abc");
    assertText(" abc", " abc");
    assertText("abc\ndef", "abc\ndef");
    assertText("abc\n def", "abc\ndef");
    assertText("abc\n  def", "abc\n def");
    assertText("abc\n def\n ghi", "abc\ndef\nghi");
  }

  @Test
  public void testResetParagraph() throws IOException {
    StringWriter buffer = new StringWriter();
    DocWriter writer = new DocWriter(buffer);
    writer.write("abc\n def\n");
    assertEquals("abc\ndef\n", buffer.toString());
    writer.resetParagraph();
    writer.write("ghi\n jkl");
    assertEquals("abc\ndef\nghi\njkl", buffer.toString());
  }

  private void assertText(String actual, String expected) throws IOException {
    StringWriter buffer = new StringWriter();
    DocWriter writer = new DocWriter(buffer);
    writer.write(actual);
    assertEquals(expected, buffer.toString());
  }
}
