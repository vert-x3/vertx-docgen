package io.vertx.docgen;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class BaseProcessor extends AbstractProcessor {

  protected DocTrees docTrees;
  protected Types typeUtils;
  protected Elements elementUtils;
  protected Helper helper;
  Map<String, String> failures = new HashMap<>();

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public Set<String> getSupportedOptions() {
    return new HashSet<>(Arrays.asList("docgen.output", "docgen.extension"));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Document.class.getName());
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    docTrees = DocTrees.instance(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    helper = new Helper(processingEnv);
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
      roundEnv.getElementsAnnotatedWith(Document.class).forEach(elt -> {
        PackageElement pkgElt = (PackageElement) elt;
        try {
          handleGen(pkgElt);
        } catch (Exception e) {
          Element reportedElt = (e instanceof DocGenException) ? ((DocGenException) e).getElement() : elt;
          String msg = e.getMessage();
          if (msg == null) {
            msg = e.toString();
          }
          e.printStackTrace();
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, reportedElt);
          if (reportedElt instanceof PackageElement) {
            failures.put(((PackageElement) reportedElt).getQualifiedName().toString(), msg);
          } else {
            throw new UnsupportedOperationException("not implemented");
          }
        }
      });
    }
    return false;
  }

  protected abstract void handleGen(PackageElement pkgElt);

  /**
   * @return the extension obtained from processor option {@literal docgen.extension} defaults to {@literal .adoc}
   *         when absent.
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
   *   <li>a {@link io.vertx.docgen.Coordinate} object, the coordinate object can have null fields</li>
   *   <li>{@code null} : the current element is being compiled, which likely means create a local link</li>
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

  protected abstract String resolveTypeLink(TypeElement elt, Coordinate coordinate);

  protected abstract String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate);

  protected abstract String resolveMethodLink(ExecutableElement elt, Coordinate coordinate);

  protected abstract String resolveFieldLink(VariableElement elt, Coordinate coordinate);

  protected abstract String renderSource(ExecutableElement elt, String source);

  /**
   * Resolve a label for the specified element, this is used when a link to a program element
   * does not specify an explicit label.<p/>
   *
   * Subclasses can override it to implement a particular behavior for elements.
   *
   * @param elt the elt to resolve a label for
   * @return the label
   */
  protected String resolveLabel(Element elt) {
    String label = elt.getSimpleName().toString();
    if (elt.getModifiers().contains(Modifier.STATIC) &&
        (elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD)) {
      label = elt.getEnclosingElement().getSimpleName() + "." + label;
    }
    return label;
  }

  /**
   * @return the current generator name
   */
  protected abstract String getName();

  private final LinkedList<PackageElement> stack = new LinkedList<>();

  protected final void process(Writer buffer, PackageElement pkgElt) {

    for (PackageElement stackElt : stack) {
      if (pkgElt.getQualifiedName().equals(stackElt.getQualifiedName())) {
        throw new DocException(stack.peekLast(), "Circular include");
      }
    }
    stack.addLast(pkgElt);

    DocWriter writer = new DocWriter(buffer);
    String pkgSource = helper.readSource(pkgElt);
    TreePath pkgTree = docTrees.getPath(pkgElt);
    DocCommentTree doc = docTrees.getDocCommentTree(pkgTree);
    DocTreeVisitor<Void, Void> visitor = new DocTreeScanner<Void, Void>() {

      private void copyContent(DocTree node) {
        int from = (int) docTrees.getSourcePositions().getStartPosition(pkgTree.getCompilationUnit(), doc, node);
        int to = (int) docTrees.getSourcePositions().getEndPosition(pkgTree.getCompilationUnit(), doc, node);;
        writer.append(pkgSource, from, to);
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
            writer.append(getName());
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
        Element resolvedElt = helper.resolveLink(signature);
        if (resolvedElt == null) {
          throw new DocGenException(pkgElt, "Could not resolve " + signature);
        } else if (resolvedElt instanceof PackageElement) {
          PackageElement includedElt = (PackageElement) resolvedElt;
          if (includedElt.getAnnotation(Document.class) == null) {
            process(writer, includedElt);
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
                String fragment = renderSource((ExecutableElement) resolvedElt, source);
                if (fragment != null) {
                  writer.literalMode();
                  writer.append(fragment);
                  writer.commentMode();
                }
                return v;
              default:
                throw new UnsupportedOperationException("todo");
            }
          }
          String link;
          switch (resolvedElt.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM: {
              TypeElement typeElt = (TypeElement) resolvedElt;
              link = resolveTypeLink(typeElt, resolveCoordinate(typeElt));
              break;
            }
            case METHOD: {
              ExecutableElement methodElt = (ExecutableElement) resolvedElt;
              TypeElement typeElt = (TypeElement) methodElt.getEnclosingElement();
              link = resolveMethodLink(methodElt, resolveCoordinate(typeElt));
              break;
            }
            case CONSTRUCTOR: {
              ExecutableElement constructorElt = (ExecutableElement) resolvedElt;
              TypeElement typeElt = (TypeElement) constructorElt.getEnclosingElement();
              link = resolveConstructorLink(constructorElt, resolveCoordinate(typeElt));
              break;
            }
            case FIELD:
            case ENUM_CONSTANT: {
              VariableElement variableElt = (VariableElement) resolvedElt;
              TypeElement typeElt = (TypeElement) variableElt.getEnclosingElement();
              link = resolveFieldLink(variableElt, resolveCoordinate(typeElt));
              break;
            }
            default:
              throw new UnsupportedOperationException("Not yet implemented " + resolvedElt + " with kind " + resolvedElt.getKind());
          }
          String label = render(node.getLabel()).trim();
          if (label.length() == 0) {
            label = resolveLabel(resolvedElt);
          }
          if (link != null)  {
            writer.append("`link:").append(link).append("[").append(label).append("]`");
          } else {
            writer.append("`").append(label).append("`");
          }
        }
        return v;
      }
    };
    doc.accept(visitor, null);
    stack.removeLast();
  }

  protected void write(PackageElement docElt, String content) {
    String outputOpt = processingEnv.getOptions().get("docgen.output");
    if (outputOpt != null) {
      outputOpt = outputOpt.replace("$lang", getName());
      try {
        Document doc = docElt.getAnnotation(Document.class);
        String relativeName = doc.fileName();
        if (relativeName.isEmpty()) {
          relativeName = docElt.getQualifiedName() + getExtension();
        }
        File dir = new File(outputOpt);
        for (int i = relativeName.indexOf('/');i != -1;i = relativeName.indexOf('/', i + 1)) {
          dir = new File(dir, relativeName.substring(0, i));
          relativeName = relativeName.substring(i + 1);
        }
        ensureDir(docElt, dir);
        File file = new File(dir, relativeName);
        try (FileWriter writer = new FileWriter(file)) {
          writer.write(content);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void ensureDir(PackageElement elt, File dir) {
    if (dir.exists()) {
      if (!dir.isDirectory()) {
        throw new DocGenException(elt, "File " + dir.getAbsolutePath() + " is not a dir");
      }
    } else if (!dir.mkdirs()) {
      throw new DocGenException(elt, "could not create dir " + dir.getAbsolutePath());
    }
  }
}
