package io.vertx.docgen;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JavaDocGenerator implements DocGenerator {

  protected DocTrees docTrees;
  protected ProcessingEnvironment processingEnv;

  @Override
  public void init(ProcessingEnvironment env) {
    docTrees = DocTrees.instance(env);
    processingEnv = env;
  }

  @Override
  public String getName() {
    return "java";
  }

  @Override
  public String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
    return "../../apidocs/" + elt.getQualifiedName().toString().replace('.', '/') + ".html";
  }

  @Override
  public String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
    return toExecutableLink(elt, elt.getEnclosingElement().getSimpleName().toString());
  }

  @Override
  public String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
    return toExecutableLink(elt, elt.getSimpleName().toString());
  }

  @Override
  public String resolveLabel(Element elt, String defaultLabel) {
    return defaultLabel;
  }

  private String toExecutableLink(ExecutableElement elt, String name) {
    TypeElement typeElt = (TypeElement) elt.getEnclosingElement();
    String link = resolveTypeLink(typeElt, null);
    StringBuilder anchor = new StringBuilder("#");
    anchor.append(name).append('-');
    TypeMirror type = elt.asType();
    ExecutableType methodType = (ExecutableType) processingEnv.getTypeUtils().erasure(type);
    List<? extends TypeMirror> parameterTypes = methodType.getParameterTypes();
    for (int i = 0; i < parameterTypes.size(); i++) {
      if (i > 0) {
        anchor.append('-');
      }
      // We need to check whether or not the parameter is annotated. In this case, we must use the unannotated type.
      TypeMirror typeOfParameter = parameterTypes.get(i);
      if (typeOfParameter instanceof Type && ((Type) typeOfParameter).isAnnotated()) {
        anchor.append(((Type) typeOfParameter).unannotatedType().toString());
      } else {
        anchor.append(typeOfParameter.toString());
      }
    }
    anchor.append('-');
    return link + anchor;
  }

  @Override
  public String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
    TypeElement typeElt = (TypeElement) elt.getEnclosingElement();
    String link = resolveTypeLink(typeElt, null);
    return link + "#" + elt.getSimpleName();
  }

  /**
   * Render the source fragment for the Java language. Java being the pivot language, we consider this method as the
   * _default_ behavior. This method is final as it must not be overridden by any extension.
   *
   * @param elt    the element
   * @param source the source
   * @return the fragment
   */
  @Override
  public String renderSource(ExecutableElement elt, String source) {
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
      for (Iterator<String> sc = new Scanner(block).useDelimiter("\n"); sc.hasNext(); ) {
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
}
