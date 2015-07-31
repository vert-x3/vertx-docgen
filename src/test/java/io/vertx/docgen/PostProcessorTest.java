package io.vertx.docgen;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Check the behavior of {@link PostProcessor}.
 */
public class PostProcessorTest {

  @Test
  public void testBlockDeclaration() {
    assertThat(PostProcessor.isBlockDeclaration("[foo]"), equalTo(true));
    assertThat(PostProcessor.isBlockDeclaration("[foo,a,b,c]"), equalTo(true));
    assertThat(PostProcessor.isBlockDeclaration("[foo ]"), equalTo(true));
    assertThat(PostProcessor.isBlockDeclaration("[ foo ]"), equalTo(true));
    assertThat(PostProcessor.isBlockDeclaration("foo"), equalTo(false));
    assertThat(PostProcessor.isBlockDeclaration(""), equalTo(false));
    assertThat(PostProcessor.isBlockDeclaration("[]"), equalTo(false));

    assertThat(PostProcessor.isBlockDeclaration("[ ]"), equalTo(true));
    assertThat(PostProcessor.isBlockDeclaration("[X]"), equalTo(true));
  }

  @Test
  public void testProcessorNameExtraction() {
    assertThat(PostProcessor.getProcessorName("[foo]"), equalTo("foo"));
    assertThat(PostProcessor.getProcessorName("[foo ]"), equalTo("foo"));
    assertThat(PostProcessor.getProcessorName("[ foo]"), equalTo("foo"));
    assertThat(PostProcessor.getProcessorName("[ foo ]"), equalTo("foo"));
    assertThat(PostProcessor.getProcessorName("[foo,a,b,c]"), equalTo("foo"));
    assertThat(PostProcessor.getProcessorName("[foo, a,b,c]"), equalTo("foo"));
  }

  @Test
  public void testProcessorAttributeExtraction() {
    assertThat(PostProcessor.getProcessorAttributes("[foo]").length, equalTo(0));
    // First parameter empty
    assertThat(PostProcessor.getProcessorAttributes("[foo,]").length, equalTo(1));
    assertThat(PostProcessor.getProcessorAttributes("[foo,]")[0], equalTo(""));
    assertThat(PostProcessor.getProcessorAttributes("[foo,,,]").length, equalTo(0));

    assertThat(PostProcessor.getProcessorAttributes("[foo, a,b,c ]")[0], equalTo("a"));
    assertThat(PostProcessor.getProcessorAttributes("[foo,a,b,c ]")[1], equalTo("b"));
    assertThat(PostProcessor.getProcessorAttributes("[foo, a,b,c ]")[2], equalTo("c"));
  }


  @Test
  public void testContentExtractionWithSingleLineBlock() {
    List<String> lines = Arrays.asList(
        "line 1",
        "line 2"
    );
    assertThat(PostProcessor.getBlockContent(lines.iterator()), containsString("line 1"));
    assertThat(PostProcessor.getBlockContent(lines.iterator()), not(containsString("line 2")));
  }

  @Test
  public void testContentExtractionWithBlock() {
    List<String> lines = Arrays.asList(
        "----",
        "line 1",
        "line 2",
        "----",
        "line 3"
    );
    assertThat(PostProcessor.getBlockContent(lines.iterator()), containsString("line 1"));
    assertThat(PostProcessor.getBlockContent(lines.iterator()), containsString("line 2"));
    assertThat(PostProcessor.getBlockContent(lines.iterator()), not(containsString("line 3")));
    assertThat(PostProcessor.getBlockContent(lines.iterator()), not(containsString("----")));
  }

}