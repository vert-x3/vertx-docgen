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
    anchor += parameterTypes.get(i).toString();
  }
  anchor += '-';
  return link + anchor;
}