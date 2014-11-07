package io.vertx.docgen;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class DocWriter extends Writer {

  final Writer delegate;
  private int status;

  DocWriter(Writer delegate) {
    this.delegate = delegate;
    this.status = 0;
  }

  void resetParagraph() {
    status = 0;
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
    try {
      while (off < len) {
        char c = cbuf[off++];
        switch (c) {
          case '\n':
            status = 1;
            delegate.write(c);
            break;
          case ' ':
            if (status == 1) {
              status = 2;
            } else {
              delegate.write(c);
            }
            break;
          default:
            delegate.write(c);
            if (status == 1) {
              status = 2;
            }
            break;
        }
      }
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void close() throws IOException {
  }
}
