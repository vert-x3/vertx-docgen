package io.vertx.docgen;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class Helper {

  final Types typeUtils;
  final Elements elementUtils;

  public Helper(ProcessingEnvironment env) {
    typeUtils = env.getTypeUtils();
    elementUtils = env.getElementUtils();
  }

  TypeMirror resolveSignatureType(CompilationUnitTree compilationUnit, String name) {
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
      TypeMirror componentType = resolveSignatureType(compilationUnit, name.substring(0, name.length() - 2));
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
        for (ImportTree importTree : compilationUnit.getImports()) {
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

  boolean matchesConstructor(Element elt, String memberName, Predicate<ExecutableElement> parametersMatcher) {
    if (elt.getKind() == ElementKind.CONSTRUCTOR) {
      ExecutableElement constructorElt = (ExecutableElement) elt;
      TypeElement typeElt = (TypeElement) constructorElt.getEnclosingElement();
      return typeElt.getSimpleName().toString().equals(memberName) && parametersMatcher.test(constructorElt);
    }
    return false;
  }

  boolean matchesMethod(Element elt, String memberName, Predicate<ExecutableElement> parametersMatcher) {
    if (elt.getKind() == ElementKind.METHOD) {
      ExecutableElement methodElt = (ExecutableElement) elt;
      return methodElt.getSimpleName().toString().equals(memberName) && parametersMatcher.test(methodElt);
    }
    return false;
  }

  boolean matchesField(Element elt, String memberName) {
    return elt.getKind() == ElementKind.FIELD && elt.getSimpleName().toString().equals(memberName);
  }

  /**
   * Return a matcher for parameters, given the parameter type signature of an executable element. The parameter signature
   * is a list of parameter types formatted as a signature, i.e all types are raw, or primitive, or arrays. Unqualified
   * types are resolved against the import of the specified {@code compilationUnitTree} argument.
   *
   * @param compilationUnitTree the compilation unit
   * @param parameterSignature the parameter type names
   * @return the matcher
   */
  Predicate<ExecutableElement> parametersMatcher(CompilationUnitTree compilationUnitTree, String[] parameterSignature) {
    TypeMirror[] types = Stream.of(parameterSignature).map(name -> resolveSignatureType(compilationUnitTree, name)).toArray(TypeMirror[]::new);
    return exeElt -> {
      if (exeElt.getParameters().size() == types.length) {
        TypeMirror tm2  = exeElt.asType();
        ExecutableType tm3  = (ExecutableType) typeUtils.erasure(tm2);
        for (int j = 0;j < types.length;j++) {
          TypeMirror t1 = tm3.getParameterTypes().get(j);
          TypeMirror t2 = types[j];
          if (t2 == null || !typeUtils.isSameType(t2, t1)) {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    };
  }

  /**
   * Check the element is an example or not.
   *
   * @param elt the elt to check
   * @return true when the checked element is an example
   */
  boolean isExample(Element elt) {
    return elt.getAnnotation(Example.class) != null || elt.getEnclosingElement() != null && isExample(elt.getEnclosingElement());
  }
}
