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
    assertCommentText("abc", "abc");
    assertCommentText(" abc", " abc");
    assertCommentText("abc\ndef", "abc\ndef");
    assertCommentText("abc\n def", "abc\ndef");
    assertCommentText("abc\n  def", "abc\n def");
    assertCommentText("abc\n def\n ghi", "abc\ndef\nghi");
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

  @Test
  public void testLiteralMode() throws IOException {
    assertLiteralText("abc", "abc");
    assertLiteralText(" abc", " abc");
    assertLiteralText("abc\ndef", "abc\ndef");
    assertLiteralText("abc\n def", "abc\n def");
    assertLiteralText("abc\n  def", "abc\n  def");
    assertLiteralText("abc\n def\n ghi", "abc\n def\n ghi");
  }

  private void assertCommentText(String actual, String expected) throws IOException {
    StringWriter buffer = new StringWriter();
    DocWriter writer = new DocWriter(buffer);
    writer.write(actual);
    assertEquals(expected, buffer.toString());
  }

  private void assertLiteralText(String actual, String expected) throws IOException {
    StringWriter buffer = new StringWriter();
    DocWriter writer = new DocWriter(buffer);
    writer.literalMode();
    writer.write(actual);
    assertEquals(expected, buffer.toString());
  }
}
