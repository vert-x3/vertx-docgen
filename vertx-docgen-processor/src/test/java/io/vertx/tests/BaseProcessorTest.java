package io.vertx.tests;

import io.vertx.docgen.Source;
import io.vertx.docgen.processor.impl.PostProcessor;
import org.junit.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BaseProcessorTest {

  @Test
  public void testIncludePkg() throws Exception {
    assertEquals("before_includesub_contentafter_include", assertDoc("io.vertx.test.includepkg"));
  }

  @Test
  public void testIncludeNonExisting() throws Exception {
    assertTrue(failDoc("io.vertx.test.includenonexisting").containsKey("io.vertx.test.includenonexisting"));
  }

  @Test
  public void testIncludeCircular() throws Exception {
    assertTrue(failDoc("io.vertx.test.includecircular").containsKey("io.vertx.test.includecircular.sub1"));
  }

  @Test
  public void testLinkToClass() throws Exception {
    assertEquals("`link:type[TheClass]`", assertDoc("io.vertx.test.linktoclass"));
    assertEquals("[`TheClass`](type)", assertMarkdownDoc("io.vertx.test.linktoclass"));
  }

  @Test
  public void testLinkToAnnotation() throws Exception {
    assertEquals("`link:annotation[@TheAnnotation]`", assertDoc("io.vertx.test.linktoannotation"));
    assertEquals("[`@TheAnnotation`](annotation)", assertMarkdownDoc("io.vertx.test.linktoannotation"));
  }

  @Test
  public void testLinkToEnum() throws Exception {
    assertEquals("`link:enum[TheEnum]`", assertDoc("io.vertx.test.linktoenum"));
    assertEquals("[`TheEnum`](enum)", assertMarkdownDoc("io.vertx.test.linktoenum"));
  }

  @Test
  public void testLinkToField() throws Exception {
    assertEquals("`link:field[f1]`", assertDoc("io.vertx.test.linktofield"));
    assertEquals("[`f1`](field)", assertMarkdownDoc("io.vertx.test.linktofield"));
  }

  @Test
  public void testLinkToEnumConstant() throws Exception {
    assertEquals("`link:enumConstant[THE_CONSTANT]`", assertDoc("io.vertx.test.linktoenumconstant"));
    assertEquals("[`THE_CONSTANT`](enumConstant)", assertMarkdownDoc("io.vertx.test.linktoenumconstant"));
  }

  @Test
  public void testLinkToStaticField() throws Exception {
    assertEquals("`link:field[TheClass.f1]`", assertDoc("io.vertx.test.linktostaticfield"));
    assertEquals("[`TheClass.f1`](field)", assertMarkdownDoc("io.vertx.test.linktostaticfield"));
  }

  @Test
  public void testLinkToMethodMember() throws Exception {
    assertEquals("`link:method[m]`", assertDoc("io.vertx.test.linktomethodmember"));
    assertEquals("[`m`](method)", assertMarkdownDoc("io.vertx.test.linktomethodmember"));
  }

  @Test
  public void testLinkToMethod() throws Exception {
    assertEquals(
        "`link:method[m1]`\n" +
        "`link:method[m1]`\n" +
        "`link:method[m2]`\n" +
        "`link:method[m2]`\n" +
        "`link:method[m2]`\n" +
        "`link:method[m3]`\n" +
        "`link:method[m3]`\n" +
        "`link:method[m3]`\n" +
        "`link:method[m4]`\n" +
        "`link:method[m5]`\n" +
        "`link:method[m6]`\n" +
        "`link:method[m7]`\n" +
        "`link:method[m8]`\n" +
        "`link:method[m9]`\n" +
        "`link:method[m10]`\n" +
        "`link:method[m11]`\n" +
        "`link:method[m12]`\n" +
        "`link:method[m13]`\n" +
        "`link:method[m14]`\n" +
        "`link:method[m15]`\n" +
        "`link:method[m16]`\n" +
        "`link:method[m17]`\n" +
        "`link:method[m18]`" +
        "", assertDoc("io.vertx.test.linktomethod"));
    assertEquals(
      "[`m1`](method)\n" +
        "[`m1`](method)\n" +
        "[`m2`](method)\n" +
        "[`m2`](method)\n" +
        "[`m2`](method)\n" +
        "[`m3`](method)\n" +
        "[`m3`](method)\n" +
        "[`m3`](method)\n" +
        "[`m4`](method)\n" +
        "[`m5`](method)\n" +
        "[`m6`](method)\n" +
        "[`m7`](method)\n" +
        "[`m8`](method)\n" +
        "[`m9`](method)\n" +
        "[`m10`](method)\n" +
        "[`m11`](method)\n" +
        "[`m12`](method)\n" +
        "[`m13`](method)\n" +
        "[`m14`](method)\n" +
        "[`m15`](method)\n" +
        "[`m16`](method)\n" +
        "[`m17`](method)\n" +
        "[`m18`](method)" +
        "", assertMarkdownDoc("io.vertx.test.linktomethod"));
  }

  @Test
  public void testLinkToStaticMethod() throws Exception {
    assertEquals("`link:method[TheClass.m]`", assertDoc("io.vertx.test.linktostaticmethod"));
    assertEquals("[`TheClass.m`](method)", assertMarkdownDoc("io.vertx.test.linktostaticmethod"));
  }

  @Test
  public void testLinkToMethodWithSimpleTypeName() throws Exception {
    assertEquals(
        "`link:method[m1]`\n" +
        "`link:method[m2]`\n" +
        "`link:method[m3]`\n" +
        "`link:method[m4]`" +
        "", assertDoc("io.vertx.test.linktomethodwithsimpletypename"));
    assertEquals(
      "[`m1`](method)\n" +
        "[`m2`](method)\n" +
        "[`m3`](method)\n" +
        "[`m4`](method)" +
        "", assertMarkdownDoc("io.vertx.test.linktomethodwithsimpletypename"));
  }

  @Test
  public void testLinkToMethodWithUnresolvableType() throws Exception {
    assertTrue(failDoc("io.vertx.test.linktomethodwithunresolvabletype").containsKey("io.vertx.test.linktomethodwithunresolvabletype"));
  }

  @Test
  public void testLinkToConstructor() throws Exception {
    assertEquals("`link:constructor[<init>]`\n`link:constructor[<init>]`", assertDoc("io.vertx.test.linktoconstructor"));
    assertEquals("[`<init>`](constructor)\n[`<init>`](constructor)", assertMarkdownDoc("io.vertx.test.linktoconstructor"));
  }

  @Test
  public void testLinkToSameNameFieldAndMethod() throws Exception {
    assertEquals("`link:field[member]`\n`link:method[member]`", assertDoc("io.vertx.test.linktosamenamefieldandmethod"));
    assertEquals("[`member`](field)\n[`member`](method)", assertMarkdownDoc("io.vertx.test.linktosamenamefieldandmethod"));
  }

  @Test
  public void testLinkToSameNameConstructorAndMethod() throws Exception {
    assertEquals("`link:constructor[<init>]`\n`link:constructor[<init>]`\n`link:constructor[<init>]`", assertDoc("io.vertx.test.linktosamenameconstructorandmethod"));
    assertEquals("[`<init>`](constructor)\n[`<init>`](constructor)\n[`<init>`](constructor)", assertMarkdownDoc("io.vertx.test.linktosamenameconstructorandmethod"));
  }

  @Test
  public void testLinkWithLabel() throws Exception {
    assertEquals("`link:method[the label value]`", assertDoc("io.vertx.test.linkwithlabel"));
    assertEquals("[`the label value`](method)", assertMarkdownDoc("io.vertx.test.linkwithlabel"));
  }

  @Test
  public void testMargin() throws Exception {
    assertEquals("A\nB\nC", assertDoc("io.vertx.test.margin"));
  }

  @Test
  public void testCommentStructure() throws Exception {
    assertEquals("the_first_sentence\n\nthe_body", assertDoc("io.vertx.test.commentstructure"));
  }

  @Test
  public void testIncludeMethodFromAnnotatedClass() throws Exception {
    assertEquals(
        "Map<String, String> map = new HashMap<>();\n" +
        "// Some comment\n" +
        "\n" +
        "if (true) {\n" +
        "  // Indented 1\n" +
        "  if (false) {\n" +
        "    // Indented 2\n" +
        "  }\n" +
        "}\n" +
        "map.put(\"abc\", \"def\");\n" +
        "map.get(\"abc\"); // Beyond last statement", assertDoc("io.vertx.test.includemethodfromannotatedclass"));
  }

  @Test
  public void testIncludeMethodFromAnnotatedMethod() throws Exception {
    assertEquals(
        "int a = 0;", assertDoc("io.vertx.test.includemethodfromannotatedmethod"));
  }

  @Test
  public void testIncludeMethodFromAnnotatedPackage() throws Exception {
    assertEquals(
        "int a = 0;", assertDoc("io.vertx.test.includemethodfromannotatedpkg"));
  }

  @Test
  public void testIncludeAnnotatedClass() throws Exception {
    assertEquals(
        "before_include@Source\n" +
            "public class TheExample {\n" +
            "\n" +
            "  // Some comment\n" +
            "  private String f;\n" +
            "\n" +
            "  public void someMethod() {\n" +
            "    System.out.println(f);\n" +
            "  }\n" +
            "}after_include", assertDoc("io.vertx.test.includeannotatedclass"));
  }

  @Test
  public void testIncludeClassFromAnnotatedPackage() throws Exception {
    assertEquals(
        "before_includepublic class TheExample {\n" +
            "\n" +
            "  // Some comment\n" +
            "  private String f;\n" +
            "\n" +
            "  public void someMethod() {\n" +
            "    System.out.println(f);\n" +
            "  }\n" +
            "}after_include", assertDoc("io.vertx.test.includeclassfromannotatedpkg"));
  }

  @Test
  public void testIncludeAnnotatedInterface() throws Exception {
    assertEquals(
        "before_include@Source\n" +
            "public interface TheExample {\n" +
            "\n" +
            "  void someMethod();\n" +
            "\n" +
            "}after_include", assertDoc("io.vertx.test.includeannotatedinterface"));
  }

  @Test
  public void testIncludeAnnotatedEnum() throws Exception {
    assertEquals(
        "before_include@Source\n" +
            "public enum TheExample {\n" +
            "\n" +
            "  A,\n" +
            "  B,\n" +
            "  C\n" +
            "\n" +
            "}after_include", assertDoc("io.vertx.test.includeannotatedenum"));
  }

  @Test
  public void testIncludeAnnotatedAnnotation() throws Exception {
    assertEquals(
        "before_include@Source\n" +
            "public @interface TheExample {\n" +
            "\n" +
            "  String value() default \"\";\n" +
            "\n" +
            "}after_include", assertDoc("io.vertx.test.includeannotatedannotation"));
  }

  @Test
  public void testLinkToPackage() throws Exception {
    assertEquals("io.vertx.test.linktopackage.sub.adoc", assertDoc("io.vertx.test.linktopackage"));
  }

  @Test
  public void testMarkup() throws Exception {
    assertEquals("<abc>abc_content</abc>\n<def attr=\"value\">def_content</def>\n<ghi>", assertDoc("io.vertx.test.markup"));
  }

  @Test
  public void testCode() throws Exception {
    assertEquals("This comment contains `some code here` and a `literal`.", assertDoc("io.vertx.test.code"));
  }

  /**
   * This test checks whether or not the source are translated depending on the {@link Source#translate()} attribute.
   * It analyses the generation of a document using the default generator and a custom generator.
   */
  @Test
  public void testSource() throws Exception {
    String doc = assertDoc("io.vertx.test.source");

    // Just use the Java processor - eveything is in lower case.

    assertTrue("#1", doc.contains("# 1\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#2", doc.contains("# 2\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#3", doc.contains("# 3\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#4", doc.contains("# 4\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#5", doc.contains("# 5\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#6", doc.contains("# 6\n" +
        "[source, java]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    // Now with the custom generator
    // Source that need to be translated is in uppercase, otherwise lowercase (regular)
    // Are not translated:
    // #2 - because of @Source(translate = false) on the class itself
    // #4 - like #2 - override parent package configuration

    doc = assertDocWithCustomGenerator("io.vertx.test.source");
    assertTrue("#1", doc.contains("# 1\n" +
        "[source, custom]\n" +
        "----\n" +
        "SYSTEM.OUT.PRINTLN(\"HELLO\");\n" +
        "----"));

    assertTrue("#2", doc.contains("# 2\n" +
        "[source, custom]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#3", doc.contains("# 3\n" +
        "[source, custom]\n" +
        "----\n" +
        "SYSTEM.OUT.PRINTLN(\"HELLO\");\n" +
        "----"));

    assertTrue("#4", doc.contains("# 4\n" +
        "[source, custom]\n" +
        "----\n" +
        "System.out.println(\"Hello\");\n" +
        "----"));

    assertTrue("#5", doc.contains("# 5\n" +
        "[source, java]\n" +
        "----\n" +
        "SYSTEM.OUT.PRINTLN(\"HELLO\");\n" +
        "----"));

    assertTrue("#6", doc.contains("# 6\n" +
        "[source, java]\n" +
        "----\n" +
        "SYSTEM.OUT.PRINTLN(\"HELLO\");\n" +
        "----"));
  }

  @Test
  public void testLang() throws Exception {
    assertEquals("The $lang is : java", assertDoc("io.vertx.test.lang"));
  }

  @Test
  public void testEntities() throws Exception {
    final String doc = assertDoc("io.vertx.test.entities");
    assertTrue("Contains 'Foo & Bar'", doc.contains("Foo &amp; Bar"));
    assertTrue("Contains '10 $'", doc.contains("10 $"));
    assertTrue("Contains '10 €", doc.contains("10 €"));
    assertTrue("Contains 'ß'", doc.contains("<p>Straße</p>"));
    assertTrue("Contains 'ß'", doc.contains("<p>Straßen</p>"));
    assertTrue("Contains '\\u00DF'", doc.contains("<p>\\u00DF</p>"));
    assertTrue("Contains correct json", doc.contains("json.put(\"key\", " +
        "\"\\u0000\\u0001\\u0080\\u009f\\u00a0\\u00ff\");\n"));
  }

  @Test
  public void testLinkUnresolved() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor() {
      @Override
      protected String resolveTypeLink(TypeElement elt) {
        return null;
      }
    }, "io.vertx.test.linkunresolved");
    compiler.assertCompile();
    String s = compiler.processor.getDoc("io.vertx.test.linkunresolved");
    assertEquals("`TheClass`", s);
  }

  @Test
  public void testLinkUnresolvedTypeWithSignature() throws Exception {
    failDoc("io.vertx.test.linkunresolvedtypewithsignature");
  }

  @Test
  public void testLanguagePostProcessor() throws Exception {

    // Java
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.language");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.language");

    assertThat(content, containsString("This is only displayed for java."));
    assertThat(content, not(containsString("This is only displayed for javascript and ruby.")));
    assertThat(content, not(containsString("----")));
    assertThat(content, not(containsString("[language")));


    // fake-JavaScript
    compiler = buildCompiler(new TestGenProcessor() {
                               @Override
                               protected String getName() {
                                 return "javascript";
                               }
                             },
        "io.vertx.test.postprocessors.language");
    compiler.assertCompile();
    content = compiler.processor.getDoc("io.vertx.test.postprocessors.language");
    assertThat(content, not(containsString("This is only displayed for java.")));
    assertThat(content, containsString("This is only displayed for javascript and ruby."));

    // fake-groovy
    compiler = buildCompiler(new TestGenProcessor() {
                               @Override
                               protected String getName() {
                                 return "groovy";
                               }
                             },
        "io.vertx.test.postprocessors.language");
    compiler.assertCompile();
    content = compiler.processor.getDoc("io.vertx.test.postprocessors.language");
    assertThat(content, not(containsString("This is only displayed for java.")));
    assertThat(content, not(containsString("This is only displayed for javascript and ruby.")));
  }

  @Test
  public void testMissingPostProcessor() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.missing");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.missing");
//    String processed = compiler.processor.applyPostProcessors(content);

    assertThat(content, containsString("This should be included."));
    assertThat(content, containsString("[missing]"));
    assertThat(content, containsString("----"));
  }

  @Test
  public void testCodeBlocks() throws Exception {
    // Code blocks must not be touched by pre-processors.
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.code");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.code");

    assertThat(content, containsString("[source,java]"));
    assertThat(content, containsString("[source]"));
    assertThat(content, containsString("----"));
    assertThat(content, containsString("System.out.println(\"Hello\");"));
    assertThat(content, containsString("  System.out.println(\"Bye\");"));
  }

  @Test
  public void testNestedBlocks() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.nested");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.nested");

    assertThat(content, containsString("[source,java]"));
    assertThat(content, containsString("----"));
    assertThat(content, not(containsString("\\----")));
    assertThat(content, containsString("System.out.println(\"Hello\");"));
  }

  @Test
  public void testLinksInPostProcessedContent() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.links");
    compiler.assertCompile();
    compiler.processor.registerPostProcessor(new PostProcessor() {
      @Override
      public String getName() {
        return "test";
      }

      @Override
      public String process(String name, String content, String... args) {
        return content;
      }
    });
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.links");
    assertThat(content, containsString("`link:type[BaseProcessor]`"));
  }

  @Test
  public void testVariableSubstitution() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.variables");
    compiler.setOption("foo", "hello");
    compiler.setOption("bar", "not-used");
    compiler.setOption("baz", "vert.x");
    compiler.assertCompile();

    String content = compiler.processor.getDoc("io.vertx.test.variables");
    String processed = compiler.processor.applyVariableSubstitution(content);

    assertThat(processed, containsString("hello"));
    assertThat(processed, containsString("${missing}"));
    assertThat(processed, containsString("vert.x"));
    assertThat(processed, containsString("${}"));
    assertThat(processed, not(containsString("not")));
  }

  @Test
  public void testUnknownTag() throws Exception {
    assertEquals("" +
        "before\n" +
        "@sometag should not be stripped\n" +
        "after", assertDoc("io.vertx.test.unknowntag"));
  }

  @Test
  public void testDocFile() throws Exception {
    assertEquals("the_content", assertDocFile("docs/simple.adoc").getDoc("simple.adoc"));
  }

  @Test
  public void testDocFileLink() throws Exception {
    assertEquals(
      "<1>`link:type[TheClass]`</1>\n" +
      "<2>`link:method[m1]`</2>\n" +
      "<3>`link:method[the label value]`</3>\n" +
      "<4>`link:method[m1]`</4>\n" +
      "<5>`link:method[m2]`</5>\n" +
      "<6>`link:method[m3]`</6>\n" +
      "<7>`link:method[the label value]`</7>",
      assertDocFile("docs/link.adoc").getDoc("link.adoc"));
  }

  @Test
  public void testDocFileLinkAfterNewLine() throws Exception {
    assertEquals(
      "a\n`link:type[TheClass]` watch the space!",
      assertDocFile("docs/linkafternewline.adoc").getDoc("linkafternewline.adoc"));
  }

  @Test
  public void testDocFileLinkWithLabel() throws Exception {
    assertEquals("<before>`link:type[the label value]`<after>", assertDocFile("docs/linkwithlabel.adoc").getDoc("linkwithlabel.adoc"));
  }

  @Test
  public void testDocFileLinkWithLang() throws Exception {
    assertEquals("The $lang is : java", assertDocFile("docs/lang.adoc").getDoc("lang.adoc"));
  }

  @Test
  public void testDocFileInclude() throws Exception {
    assertEquals("<before>Map<String, String> map = new HashMap<>();\n" +
        "// Some comment\n" +
        "\n" +
        "if (true) {\n" +
        "  // Indented 1\n" +
        "  if (false) {\n" +
        "    // Indented 2\n" +
        "  }\n" +
        "}\n" +
        "map.put(\"abc\", \"def\");\n" +
        "map.get(\"abc\"); // Beyond last statement<after>", assertDocFile("docs/include.adoc").getDoc("include.adoc"));
  }

  @Test
  public void testDocFileCodeBlock() throws Exception {
    assertEquals("This is a file with a code block. The indentation\n" +
        " should be kept.\n" +
        "\n" +
        "[source,xml]\n" +
        "----\n" +
        "<foo>\n" +
        "  <bar />\n" +
        "</foo>\n" +
        "----\n" +
        "\n" +
        " This is also a literal.\n" + 
        "\n" +
        "This isn't.\n" +
        "\n" +
        "It should still work after an include.\n" + 
        "\n" +
        "Map<String, String> map = new HashMap<>();\n" +
        "// Some comment\n" +
        "\n" +
        "if (true) {\n" +
        "  // Indented 1\n" +
        "  if (false) {\n" +
        "    // Indented 2\n" +
        "  }\n" +
        "}\n" +
        "map.put(\"abc\", \"def\");\n" +
        "map.get(\"abc\"); // Beyond last statement\n" +
        "\n" +
        "[source,xml]\n" +
        "----\n" +
        "<foo>\n" +
        "  <bar />\n" +
        "</foo>\n" +
        "----", assertDocFile("docs/codeblock.adoc").getDoc("codeblock.adoc"));
  }

  @Test
  public void testDocFileWithLinkToUnresolvableType() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),  "io.vertx.test.file");
    compiler.setOption("docgen.source", docFile("docs/linktounresolvabletype.adoc").getAbsolutePath());
    compiler.failCompile();
  }

  @Test
  public void testDocFileNotFound() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),  "io.vertx.test.file");
    compiler.setOption("docgen.source", new File(new File("."), "does_not_exists").getAbsolutePath());
    compiler.failCompile();
  }

  @Test
  public void testDocFileNotFile() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),  "io.vertx.test.file");
    compiler.setOption("docgen.source", new File(".").getAbsolutePath());
    compiler.failCompile();
  }

  @Test
  public void testDocDir() throws Exception {
    TestGenProcessor processor = assertDocFile("docs/dir");
    assertEquals("foo_content", processor.getDoc("foo.adoc"));
    assertEquals("bar_content", processor.getDoc("bar.adoc"));
    assertEquals("daa_content", processor.getDoc("juu/daa.adoc"));
  }

  @Test
  public void testGen() throws Exception {

    AtomicInteger count = new AtomicInteger();
    AbstractProcessor proc = new AbstractProcessor() {
      @Override
      public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
      }
      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if  (count.getAndIncrement() == 0) {
          try {
            Filer filer = processingEnv.getFiler();
            Element elt = processingEnv.getElementUtils().getTypeElement("gen.GeneratedClass");
            JavaFileObject src = filer.createSourceFile("io.vertx.test.gen.GeneratedClass", elt);
            try (Writer writer = src.openWriter()) {
              writer.append("package io.vertx.test.gen;\npublic class GeneratedClass {\n}");
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        return true;
      }
    };

    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),  "io.vertx.test.gen");
    compiler.addProcessor(proc);
    compiler.assertCompile();
    assertEquals(3, count.get());

  }

  private File docFile(String relativeName) throws Exception {
    URL resource = BaseProcessorTest.class.getClassLoader().getResource(relativeName);
    assertNotNull(resource);
    return new File(resource.toURI());
  }

  private TestGenProcessor assertDocFile(String relativeName) throws Exception {
    File src = docFile(relativeName);
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),  "io.vertx.test.file");
    compiler.setOption("docgen.source", src.getAbsolutePath());
    compiler.assertCompile();
    return compiler.processor;
  }

  private Map<String, String> failDoc(String pkg) throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(), pkg);
    compiler.failCompile();
    return compiler.processor.failures;
  }

  private String assertDoc(String pkg) throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(), pkg);
    compiler.assertCompile();
    return compiler.processor.getDoc(pkg);
  }

  private String assertMarkdownDoc(String pkg) throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(), pkg);
    compiler.setOption("docgen.syntax", "markdown");
    compiler.assertCompile();
    return compiler.processor.getDoc(pkg);
  }

  private String assertDocWithCustomGenerator(String pkg) throws Exception {
    Compiler<CustomTestGenProcessor> compiler = buildCompiler(new CustomTestGenProcessor(), pkg);
    compiler.assertCompile();
    return compiler.processor.getDoc(pkg);
  }

  static <P extends Processor> Compiler<P> buildCompiler(P processor, String pkg) throws Exception {
    int index = 0;
    File output;
    do {
      output = new File("target/" + pkg + (index == 0 ? "" : "" + index));
      index++;
    }
    while (output.exists());
    Path sourcePath = new File(output, "src/" + pkg.replace('.', '/')).toPath();
    File classOutput = new File(output, "classes");
    assertTrue(sourcePath.toFile().mkdirs());
    ArrayList<File> sources = new ArrayList<>();
    Path fromPath = new File("src/test/java/" + pkg.replace('.', '/')).toPath();
    SimpleFileVisitor<Path> visitor =  new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetPath = sourcePath.resolve(fromPath.relativize(dir));
        if (!Files.exists(targetPath)) {
          Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path copy = Files.copy(file, sourcePath.resolve(fromPath.relativize(file)));
        if (copy.toString().endsWith(".java")) {
          sources.add(copy.toFile());
        }
        return FileVisitResult.CONTINUE;
      }
    };
    Files.walkFileTree(fromPath, visitor);
    assertTrue(sources.size() > 0);
    return new Compiler<>(sources, classOutput, processor);
  }
}
