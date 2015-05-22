package io.vertx.docgen;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class Helper {

  static final Pattern LANG_PATTERN = Pattern.compile("(\\\\)?\\$lang");

  final Types typeUtils;
  final Elements elementUtils;
  final DocTrees docTrees;

  public Helper(ProcessingEnvironment env) {
    typeUtils = env.getTypeUtils();
    elementUtils = env.getElementUtils();
    docTrees = DocTrees.instance(env);
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

  boolean matchesFieldOrEnumConstant(Element elt, String memberName) {
    ElementKind kind = elt.getKind();
    return (kind == ElementKind.FIELD || kind == ElementKind.ENUM_CONSTANT) && elt.getSimpleName().toString().equals(memberName);
  }

  private static final Pattern P = Pattern.compile("#(\\p{javaJavaIdentifierStart}(?:\\p{javaJavaIdentifierPart})*)(?:\\((.*)\\))?$");

  public Element resolveLink(String signature) {
    Matcher signatureMatcher = P.matcher(signature);
    if (signatureMatcher.find()) {
      String memberName = signatureMatcher.group(1);
      String typeName = signature.substring(0, signatureMatcher.start());
      TypeElement typeElt = elementUtils.getTypeElement(typeName);
      if (typeElt == null) {
        return null;
      }
      Predicate<? super Element> memberMatcher;
      if (signatureMatcher.group(2) != null) {
        String t = signatureMatcher.group(2).trim();
        Predicate<ExecutableElement> parametersMatcher;
        if (t.length() == 0) {
          parametersMatcher = exeElt -> exeElt.getParameters().isEmpty();
        } else {
          parametersMatcher = parametersMatcher(t.split("\\s*,\\s*"));
        }
        memberMatcher = elt -> matchesConstructor(elt, memberName, parametersMatcher) || matchesMethod(elt, memberName, parametersMatcher);
      } else {
        memberMatcher = elt -> matchesConstructor(elt, memberName, exeElt -> true) ||
            matchesMethod(elt, memberName, exeElt -> true) ||
            matchesFieldOrEnumConstant(elt, memberName);
      }
      // The order of kinds is important
      for (ElementKind kind : Arrays.asList(ElementKind.FIELD, ElementKind.ENUM_CONSTANT, ElementKind.CONSTRUCTOR, ElementKind.METHOD)) {
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

  /**
   * Return a matcher for parameters, given the parameter type signature of an executable element. The parameter signature
   * is a list of parameter types formatted as a signature, i.e all types are raw, or primitive, or arrays. Unqualified
   * types are resolved against the import of the specified {@code compilationUnitTree} argument.
   *
   * @param parameterSignature the parameter type names
   * @return the matcher
   */
  Predicate<ExecutableElement> parametersMatcher(String[] parameterSignature) {
    return exeElt -> {
      if (exeElt.getParameters().size() == parameterSignature.length) {
        TypeMirror tm2  = exeElt.asType();
        ExecutableType tm3  = (ExecutableType) typeUtils.erasure(tm2);
        for (int j = 0;j < parameterSignature.length;j++) {
          String t1 = tm3.getParameterTypes().get(j).toString();
          String t2 = parameterSignature[j];
          if (t2.indexOf('.') == -1) {
            t1 = t1.substring(t1.lastIndexOf('.') + 1);
          }
          if (!t1.equals(t2)) {
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
    return elt.getAnnotation(Source.class) != null || elt.getEnclosingElement() != null && isExample(elt.getEnclosingElement());
  }

  /**
   * Read the source code of the provided element, this returns the source of the entire related compilation unit.
   *
   * @param elt the element to load
   * @return the source
   */
  String readSource(Element elt) {
    CompilationUnitTree unit = docTrees.getPath(elt).getCompilationUnit();
    StringBuilder source = new StringBuilder();
    try(Reader reader = unit.getSourceFile().openReader(true)) {
      char[] buffer = new char[256];
      while (true) {
        int len = reader.read(buffer);
        if (len == -1) {
          break;
        }
        source.append(buffer, 0, len);
      }
      return source.toString();
    } catch (IOException e) {
      throw new DocGenException(elt, "Could not read source code of element " + elt);
    }
  }
}
