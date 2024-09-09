package io.vertx.docgen.processor.impl;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DocWriter extends Writer {

  private final StringBuilder delegate;
  private final List<Object> chunks = new ArrayList<>();
  private int status;
  private boolean literal;

  public DocWriter() {
    this.delegate = new StringBuilder();
    this.status = 0;
    this.literal = false;
  }

  public String render() {
    StringBuilder buffer = new StringBuilder();
    render(buffer);
    return buffer.toString();
  }

  private void render(StringBuilder buffer) {
    chunks.forEach(chunk -> {
      if (chunk instanceof Supplier) {
        Supplier<DocWriter> consumer = (Supplier<DocWriter>) chunk;
        DocWriter writer = consumer.get();
        writer.render(buffer);
      } else {
        buffer.append(chunk);
      }
    });
    buffer.append(delegate);
    delegate.setLength(0);
    chunks.clear();
  }

  public void exec(Runnable r) {
    boolean bl = literal;
    literal = false;
    status = 0;
    r.run();
    literal = bl;
  }

  public void resetParagraph() {
    status = 0;
    literal = false;
  }

  /**
   * Switch the write to literal mode: appended text is added as is.
   */
  public void literalMode() {
    literal = true;
  }

  /**
   * Switch to comment mode: after a <code>\n</code> char, the first space is skipped. This is needed
   * because the javadoc text we obtain adds an extra space, except for the first time.
   */
  public void commentMode() {
    literal = false;
  }

  public void write(Supplier<DocWriter> future) {
    if (delegate.length() > 0) {
      String s = delegate.toString();
      delegate.setLength(0);
      chunks.add(s);
    }
    chunks.add(future);
  }

  @Override
  public void write(int c) {
    try {
      super.write(c);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public void write(char[] cbuf) {
    try {
      super.write(cbuf);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public void write(String str) {
    try {
      super.write(str);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public void write(String str, int off, int len) {
    try {
      super.write(str, off, len);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public DocWriter append(CharSequence csq) {
    try {
      return (DocWriter) super.append(csq);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public DocWriter append(CharSequence csq, int start, int end) {
    try {
      return (DocWriter) super.append(csq, start, end);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public DocWriter append(char c) {
    try {
      return (DocWriter) super.append(c);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public void write(char[] cbuf, int off, int len) {
    if (literal) {
      while (off < len) {
        delegate.append(cbuf[off++]);
      }
    } else {
      while (off < len) {
        char c = cbuf[off++];
        switch (c) {
          case '\n':
            status = 1;
            delegate.append(c);
            break;
          case ' ':
            if (status == 1) {
              status = 2;
            } else {
              delegate.append(c);
            }
            break;
          default:
            delegate.append(c);
            if (status == 1) {
              status = 2;
            }
            break;
        }
      }
    }
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void close() throws IOException {
  }
}
