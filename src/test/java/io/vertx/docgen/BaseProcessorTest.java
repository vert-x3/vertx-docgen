package io.vertx.docgen;

import org.junit.Test;

import javax.annotation.processing.Processor;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
    assertTrue(failDoc("io.vertx.test.includecircular").containsKey("io.vertx.test.includecircular"));
  }

  @Test
  public void testLinkToClass() throws Exception {
    assertEquals("`link:type[TheClass]`", assertDoc("io.vertx.test.linktoclass"));
  }

  @Test
  public void testLinkToEnum() throws Exception {
    assertEquals("`link:enum[TheEnum]`", assertDoc("io.vertx.test.linktoenum"));
  }

  @Test
  public void testLinkToField() throws Exception {
    assertEquals("`link:field[f1]`", assertDoc("io.vertx.test.linktofield"));
  }

  @Test
  public void testLinkToEnumConstant() throws Exception {
    assertEquals("`link:enumConstant[THE_CONSTANT]`", assertDoc("io.vertx.test.linktoenumconstant"));
  }

  @Test
  public void testLinkToStaticField() throws Exception {
    assertEquals("`link:field[TheClass.f1]`", assertDoc("io.vertx.test.linktostaticfield"));
  }

  @Test
  public void testLinkToMethodMember() throws Exception {
    assertEquals("`link:method[m]`", assertDoc("io.vertx.test.linktomethodmember"));
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
        "`link:method[m17]`" +
        "", assertDoc("io.vertx.test.linktomethod"));
  }

  @Test
  public void testLinkToStaticMethod() throws Exception {
    assertEquals(
        "`link:method[TheClass.m]`", assertDoc("io.vertx.test.linktostaticmethod"));
  }

  @Test
  public void testLinkToMethodWithSimpleTypeName() throws Exception {
    assertEquals(
        "`link:method[m1]`\n" +
        "`link:method[m2]`\n" +
        "`link:method[m3]`\n" +
        "`link:method[m4]`" +
        "", assertDoc("io.vertx.test.linktomethodwithsimpletypename"));
  }

  @Test
  public void testLinkToMethodWithUnresolvableType() throws Exception {
    assertTrue(failDoc("io.vertx.test.linktomethodwithunresolvabletype").containsKey("io.vertx.test.linktomethodwithunresolvabletype"));
  }

  @Test
  public void testLinkToConstructor() throws Exception {
    assertEquals("`link:constructor[<init>]`\n`link:constructor[<init>]`", assertDoc("io.vertx.test.linktoconstructor"));
  }

  @Test
  public void testLinkToSameNameFieldAndMethod() throws Exception {
    assertEquals("`link:field[member]`\n`link:method[member]`", assertDoc("io.vertx.test.linktosamenamefieldandmethod"));
  }

  @Test
  public void testLinkToSameNameConstructorAndMethod() throws Exception {
    assertEquals("`link:constructor[<init>]`\n`link:constructor[<init>]`\n`link:constructor[<init>]`", assertDoc("io.vertx.test.linktosamenameconstructorandmethod"));
  }

  @Test
  public void testLinkWithLabel() throws Exception {
    assertEquals("`link:method[the label value]`", assertDoc("io.vertx.test.linkwithlabel"));
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
  public void testLinkToPackage() throws Exception {
    assertEquals("package[io.vertx.test.linktopackage.sub]", assertDoc("io.vertx.test.linktopackage"));
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
  public void testResolveLinkWithClass() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(), "io.vertx.test.linkresolution.resolvable");
    compiler.assertCompile();
    File dependency = compiler.classOutput;
    File metaInf = new File(dependency, "META-INF");
    assertTrue(metaInf.mkdir());
    File manifest = new File(metaInf, "MANIFEST.MF");
    Manifest m = new Manifest();
    m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    m.getMainAttributes().put(new Attributes.Name("Maven-Group-Id"), "foo");
    m.getMainAttributes().put(new Attributes.Name("Maven-Artifact-Id"), "bar");
    m.getMainAttributes().put(new Attributes.Name("Maven-Version"), "1.2.3");
    try (OutputStream out = new FileOutputStream(manifest)) {
      m.write(out);
    }
    LinkedList<Coordinate> resolved = new LinkedList<>();
    compiler = buildCompiler(new TestGenProcessor() {
      @Override
      protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
        resolved.add(coordinate);
        return super.resolveTypeLink(elt, coordinate);
      }
    }, "io.vertx.test.linkresolution.resolving");
    List<File> files = new ArrayList<>();
    files.add(dependency);
    compiler.fileManager.getLocation(StandardLocation.CLASS_PATH).forEach(files::add);
    compiler.fileManager.setLocation(StandardLocation.CLASS_PATH, files);
    compiler.assertCompile();
    String s = compiler.processor.getDoc("io.vertx.test.linkresolution.resolving");
    assertEquals("`link:type[ResolvableType]`", s);
    assertEquals(1, resolved.size());
    assertEquals("foo", resolved.get(0).getGroupId());
    assertEquals("bar", resolved.get(0).getArtifactId());
    assertEquals("1.2.3", resolved.get(0).getVersion());
  }

  @Test
  public void testResolveLinkWithSourceAndClass() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(), "io.vertx.test.linkresolution.resolvable");
    compiler.assertCompile();
    File dependency = compiler.classOutput;
    File metaInf = new File(dependency, "META-INF");
    assertTrue(metaInf.mkdir());
    File manifest = new File(metaInf, "MANIFEST.MF");
    Manifest m = new Manifest();
    m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    m.getMainAttributes().put(new Attributes.Name("Maven-Group-Id"), "foo");
    m.getMainAttributes().put(new Attributes.Name("Maven-Artifact-Id"), "bar");
    m.getMainAttributes().put(new Attributes.Name("Maven-Version"), "1.2.3");
    try (OutputStream out = new FileOutputStream(manifest)) {
      m.write(out);
    }
    LinkedList<Coordinate> resolved = new LinkedList<>();
    compiler = buildCompiler(new TestGenProcessor() {
      @Override
      protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
        resolved.add(coordinate);
        return super.resolveTypeLink(elt, coordinate);
      }
    }, "io.vertx.test.linkresolution");
    List<File> files = new ArrayList<>();
    files.add(dependency);
    compiler.fileManager.getLocation(StandardLocation.CLASS_PATH).forEach(files::add);
    compiler.fileManager.setLocation(StandardLocation.CLASS_PATH, files);
    compiler.assertCompile();
    String s = compiler.processor.getDoc("io.vertx.test.linkresolution.resolving");
    assertEquals("`link:type[ResolvableType]`", s);
    assertEquals(Collections.<Coordinate>singletonList(null), resolved);
  }

  @Test
  public void testLinkUnresolved() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor() {
      @Override
      protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
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
    String processed = compiler.processor.applyPostProcessors(content);

    assertThat(processed, containsString("This is only displayed for java."));
    assertThat(processed, not(containsString("This is only displayed for javascript and ruby.")));
    assertThat(processed, not(containsString("----")));
    assertThat(processed, not(containsString("[language")));


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
    processed = compiler.processor.applyPostProcessors(content);
    assertThat(processed, not(containsString("This is only displayed for java.")));
    assertThat(processed, containsString("This is only displayed for javascript and ruby."));

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
    processed = compiler.processor.applyPostProcessors(content);
    assertThat(processed, not(containsString("This is only displayed for java.")));
    assertThat(processed, not(containsString("This is only displayed for javascript and ruby.")));
  }

  @Test
  public void testMissingPostProcessor() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.missing");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.missing");
    String processed = compiler.processor.applyPostProcessors(content);

    assertThat(processed, containsString("This should be included."));
    assertThat(processed, containsString("[missing]"));
    assertThat(processed, containsString("----"));
  }

  @Test
  public void testCodeBlocks() throws Exception {
    // Code blocks must not be touched by pre-processors.
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.code");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.code");
    String processed = compiler.processor.applyPostProcessors(content);

    assertThat(processed, containsString("[source,java]"));
    assertThat(processed, containsString("[source]"));
    assertThat(processed, containsString("----"));
    assertThat(processed, containsString("System.out.println(\"Hello\");"));
    assertThat(processed, containsString("  System.out.println(\"Bye\");"));
  }

  @Test
  public void testNestedBlocks() throws Exception {
    Compiler<TestGenProcessor> compiler = buildCompiler(new TestGenProcessor(),
        "io.vertx.test.postprocessors.nested");
    compiler.assertCompile();
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.nested");
    String processed = compiler.processor.applyPostProcessors(content);

    assertThat(processed, containsString("[source,java]"));
    assertThat(processed, containsString("----"));
    assertThat(processed, not(containsString("\\----")));
    assertThat(processed, containsString("System.out.println(\"Hello\");"));
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
      public String process(BaseProcessor processor, String content, String... args) {
        return content;
      }
    });
    String content = compiler.processor.getDoc("io.vertx.test.postprocessors.links");
    String processed = compiler.processor.applyPostProcessors(content);
    assertThat(processed, containsString("`link:type[BaseProcessor]`"));
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
    return new Compiler<>(processor, sources, classOutput);
  }
}
