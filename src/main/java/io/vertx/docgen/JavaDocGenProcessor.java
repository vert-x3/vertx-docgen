package io.vertx.docgen;

import com.sun.tools.javac.code.Type;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.io.StringWriter;
import java.util.List;

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
  protected String getName() {
    return "java";
  }

  @Override
  protected String resolveTypeLink(TypeElement elt, Coordinate coordinate) {
    return "../../apidocs/" + elt.getQualifiedName().toString().replace('.', '/') + ".html";
  }

  @Override
  protected String resolveConstructorLink(ExecutableElement elt, Coordinate coordinate) {
    return toExecutableLink(elt, elt.getEnclosingElement().getSimpleName().toString());
  }

  @Override
  protected String resolveMethodLink(ExecutableElement elt, Coordinate coordinate) {
    return toExecutableLink(elt, elt.getSimpleName().toString());
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
  protected String resolveFieldLink(VariableElement elt, Coordinate coordinate) {
    TypeElement typeElt = (TypeElement) elt.getEnclosingElement();
    String link = resolveTypeLink(typeElt, null);
    return link + "#" + elt.getSimpleName();
  }

  protected String renderSource(ExecutableElement elt, String source) {
    // Just use the default rendering process.
    return defaultRenderSource(elt, source);
  }
}
