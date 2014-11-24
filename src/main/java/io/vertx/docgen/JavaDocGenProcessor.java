package io.vertx.docgen;

import javax.annotation.processing.SupportedAnnotationTypes;
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
@SupportedAnnotationTypes({"io.vertx.docgen.Document"})
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class JavaDocGenProcessor extends BaseProcessor {

  @Override
  protected void handleGen(PackageElement docElt) {
    StringWriter buffer = new StringWriter();
    process(buffer, docElt);
    write(docElt, buffer.toString());
  }

  @Override
  protected String resolveLinkgPackageDoc(PackageElement elt) {
    return elt.toString() + ".adoc";
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
}
