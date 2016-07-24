package io.vertx.docgen;

import com.sun.source.doctree.*;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class BaseProcessor extends AbstractProcessor {

  protected DocTrees docTrees;
  protected Helper helper;
  protected List<String> sources;
  protected Set<PostProcessor> postProcessors = new LinkedHashSet<>();
  Map<String, String> failures = new HashMap<>();

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public Set<String> getSupportedOptions() {
    return new HashSet<>(Arrays.asList("docgen.output", "docgen.extension", "docgen.source"));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton("*");
  }

  public synchronized BaseProcessor registerPostProcessor(PostProcessor postProcessor) {
    if (getPostProcessor(postProcessor.getName()) != null) {
      throw new IllegalArgumentException("Post-processor with name '" + postProcessor.getName() + "' is already " +
          "registered.");
    }
    postProcessors.add(postProcessor);
    return this;
  }

  public synchronized PostProcessor getPostProcessor(String name) {
    for (PostProcessor pp : postProcessors) {
      if (pp.getName().equalsIgnoreCase(name)) {
        return pp;
      }
    }
    return null;
  }


  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    String sourceOpt = processingEnv.getOptions().get("docgen.source");
    if (sourceOpt != null) {
      sources = new ArrayList<>(Arrays.asList(sourceOpt.split("\\s*,\\s*")));
    }
    docTrees = DocTrees.instance(processingEnv);
    helper = new Helper(processingEnv);
    registerPostProcessor(new LanguageFilterPostProcessor());
  }

  private String render(List<? extends DocTree> trees) {
    StringBuilder buffer = new StringBuilder();
    DocTreeVisitor<Void, Void> visitor = new DocTreeScanner<Void, Void>() {
      @Override
      public Void visitText(TextTree node, Void aVoid) {
        buffer.append(node.getBody());
        return super.visitText(node, aVoid);
      }
    };
    trees.forEach(tree -> tree.accept(visitor, null));
    return buffer.toString();
  }


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (failures.isEmpty()) {
      try {
        if (!roundEnv.processingOver()) {
          roundEnv.getElementsAnnotatedWith(Document.class).forEach(elt -> {
            try {
              handleGen(new PackageDoc((PackageElement) elt));
            } catch (DocGenException e) {
              if (e.element == null) {
                e.element = elt;
              }
              throw e;
            }
          });
        } else {
          if (sources != null) {
            for (String source : sources) {
              File f = new File(source);
              if (!f.exists()) {
                throw new FileNotFoundException("Cannot process document " + source);
              }
              if (!f.isFile()) {
                throw new IOException("Document " + source + " is not a file");
              }
              handleGen(new FileDoc(f));
            }
          }
        }
      } catch(Exception e) {
        Element reportedElt = (e instanceof DocGenException) ? ((DocGenException) e).element : null;
        String msg = e.getMessage();
        if (msg == null) {
          msg = e.toString();
        }
        e.printStackTrace();
        if (reportedElt != null) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, reportedElt);
          if (reportedElt instanceof PackageElement) {
            failures.put(((PackageElement) reportedElt).getQualifiedName().toString(), msg);
          } else {
            throw new UnsupportedOperationException("not implemented");
          }
        } else {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
      }
    }
    return false;
  }

  protected abstract Iterable<DocGenerator> generators();

  private final void handleGen(Doc doc) {
    for (DocGenerator generator : generators()) {
      generator.init(processingEnv);
      StringWriter buffer = new StringWriter();
      process(generator, buffer, doc);
      String content = postProcess(generator.getName(), buffer.toString());
      write(generator, doc, content);
    }
  }

  /**
   * @return the extension obtained from processor option {@literal docgen.extension} defaults to {@literal .adoc}
   * when absent.
   */
  protected String getExtension() {
    String extension = processingEnv.getOptions().get("docgen.extension");
    if (extension != null) {
      return extension;
    }
    return ".adoc";
  }

  protected String resolveLinkToPackageDoc(PackageElement elt) {
    Document annotation = elt.getAnnotation(Document.class);
    String fileName = annotation.fileName();
    if (fileName.isEmpty()) {
      return elt.toString() + getExtension();
    } else {
      return fileName;
    }
  }

  /**
   * Resolve the coordinate of the type element, this method returns either:
   * <ul>
   * <li>a {@link io.vertx.docgen.Coordinate} object, the coordinate object can have null fields</li>
   * <li>{@code null} : the current element is being compiled, which likely means create a local link</li>
   * </ul>
   *
   * @param typeElt the type element to resolve
   * @return the resolved coordinate object or null if the element is locally compiled
   */
  private Coordinate resolveCoordinate(TypeElement typeElt) {
    try {
      Symbol.ClassSymbol cs = (Symbol.ClassSymbol) typeElt;
      if (cs.sourcefile != null && getURL(cs.sourcefile) != null) {
        // .java source we can link locally
        return null;
      }
      if (cs.classfile != null) {
        JavaFileObject cf = cs.classfile;
        URL classURL = getURL(cf);
        if (classURL != null && classURL.getFile().endsWith(".class")) {
          URL manifestURL = new URL(classURL.toString().substring(0, classURL.toString().length() - (typeElt.getQualifiedName().toString().length() + 6)) + "META-INF/MANIFEST.MF");
          InputStream manifestIs = manifestURL.openStream();
          if (manifestIs != null) {
            Manifest manifest = new Manifest(manifestIs);
            Attributes attributes = manifest.getMainAttributes();
            String groupId = attributes.getValue(new Attributes.Name("Maven-Group-Id"));
            String artifactId = attributes.getValue(new Attributes.Name("Maven-Artifact-Id"));
            String version = attributes.getValue(new Attributes.Name("Maven-Version"));
            return new Coordinate(groupId, artifactId, version);
          }
        }
      }
    } catch (Exception ignore) {
      //
    }
    return new Coordinate(null, null, null);
  }

  private URL getURL(JavaFileObject fileObject) {
    try {
      return fileObject.toUri().toURL();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Resolve a label for the specified element, this is used when a link to a program element
   * does not specify an explicit label.<p/>
   * <p/>
   * Subclasses can override it to implement a particular behavior for elements.
   *
   * @param elt the elt to resolve a label for
   * @return the label
   */
  private String resolveLabel(DocGenerator generator, Element elt) {
    String label = elt.getSimpleName().toString();
    if (elt.getModifiers().contains(Modifier.STATIC) &&
        (elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD)) {
      label = elt.getEnclosingElement().getSimpleName() + "." + label;
    }
    if (elt.getKind() == ElementKind.ANNOTATION_TYPE) {
      label = "@" +label;
    }
    return generator.resolveLabel(elt, label);
  }

  private final LinkedList<PackageElement> stack = new LinkedList<>();

  interface Doc {
    String id();
    String resolveRelativeFileName(DocGenerator generator);
  }

  class PackageDoc implements Doc {

    final PackageElement elt;

    PackageDoc(PackageElement elt) {
      this.elt = elt;
    }

    @Override
    public String id() {
      return elt.getQualifiedName().toString();
    }

    /**
     * Return the relative file name of a document.
     *
     * @param generator the doc generator
     * @return the relative file name
     */
    public String resolveRelativeFileName(DocGenerator generator) {
      Document doc = elt.getAnnotation(Document.class);
      String relativeName = doc.fileName();
      if (relativeName.isEmpty()) {
        relativeName = elt.getQualifiedName() + getExtension();
      }
      return generator.resolveRelativeFileName(elt, relativeName);
    }
  }

  class FileDoc implements Doc {

    final File file;

    FileDoc(File file) {
      this.file = file;
    }

    @Override
    public String id() {
      return file.getName();
    }

    @Override
    public String resolveRelativeFileName(DocGenerator generator) {
      return file.getName();
    }
  }

  protected final void process(DocGenerator generator, Writer buffer, Doc doc) {

    if (doc instanceof PackageDoc) {
      PackageElement pkgElt = ((PackageDoc) doc).elt;
      for (PackageElement stackElt : stack) {
        if (pkgElt.getQualifiedName().equals(stackElt.getQualifiedName())) {
          throw new DocGenException(stack.peekLast(), "Circular include");
        }
      }
      stack.addLast(pkgElt);

      DocWriter writer = new DocWriter(buffer);
      String pkgSource = helper.readSource(pkgElt);
      TreePath pkgPath = docTrees.getPath(pkgElt);
      DocCommentTree docTree = docTrees.getDocCommentTree(pkgPath);
      DocTreeVisitor<Void, Void> visitor = new DocTreeScanner<Void, Void>() {

        private void copyContent(DocTree node) {
          int from = (int) docTrees.getSourcePositions().getStartPosition(pkgPath.getCompilationUnit(), docTree, node);
          int to = (int) docTrees.getSourcePositions().getEndPosition(pkgPath.getCompilationUnit(), docTree, node);
          writer.append(pkgSource, from, to);
        }

        @Override
        public Void visitUnknownBlockTag(UnknownBlockTagTree node, Void v) {
          writer.append("@").append(node.getTagName()).append(" ");
          return super.visitUnknownBlockTag(node, v);
        }

        @Override
        public Void visitDocComment(DocCommentTree node, Void v) {
          v = scan(node.getFirstSentence(), v);
          List<? extends DocTree> body = node.getBody();
          if (body.size() > 0) {
            writer.append("\n\n");
            writer.resetParagraph();
            v = scan(body, v);
          }
          List<? extends DocTree> blockTags = node.getBlockTags();
          if (blockTags.size() > 0) {
            writer.append("\n");
            v = scan(blockTags, v);
          }
          return v;
        }

        @Override
        public Void visitErroneous(ErroneousTree node, Void v) {
          return visitText(node, v);
        }

        @Override
        public Void visitText(TextTree node, Void v) {
          String body = node.getBody();
          Matcher matcher = Helper.LANG_PATTERN.matcher(body);
          int prev = 0;
          while (matcher.find()) {
            writer.append(body, prev, matcher.start());
            if (matcher.group(1) != null) {
              // \$lang
              writer.append("$lang");
            } else {
              writer.append(generator.getName());
            }
            prev = matcher.end();
          }
          writer.append(body, prev, body.length());
          return super.visitText(node, v);
        }

        /**
         * Handles both literal and code. We generate the asciidoc output using {@literal `}.
         */
        @Override
        public Void visitLiteral(LiteralTree node, Void aVoid) {
          writer.append("`").append(node.getBody().getBody()).append("`");
          return super.visitLiteral(node, aVoid);
        }

        @Override
        public Void visitEntity(EntityTree node, Void aVoid) {
          writer.append(EntityUtils.unescapeEntity(node.getName().toString()));
          return super.visitEntity(node, aVoid);
        }

        @Override
        public Void visitStartElement(StartElementTree node, Void v) {
          copyContent(node);
          return v;
        }

        @Override
        public Void visitEndElement(EndElementTree node, Void v) {
          writer.write("</");
          writer.append(node.getName());
          writer.append('>');
          return v;
        }

        @Override
        public Void visitLink(LinkTree node, Void v) {
          String signature = node.getReference().getSignature();
          String label = render(node.getLabel()).trim();
          BaseProcessor.this.visitLink(pkgElt, label, signature, generator, writer);
          return v;
        }
      };
      docTree.accept(visitor, null);
      stack.removeLast();
    } else {
      FileDoc fileDoc = (FileDoc) doc;
      try {
        String content = new String(Files.readAllBytes(fileDoc.file.toPath()), StandardCharsets.UTF_8);
        Matcher matcher = ABC.matcher(content);
        int prev = 0;
        while (matcher.find()) {
          buffer.write(content, prev, matcher.start() - prev);
          String value = matcher.group(1).trim();
          StringTokenizer tokenizer = new StringTokenizer(value);
          if (tokenizer.hasMoreTokens()) {
            String signature = tokenizer.nextToken();
            String label = value.substring(signature.length()).trim();
            BaseProcessor.this.visitLink(null, label, signature, generator, new DocWriter(buffer));
          }
          prev = matcher.end();
        }
        buffer.append(content, prev, content.length());
      } catch (IOException e) {
        throw new DocGenException(e.getMessage());
      }
    }
  }

  private static final Pattern ABC = Pattern.compile("\\{@link\\s([^}]+)\\}");
  private static final Pattern DEF = Pattern.compile("\\s");

  private void visitLink(PackageElement pkgElt, String label, String signature, DocGenerator generator, DocWriter writer) {
    Element resolvedElt = helper.resolveLink(signature);
    if (resolvedElt == null) {
      throw new DocGenException(pkgElt, "Could not resolve " + signature);
    } else if (resolvedElt instanceof PackageElement) {
      PackageElement includedElt = (PackageElement) resolvedElt;
      if (includedElt.getAnnotation(Document.class) == null) {
        process(generator, writer, new PackageDoc(includedElt));
      } else {
        String link = resolveLinkToPackageDoc((PackageElement) resolvedElt);
        writer.append(link);
      }
    } else {
      if (helper.isExample(resolvedElt)) {
        String source = helper.readSource(resolvedElt);
        switch (resolvedElt.getKind()) {
          case CONSTRUCTOR:
          case METHOD:
            // Check whether or not the fragment must be translated
            String fragment;
            if (helper.hasToBeTranslated(resolvedElt)) {
              // Invoke the custom renderer, this may should the translation to the expected language.
              fragment = generator.renderSource((ExecutableElement) resolvedElt, source);
            } else {
              // Do not call the custom rendering process, just use the default / java one.
              JavaDocGenerator javaGen = new JavaDocGenerator();
              javaGen.init(processingEnv);
              fragment = javaGen.renderSource((ExecutableElement) resolvedElt, source);
            }
            if (fragment != null) {
              writer.literalMode();
              writer.append(fragment);
              writer.commentMode();
            }
            return;
          case CLASS:
            if (helper.hasToBeTranslated(resolvedElt)) {
              throw new UnsupportedOperationException("File inclusion is only supported for not translated" +
                  " Java classes");
            }
            if (source != null) {
              writer.literalMode();
              writer.append(source);
              writer.commentMode();
            }
            return;
          default:
            throw new UnsupportedOperationException("unsupported element: " + resolvedElt.getKind());
        }
      }
      String link;
      switch (resolvedElt.getKind()) {
        case CLASS:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case ENUM: {
          TypeElement typeElt = (TypeElement) resolvedElt;
          link = generator.resolveTypeLink(typeElt, resolveCoordinate(typeElt));
          break;
        }
        case METHOD: {
          ExecutableElement methodElt = (ExecutableElement) resolvedElt;
          TypeElement typeElt = (TypeElement) methodElt.getEnclosingElement();
          link = generator.resolveMethodLink(methodElt, resolveCoordinate(typeElt));
          break;
        }
        case CONSTRUCTOR: {
          ExecutableElement constructorElt = (ExecutableElement) resolvedElt;
          TypeElement typeElt = (TypeElement) constructorElt.getEnclosingElement();
          link = generator.resolveConstructorLink(constructorElt, resolveCoordinate(typeElt));
          break;
        }
        case FIELD:
        case ENUM_CONSTANT: {
          VariableElement variableElt = (VariableElement) resolvedElt;
          TypeElement typeElt = (TypeElement) variableElt.getEnclosingElement();
          link = generator.resolveFieldLink(variableElt, resolveCoordinate(typeElt));
          break;
        }
        default:
          throw new UnsupportedOperationException("Not yet implemented " + resolvedElt + " with kind " + resolvedElt.getKind());
      }
      if (label.length() == 0) {
        label = resolveLabel(generator, resolvedElt);
      }
      if (link != null) {
        writer.append("`link:").append(link).append("[").append(label).append("]`");
      } else {
        writer.append("`").append(label).append("`");
      }
    }
  }

  protected String postProcess(String name, String content) {
    String processed = applyVariableSubstitution(content);
    processed = applyPostProcessors(name, processed);
    return processed;
  }

  protected void write(DocGenerator generator, Doc doc, String content) {
    String outputOpt = processingEnv.getOptions().get("docgen.output");
    if (outputOpt != null) {
      outputOpt = outputOpt.replace("$lang", generator.getName());
      String relativeName = doc.resolveRelativeFileName(generator);
      try {
        File dir = new File(outputOpt);
        for (int i = relativeName.indexOf('/'); i != -1; i = relativeName.indexOf('/', i + 1)) {
          dir = new File(dir, relativeName.substring(0, i));
          relativeName = relativeName.substring(i + 1);
        }
        ensureDir(dir);
        File file = new File(dir, relativeName);
        try (FileWriter writer = new FileWriter(file)) {
          writer.write(content);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Apply post-processors.
   *
   * @param content the (asciidoc) content
   * @return the content after post-processing.
   */
  protected String applyPostProcessors(String name2, String content) {
    final List<String> lines = Arrays.asList(content.split("\r?\n"));
    StringBuilder processed = new StringBuilder();
    Iterator<String> iterator = lines.iterator();
    while (iterator.hasNext()) {
      String line = iterator.next();
      String trimmedLine = line.trim();
      if (!PostProcessor.isBlockDeclaration(trimmedLine)) {
        processed.append(line);
        if (iterator.hasNext()) {
          processed.append("\n");
        }
      } else {
        String name = PostProcessor.getProcessorName(trimmedLine);
        String[] attributes = PostProcessor.getProcessorAttributes(trimmedLine);
        PostProcessor postProcessor = getPostProcessor(name);
        if (postProcessor == null) {
          processed.append(line);
          if (iterator.hasNext()) {
            processed.append("\n");
          }
        } else {
          // Extract content.
          String block = PostProcessor.getBlockContent(iterator);
          processed.append(postProcessor.process(name2, block, attributes));
          if (iterator.hasNext()) {
            processed.append("\n");
          }
        }
      }
    }
    return processed.toString();
  }

  private void ensureDir(File dir) {
    if (dir.exists()) {
      if (!dir.isDirectory()) {
        throw new DocGenException("File " + dir.getAbsolutePath() + " is not a dir");
      }
    } else if (!dir.mkdirs()) {
      throw new DocGenException("could not create dir " + dir.getAbsolutePath());
    }
  }

  /**
   * Replace `@{var} by the variable value passed to the annotation processor.
   *
   * @param content the content
   * @return the content with variable values
   */
  public String applyVariableSubstitution(String content) {
    for (Map.Entry<String, String> entry : processingEnv.getOptions().entrySet()) {
      content = content.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return content;
  }
}
