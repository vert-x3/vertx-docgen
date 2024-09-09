package io.vertx.docgen;

import io.vertx.docgen.impl.LanguageFilterPostProcessor;
import io.vertx.docgen.impl.PostProcessor;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * Checks the {@link LanguageFilterPostProcessor}.
 */
public class LanguageFilterPostProcessorTest {

  LanguageFilterPostProcessor postProcessor = new LanguageFilterPostProcessor();

  @Test
  public void testThatMatchingLanguagesAreNotFilteredOut() {
    String content = "This is something only for java";
    String result = postProcessor.process("java", content, "java");
    assertThat(result, containsString(content));

    result = postProcessor.process("java", content, "java", "javascript", "ruby");
    assertThat(result, containsString(content));
  }

  @Test
  public void testThatNotMatchingLanguagesAreFilteredOut() {
    String content = "This is something only for javascript and ruby";
    String result = postProcessor.process("java", content, "java");
    assertThat(result, containsString(content));

    result = postProcessor.process("java", content, "javascript", "ruby");
    assertThat(result, not(containsString(content)));
    assertThat(result, equalTo(PostProcessor.EMPTY_CONTENT));
  }

  @Test
  public void testWhenContentIsEmpty() {
    String content = "";
    String result = postProcessor.process("java", content, "java");
    assertThat(result, containsString(content));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithNoArgs() {
    String content = "";
    String result = postProcessor.process("java", content);
    assertThat(result, containsString(content));
  }

}
