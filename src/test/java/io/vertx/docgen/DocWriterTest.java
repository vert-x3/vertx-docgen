package io.vertx.docgen;

import org.junit.Test;

import java.io.IOException;

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
    DocWriter writer = new DocWriter();
    writer.write("abc\n def\n");
    writer.resetParagraph();
    writer.write("ghi\n jkl");
    assertEquals("abc\ndef\nghi\njkl", writer.render());
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

  @Test
  public void testFuture() throws IOException {
    DocWriter writer = new DocWriter();
    writer.write("a");
    writer.write(() -> {
      DocWriter n1 = new DocWriter();
      n1.write("b");
      n1.write(() -> {
        DocWriter n2 = new DocWriter();
        n2.write("c");
        return n2;
      });
      return n1;
    });
    writer.write("d");
    assertEquals("abcd", writer.render());
    assertEquals("", writer.render());
  }

  @Test
  public void testExec() throws IOException {
    DocWriter writer = new DocWriter();
    writer.literalMode();
    writer.write("abc\n def");
    writer.exec(() -> writer.write("\n ghi"));
    writer.write("\n jkl");
    assertEquals("abc\n def\nghi\n jkl", writer.render());
  }

  @Test
  public void testExecAfterNewLine() throws IOException {
    DocWriter writer = new DocWriter();
    writer.write("abc\n");
    writer.exec(() -> writer.write("def"));
    writer.write(" ghi");
    assertEquals("abc\ndef ghi", writer.render());
  }

  private void assertCommentText(String actual, String expected) throws IOException {
    DocWriter writer = new DocWriter();
    writer.write(actual);
    assertEquals(expected, writer.render());
  }

  private void assertLiteralText(String actual, String expected) throws IOException {
    DocWriter writer = new DocWriter();
    writer.literalMode();
    writer.write(actual);
    assertEquals(expected, writer.render());
  }
}
