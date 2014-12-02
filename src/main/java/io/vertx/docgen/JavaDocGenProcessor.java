package io.vertx.docgen;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreePath;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Processor specialized for Java.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JavaDocGenProcessor extends BaseProcessor {

  @Override
  protected void handleGen(PackageElement docElt) {
    StringWriter buffer = new StringWriter();
    process(buffer, docElt);
    write(docElt, buffer.toString());
  }

  @Override
  protected String toTypeLink(TypeElement elt) {
    return "apidocs/" + elt.getQualifiedName().toString().replace('.', '/') + ".html";
  }

  @Override
  protected String toConstructorLink(ExecutableElement elt) {
    return toExecutableLink(elt, elt.getEnclosingElement().getSimpleName().toString());
  }

  @Override
  protected String toMethodLink(ExecutableElement elt) {
    return toExecutableLink(elt, elt.getSimpleName().toString());
  }

  private String toExecutableLink(ExecutableElement elt, String name) {
    TypeElement typeElt = (TypeElement) elt.getEnclosingElement();
    String link = toTypeLink(typeElt);
    StringBuilder anchor = new StringBuilder("#");
    anchor.append(name).append('-');
    TypeMirror type  = elt.asType();
    ExecutableType methodType  = (ExecutableType) processingEnv.getTypeUtils().erasure(type);
    List<? extends TypeMirror> parameterTypes = methodType.getParameterTypes();
    for (int i = 0;i < parameterTypes.size();i++) {
      if (i > 0) {
        anchor.append('-');
      }
      anchor.append(parameterTypes.get(i));
    }
    anchor.append('-');
    return link + anchor;
  }

  @Override
  protected String toFieldLink(VariableElement elt) {
    TypeElement typeElt = (TypeElement) elt.getEnclosingElement();
    String link = toTypeLink(typeElt);
    return link + "#" + elt.getSimpleName();
  }

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
}
