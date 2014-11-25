package io.vertx.docgen;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class BaseProcessor extends AbstractProcessor {

  private DocTrees docTrees;
  private Types typeUtils;
  private Elements elementUtils;
  private Helper helper;
  Map<String, String> failures = new HashMap<>();

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

  protected abstract String resolveLinkgPackageDoc(PackageElement elt);

  protected abstract String toTypeLink(TypeElement elt);

  protected abstract String toConstructorLink(ExecutableElement elt);

  protected abstract String toMethodLink(ExecutableElement elt);

  protected abstract String toFieldLink(VariableElement elt);

  protected String renderSource(ExecutableElement elt, String source) {
    TreePath resolvedTP = docTrees.getPath(elt);
    CompilationUnitTree unit = resolvedTP.getCompilationUnit();
    MethodTree methodTree = (MethodTree) resolvedTP.getLeaf();
    BlockTree blockTree = methodTree.getBody();
    // Get block
    List<? extends StatementTree> statements = blockTree.getStatements();
    if (statements.size() > 0) {
      int from = (int) docTrees.getSourcePositions().getStartPosition(unit, statements.get(0));
      int to = (int) docTrees.getSourcePositions().getEndPosition(unit, statements.get(statements.size() - 1));
      // Correct boundaries
      while (from > 1 && source.charAt(from - 1) != '\n') {
        from--;
      }
      while (to < source.length() && source.charAt(to) != '\n') {
        to++;
      }
      String block = source.substring(from, to);
      // Determine margin
      int blockMargin = Integer.MAX_VALUE;
      LineMap lineMap = unit.getLineMap();
      for (StatementTree statement : statements) {
        int statementStart = (int) docTrees.getSourcePositions().getStartPosition(unit, statement);
        int lineStart = statementStart;
        while (lineMap.getLineNumber(statementStart) == lineMap.getLineNumber(lineStart - 1)) {
          lineStart--;
        }
        blockMargin = Math.min(blockMargin, statementStart - lineStart);
      }
      // Crop the fragment
      StringBuilder fragment = new StringBuilder();
      for (Iterator<String> sc = new Scanner(block).useDelimiter("\n");sc.hasNext();) {
        String line = sc.next();
        int margin = Math.min(blockMargin, line.length());
        line = line.substring(margin);
        fragment.append(line);
        if (sc.hasNext()) {
          fragment.append('\n');
        }
      }
      return fragment.toString();
    } else {
      return null;
    }
  }

  private static final Pattern P = Pattern.compile("#(\\p{javaJavaIdentifierStart}(?:\\p{javaJavaIdentifierPart})*)(?:\\((.*)\\))?$");

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
        writer.append(body);
        return super.visitText(node, v);
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
        Element resolvedElt = resolveLink(signature);
        if (resolvedElt == null) {
          throw new DocGenException(pkgElt, "Could not resolve " + signature);
        } else if (resolvedElt instanceof PackageElement) {
          PackageElement includedElt = (PackageElement) resolvedElt;
          if (includedElt.getAnnotation(Document.class) == null) {
            process(writer, includedElt);
          } else {
            String link = resolveLinkgPackageDoc((PackageElement) resolvedElt);
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
              link = toTypeLink((TypeElement) resolvedElt);
              break;
            case METHOD:
              link = toMethodLink((ExecutableElement) resolvedElt);
              break;
            case CONSTRUCTOR:
              link = toConstructorLink((ExecutableElement) resolvedElt);
              break;
            case FIELD:
              link = toFieldLink((VariableElement) resolvedElt);
              break;
            default:
              throw new UnsupportedOperationException("Not yet implemented " + resolvedElt + " with kind " + resolvedElt.getKind());
          }
          String label = render(node.getLabel()).trim();
          if (label.length() == 0) {
            label = resolvedElt.getSimpleName().toString();
          }
          writer.append("link:").append(link).append("[`").append(label).append("`]");
        }
        return v;
      }

      private Element resolveLink(String signature) {
        Matcher signatureMatcher = P.matcher(signature);
        if (signatureMatcher.find()) {
          String memberName = signatureMatcher.group(1);
          String typeName = signature.substring(0, signatureMatcher.start());
          TypeElement typeElt = elementUtils.getTypeElement(typeName);
          Predicate<? super Element> memberMatcher;
          if (signatureMatcher.group(2) != null) {
            String t = signatureMatcher.group(2).trim();
            Predicate<ExecutableElement> parametersMatcher;
            if (t.length() == 0) {
              parametersMatcher = exeElt -> exeElt.getParameters().isEmpty();
            } else {
              parametersMatcher = helper.parametersMatcher(pkgTree.getCompilationUnit(), t.split("\\s*,\\s*"));
            }
            memberMatcher = elt -> helper.matchesConstructor(elt, memberName, parametersMatcher) || helper.matchesMethod(elt, memberName, parametersMatcher);
          } else {
            memberMatcher = elt -> helper.matchesConstructor(elt, memberName, exeElt -> true) ||
                helper.matchesMethod(elt, memberName, exeElt -> true) ||
                helper.matchesField(elt, memberName);
          }
          // The order of kinds is important
          for (ElementKind kind : Arrays.asList(ElementKind.FIELD, ElementKind.CONSTRUCTOR, ElementKind.METHOD)) {
            for (Element memberElt : elementUtils.getAllMembers(typeElt)) {
              if(memberElt.getKind() == kind && memberMatcher.test(memberElt)) {
                return memberElt;
              }
            }
          }
          return null;
        } else {
          Element elt = elementUtils.getTypeElement(signature);
          if (elt == null) {
            elt = elementUtils.getPackageElement(signature);
          }
          return elt;
        }
      }
    };
    doc.accept(visitor, null);
    stack.removeLast();
  }

  protected void write(PackageElement docElt, String content) {
    String outputOpt = processingEnv.getOptions().get("docgen.output");
    if (outputOpt != null) {
      File outputDir = new File(outputOpt);
      if (outputDir.exists()) {
        if (outputDir.isDirectory()) {
          write(outputDir, docElt, content);
        } else {
          System.out.println("could not use non dir " + outputDir.getAbsolutePath());
        }
      } else {
        if (outputDir.mkdirs()) {
          write(outputDir, docElt, content);
        } else {
          System.out.println("could not create dir " + outputDir.getAbsolutePath());
        }
      }
    }
  }

  private void write(File dir, PackageElement docElt, String content) {
    try {
      File file = new File(dir, docElt.getQualifiedName().toString() + ".adoc");
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(content);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
