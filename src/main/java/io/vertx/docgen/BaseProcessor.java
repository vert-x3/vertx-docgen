package io.vertx.docgen;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import io.vertx.codegen.GenException;
import io.vertx.codegen.annotations.GenModule;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
      roundEnv.getElementsAnnotatedWith(GenModule.class).forEach(elt -> {
        StringBuilder buffer = new StringBuilder();
        PackageElement pkgElt = (PackageElement) elt;
        try {
          process(buffer, pkgElt);
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
        handleGen(pkgElt, buffer.toString());
      });
    }
    return false;
  }

  protected abstract void handleGen(PackageElement moduleElt, String content);

  protected abstract String resolveLinkTypeDoc(TypeElement elt);

  protected abstract String resolveLinkConstructorDoc(ExecutableElement elt);

  protected abstract String resolveLinkMethodDoc(ExecutableElement elt);

  protected abstract String resolveLinkFieldDoc(VariableElement elt);

  private static final Pattern P = Pattern.compile("#(\\p{javaJavaIdentifierStart}(?:\\p{javaJavaIdentifierPart})*)(?:\\((.*)\\))?$");

  private final LinkedList<PackageElement> stack = new LinkedList<>();

  private void process(StringBuilder buffer, PackageElement pkgElt) {

    for (PackageElement stackElt : stack) {
      if (pkgElt.getQualifiedName().equals(stackElt.getQualifiedName())) {
        throw new GenException(stack.peekLast(), "Circular include");
      }
    }
    stack.addLast(pkgElt);

    TreePath tp = docTrees.getPath(pkgElt);
    DocCommentTree doc = docTrees.getDocCommentTree(tp);
    DocTreeVisitor<Void, Void> visitor = new DocTreeScanner<Void, Void>() {

      @Override
      public Void visitText(TextTree node, Void v) {
        String body = node.getBody();
        buffer.append(body);
        return super.visitText(node, v);
      }

      @Override
      public Void visitLink(LinkTree node, Void v) {
        String signature = node.getReference().getSignature();
        Element resolvedElt = resolveLink(signature);
        if (resolvedElt == null) {
          throw new DocGenException(pkgElt, "Could not resolve " + signature);
        } else if (resolvedElt instanceof PackageElement) {
          PackageElement includedElt = (PackageElement) resolvedElt;
          process(buffer, includedElt);
        } else {
          String link;
          switch (resolvedElt.getKind()) {
            case CLASS:
            case INTERFACE:
              link = resolveLinkTypeDoc((TypeElement) resolvedElt);
              break;
            case METHOD:
              link = resolveLinkMethodDoc((ExecutableElement) resolvedElt);
              break;
            case CONSTRUCTOR:
              link = resolveLinkConstructorDoc((ExecutableElement) resolvedElt);
              break;
            case FIELD:
              link = resolveLinkFieldDoc((VariableElement) resolvedElt);
              break;
            default:
              throw new UnsupportedOperationException("Not yet implemented " + resolvedElt + " with kind " + resolvedElt.getKind());
          }
          String label = render(node.getLabel()).trim();
          if (label.length() == 0) {
            label = resolvedElt.getSimpleName().toString();
          }
          buffer.append("link:").append(link).append("[`").append(label).append("`]");
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
              parametersMatcher = helper.parametersMatcher(tp.getCompilationUnit(), t.split("\\s*,\\s*"));
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
}
