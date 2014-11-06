package io.vertx.docgen;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import io.vertx.codegen.annotations.GenModule;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@SupportedAnnotationTypes({
    "io.vertx.codegen.annotations.GenModule"
})
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class DocGenProcessor extends AbstractProcessor {

  private DocTrees docTrees;
  Map<String, String> results = new HashMap<>();
  Map<String, String> failures = new HashMap<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    docTrees = DocTrees.instance(processingEnv);
  }

  public String getDoc(String name) {
    return results.get(name);
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
        results.put(pkgElt.getQualifiedName().toString(), buffer.toString());
      });
    }
    return false;
  }

  protected String resolveLinkTypeDoc(TypeElement elt) {
    return "abc";
  }

  protected String resolveLinkMethodDoc(ExecutableElement elt) {
    return "def";
  }

  private static final Pattern P = Pattern.compile("#(\\p{javaJavaIdentifierStart}(?:\\p{javaJavaIdentifierPart})*)(?:\\((.*)\\))?$");

  private void process(StringBuilder buffer, PackageElement pkgElt) {
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
          String name;
          if (resolvedElt instanceof TypeElement) {
            link = resolveLinkTypeDoc((TypeElement) resolvedElt);
            name = resolvedElt.getSimpleName().toString();
          } else {
            link = resolveLinkMethodDoc((ExecutableElement) resolvedElt);
            name = resolvedElt.getSimpleName().toString();
          }
          buffer.append(link).append("[`").append(name).append("`]");
        }
        return super.visitLink(node, v);
      }

      private TypeMirror resolveSignatureType(String name) {
        if (name.equals("boolean")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
        } else if (name.equals("byte")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.BYTE);
        } else if (name.equals("short")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.SHORT);
        } else if (name.equals("int")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.INT);
        } else if (name.equals("long")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.LONG);
        } else if (name.equals("float")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.FLOAT);
        } else if (name.equals("double")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.DOUBLE);
        } else if (name.equals("char")) {
          return processingEnv.getTypeUtils().getPrimitiveType(TypeKind.CHAR);
        } else if (name.endsWith("[]")) {
          TypeMirror componentType = resolveSignatureType(name.substring(0, name.length() - 2));
          if (componentType != null) {
            return processingEnv.getTypeUtils().getArrayType(componentType);
          }
        } else {
          TypeElement typeElt;
          int index = name.indexOf('.');
          if (index >= 0) {
            typeElt = processingEnv.getElementUtils().getTypeElement(name);
          } else {
            typeElt = null;
            for (ImportTree importTree : tp.getCompilationUnit().getImports()) {
              Tree identifier = importTree.getQualifiedIdentifier();
              if (identifier instanceof MemberSelectTree) {
                MemberSelectTree memberSelect = (MemberSelectTree) identifier;
                if (name.equals(memberSelect.getIdentifier().toString())) {
                  typeElt = processingEnv.getElementUtils().getTypeElement(memberSelect.getExpression() + "." + memberSelect.getIdentifier());
                  if (typeElt != null) {
                    break;
                  }
                }
              } else {
                throw new UnsupportedOperationException("not implemented");
              }
            }
            if (typeElt == null) {
              typeElt = processingEnv.getElementUtils().getTypeElement("java.lang." + name);
            }
          }
          if (typeElt != null) {
            return processingEnv.getTypeUtils().erasure(typeElt.asType());
          }
        }
        return null;
      }

      private Element resolveLink(String signature) {
        Matcher m = P.matcher(signature);
        String elementName;
        if (m.find()) {
          String methodName = m.group(1);
          elementName = signature.substring(0, m.start());
          TypeElement targetElt = processingEnv.getElementUtils().getTypeElement(elementName);
          Predicate<ExecutableElement> matcher;
          if (m.group(2) != null) {
            String t = m.group(2).trim();
            if (t.length() == 0) {
              matcher = methodElt -> methodElt.getSimpleName().toString().equals(methodName) && methodElt.getParameters().isEmpty();
            } else {
              String[] types = t.split("\\s*,\\s*");
              matcher = methodElt -> {
                if (methodElt.getSimpleName().toString().equals(methodName) && types.length == methodElt.getParameters().size()) {
                  TypeMirror tm2  = methodElt.asType();
                  ExecutableType tm3  = (ExecutableType) processingEnv.getTypeUtils().erasure(tm2);
                  for (int i = 0;i < types.length;i++) {
                    TypeMirror t1 = tm3.getParameterTypes().get(i);
                    TypeMirror t2 = resolveSignatureType(types[i]);
                    if (t2 == null || !processingEnv.getTypeUtils().isSameType(t2, t1)) {
                      return false;
                    }
                  }
                  return true;
                } else {
                  return false;
                }
              };
            }
          } else {
            matcher = methodElt -> methodElt.getSimpleName().toString().equals(methodName);
          }
          for (Element memberElt : processingEnv.getElementUtils().getAllMembers(targetElt)) {
            switch (memberElt.getKind()) {
              case METHOD:
                ExecutableElement methodElt = (ExecutableElement) memberElt;
                if (matcher.test(methodElt)) {
                  return methodElt;
                }
                break;
            }
          }
          return null;
        } else {
          Element elt = processingEnv.getElementUtils().getTypeElement(signature);
          if (elt == null) {
            elt = processingEnv.getElementUtils().getPackageElement(signature);
          }
          return elt;
        }
      }
    };
    doc.accept(visitor, null);
  }
}
