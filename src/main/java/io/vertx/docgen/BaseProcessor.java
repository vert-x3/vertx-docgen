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
import io.vertx.codegen.GenException;
import io.vertx.codegen.annotations.GenModule;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
import java.util.stream.Stream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class BaseProcessor extends AbstractProcessor {

  private DocTrees docTrees;
  private Types typeUtils;
  private Elements elementUtils;
  Map<String, String> failures = new HashMap<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    docTrees = DocTrees.instance(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
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

      private TypeMirror resolveSignatureType(String name) {
        if (name.equals("boolean")) {
          return typeUtils.getPrimitiveType(TypeKind.BOOLEAN);
        } else if (name.equals("byte")) {
          return typeUtils.getPrimitiveType(TypeKind.BYTE);
        } else if (name.equals("short")) {
          return typeUtils.getPrimitiveType(TypeKind.SHORT);
        } else if (name.equals("int")) {
          return typeUtils.getPrimitiveType(TypeKind.INT);
        } else if (name.equals("long")) {
          return typeUtils.getPrimitiveType(TypeKind.LONG);
        } else if (name.equals("float")) {
          return typeUtils.getPrimitiveType(TypeKind.FLOAT);
        } else if (name.equals("double")) {
          return typeUtils.getPrimitiveType(TypeKind.DOUBLE);
        } else if (name.equals("char")) {
          return typeUtils.getPrimitiveType(TypeKind.CHAR);
        } else if (name.endsWith("[]")) {
          TypeMirror componentType = resolveSignatureType(name.substring(0, name.length() - 2));
          if (componentType != null) {
            return typeUtils.getArrayType(componentType);
          }
        } else {
          TypeElement typeElt;
          int index = name.indexOf('.');
          if (index >= 0) {
            typeElt = elementUtils.getTypeElement(name);
          } else {
            typeElt = null;
            for (ImportTree importTree : tp.getCompilationUnit().getImports()) {
              Tree identifier = importTree.getQualifiedIdentifier();
              if (identifier instanceof MemberSelectTree) {
                MemberSelectTree memberSelect = (MemberSelectTree) identifier;
                if (name.equals(memberSelect.getIdentifier().toString())) {
                  typeElt = elementUtils.getTypeElement(memberSelect.getExpression() + "." + memberSelect.getIdentifier());
                  if (typeElt != null) {
                    break;
                  }
                }
              } else {
                throw new UnsupportedOperationException("not implemented");
              }
            }
            if (typeElt == null) {
              typeElt = elementUtils.getTypeElement("java.lang." + name);
            }
          }
          if (typeElt != null) {
            return typeUtils.erasure(typeElt.asType());
          }
        }
        return null;
      }

      private Element resolveLink(String signature) {
        Matcher signatureMatcher = P.matcher(signature);
        if (signatureMatcher.find()) {
          String memberName = signatureMatcher.group(1);
          String typeName = signature.substring(0, signatureMatcher.start());
          TypeElement typeElt = elementUtils.getTypeElement(typeName);
          Predicate<Element> memberMatcher;
          if (signatureMatcher.group(2) != null) {
            String t = signatureMatcher.group(2).trim();
            if (t.length() == 0) {
              memberMatcher = elt -> {
                if (elt.getKind() == ElementKind.CONSTRUCTOR) {
                  ExecutableElement methodElt = (ExecutableElement) elt;
                  return typeElt.getSimpleName().toString().equals(memberName) && methodElt.getParameters().isEmpty();
                }
                if (elt.getKind() == ElementKind.METHOD) {
                  ExecutableElement methodElt = (ExecutableElement) elt;
                  return methodElt.getSimpleName().toString().equals(memberName) && methodElt.getParameters().isEmpty();
                }
                return false;
              };
            } else {
              TypeMirror[] types = Stream.of(t.split("\\s*,\\s*")).map(this::resolveSignatureType).toArray(TypeMirror[]::new);
              memberMatcher = elt -> {
                if (elt.getKind() == ElementKind.FIELD) {
                  VariableElement variableElt = (VariableElement) elt;
                  if (variableElt.getSimpleName().toString().equals(memberName)) {
                    return true;
                  }
                }
                ExecutableElement exeElt = (ExecutableElement) elt;
                Name[] names = {typeElt.getSimpleName(),exeElt.getSimpleName()};
                ElementKind[] kinds = {ElementKind.CONSTRUCTOR,ElementKind.METHOD};
                next:
                for (int i = 0;i < names.length;i++) {
                  if (exeElt.getKind() == kinds[i] && names[i].toString().equals(memberName) && types.length == exeElt.getParameters().size()) {
                    TypeMirror tm2  = exeElt.asType();
                    ExecutableType tm3  = (ExecutableType) typeUtils.erasure(tm2);
                    for (int j = 0;j < types.length;j++) {
                      TypeMirror t1 = tm3.getParameterTypes().get(j);
                      TypeMirror t2 = types[j];
                      if (t2 == null || !typeUtils.isSameType(t2, t1)) {
                        continue next;
                      }
                    }
                    return true;
                  }
                }
                return false;
              };
            }
          } else {
            memberMatcher = elt -> {
              if (elt.getKind() == ElementKind.CONSTRUCTOR && typeElt.getSimpleName().toString().equals(memberName)) {
                return true;
              }
              if ((elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD) && elt.getSimpleName().toString().equals(memberName)) {
                return true;
              }
              return false;
            };
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
