function renderSource(elt, source) {
  return "todo";
}

function toTypeLink(elt) {
  return "apidocs/" + elt.getQualifiedName().toString().replace(".", "/") + ".html";
}

function toMethodLink(elt) {
  return toExecutableLink(elt, elt.getSimpleName().toString());
}

function toConstructorLink(elt) {
  return toExecutableLink(elt, elt.getEnclosingElement().getSimpleName().toString());
}

function toFieldLink(elt) {
  var typeElt = elt.getEnclosingElement();
  var link = toTypeLink(typeElt);
  return link + '#' + elt.getSimpleName().toString();
}

function resolveLabel(elt, label) {
  return label;
}

function toExecutableLink(elt, name) {
  var typeElt = elt.getEnclosingElement();
  var link = toTypeLink(typeElt);
  var anchor = '#' + name + "-";
  var type = elt.asType();
  var methodType  = typeUtils.erasure(type);
  var parameterTypes = methodType.getParameterTypes();
  for (var i = 0;i < parameterTypes.size();i++) {
    if (i > 0) {
      anchor += '-';
    }
    var typeOfParameter = parameterTypes.get(i);
    // If the parameter is annotated, we should ignore the annotation to generate the right link.
    // (annotations are not part of the javadoc links).
    if (typeOfParameter instanceof com.sun.tools.javac.code.Type$AnnotatedType) {
      anchor += typeOfParameter.unannotatedType().toString();
    } else {
      anchor += typeOfParameter.toString();
    }
  }
  anchor += '-';
  return link + anchor;
}