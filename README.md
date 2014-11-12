# Docgen

## Description

Create an Asciidoc document from package Javadoc. It comes as an annotation processor that process the various
package Javadoc and create a single Asciidoc file during the compilation phase.

## Features

### Document modulalization

The `{@link }` Javadoc tag includes the target doc when the target is a package. This can be used to make your
documentatin modular and split the various chapters of the document accross the API packages.

### Source inclusion

The `{@link }` Javadoc tag includes the referenced elements when this element is annotated with a `io.vertx.docgen.Example`
annotation (otherwise it will just create a link). This can be used for examples:

```
@Example
public class Examples {

  public void myExample() {
    MyApi api = getApi(); // <1>
    List<Boolean> list = new ArrayList<>();
    Set<Long> set = new HashSet<>();
    api.myMethod("whatever", 0, list, set); // <2>
  }

}
```

It can be then included:

```
[source,java]
----
{@link mypackage.Examples#myExample}
----
<1> get a MyApi
<2> call {@link mypackage.MyApi#myMethod api method}
```

Producing:

```
[source,java]
----
MyApi api = getApi(); // <1>
List<Boolean> list = new ArrayList<>();
Set<Long> set = new HashSet<>();
api.myMethod("whatever", 0, list, set); // <2>
----
<1> get a MyApi
<2> call link:apidocs/myproj/MyApi.html#myMethod-java.lang.String-int-java.util.List-java.util.Set-[`api method`]
```

### Referencing program elements

The `{@link }` Javadoc tag creates a link to the Javadoc of a program element when the target is
 a type, a field or a method.

## Example

See the nested _test_proj_ maven project.

Given the two files:

```
/**
 * = The great project
 *
 * {@link test.proj.foofeature}
 *
 */
package test.proj;
```

```
/**
 * == The foo feature.
 *
 * The {@link test.proj.foofeature.FooApi api class}
 * The {@link test.proj.foofeature.FooApi#myMethod api method}
 * The {@link test.proj.foofeature.FooApi#myField api field}
 *
 * * item1
 * * item 2
 * * item 3
 *
 */
package test.proj.foofeature;
```

Generate the following Asciidoc file:

```
= The great project

== The foo feature.

The link:apidocs/test/proj/foofeature/FooApi.html[`api class`]
The link:apidocs/test/proj/foofeature/FooApi.html#myMethod-java.lang.String-int-java.util.List-java.util.Set-[`api method`]
The link:apidocs/test/proj/foofeature/FooApi.html#myField[`api field`]

* item1
* item 2
* item 3
```