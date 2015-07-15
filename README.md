# Docgen

[![Build Status](https://vertx.ci.cloudbees.com/buildStatus/icon?job=vert.x3-docgen)](https://vertx.ci.cloudbees.com/view/vert.x-3/job/vert.x3-docgen/)

## Description

Create an Asciidoc document from package Javadoc. It comes as an annotation processor that process the various
package Javadoc and create a single Asciidoc file during the compilation phase.

## Motivation

- The documentation can reference the underlying API doc (Javadoc, Scaladoc, JSdoc, etc…) : when the Asciidoc
content is created the `{@link}` are transformed to a link to the API doc
- The documentation always point to an existing API, i.e the {@link} references validity are checked
and make the compilation fails
- Refactoring friendly, when you rename a method the corresponding {@link} are updated, allowing you
easily spot the affected doc (using git diff or something)

## Features

### Document declaration

`@Document` annotations are processed, each annotated package creates a corresponding asciidoc document. The
annotation can specify an optional `fileName` member otherwise the document file name will be generated using the
annotated element.

### Document modulalization

The `{@link }` Javadoc tag includes or link to the target doc when the target is a package. This can be used to make your
documentation modular and split the various chapters of the document accross the API packages.

When the package is annotated with `@Document` the link renders to a relative file url, otherwise its content
will be literaly included in the document.

### Source inclusion

The `{@link }` Javadoc tag includes the referenced elements when this element is annotated with a `io.vertx.docgen.Source`
annotation (otherwise it will just create a link). The `@Source` annotation can annotate a single method, a class
or a package.

By default the source code is translated to the output language. This feature can be disabled with `@Source
(translate=false)`. When including source code, the _closest_ (class, package, parent package...) `@Source` annotation
 is looked up and the value of the `translate` attribute is used. 
 
When `translate` is set to `false`, it supports the inclusion of Java file (entire file) in the documentation with 
the following syntax:

```
[source, java]
----
{@link org.acme.MyClass}
----
```

### Lang token

The `$lang` token is replaced by the processed language in:
- `docgen.output`
- processed text (@Source is excluded)

The `\$lang` escapes to `$lang`.

### Referencing program elements

The `{@link }` Javadoc tag creates a link to the Javadoc of a program element when the target is
 a type, a field or a method.

## Configuration

The annotation processor can be configured via annotation processing options:

- `docgen.output` : path of the file output dir, the path may contain the token `$lang` that will be subsituted by
the current language being generated (the `docgen.json` name field)
- `docgen.extension` : the file extension to use when file names are generated, defaults to `.adoc`

## Example

See the nested _test_proj_ maven project.

Given the  files:

```
/**
 * = The great project
 *
 * include::{@link test.proj.foofeature}[]
 *
 */
@io.vertx.docgen.Document(fileName = "index.adoc")
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
 * Some code:
 *
 * [source,java]
 * ----
 * {@link test.proj.foofeature.Examples#fooExample}
 * ----
 * <1> get a Foo
 * <2> call {@link test.proj.foofeature.FooApi#myMethod api method}
 *
 * === A sub section
 *
 * {@link test.proj.foofeature.subsection}
 */
@io.vertx.docgen.Document
package test.proj.foofeature;
```

```
/**
 * A literaly included section
 */
package test.proj.foofeature.subsection;
```

Generate the following Asciidoc files:

```
= The great project

include::test.proj.foofeature.adoc[]
```
_test.proj.adoc_

```
== The foo feature.

The link:apidocs/test/proj/foofeature/FooApi.html[`api class`]
The link:apidocs/test/proj/foofeature/FooApi.html#myMethod-java.lang.String-int-java.util.List-java.util.Set-[`api method`]
The link:apidocs/test/proj/foofeature/FooApi.html#myField[`api field`]

* item1
* item 2
* item 3

Some code:

[source,java]
----
FooApi foo = getFoo(); // <1>
List<Boolean> list = new ArrayList<>();
Set<Long> set = new HashSet<>();
foo.myMethod("whatever", 0, list, set); // <2>
----
<1> get a Foo
<2> call link:apidocs/test/proj/foofeature/FooApi.html#myMethod-java.lang.String-int-java.util.List-java.util.Set-[`api method`]

=== A sub section

A literaly included section
```
_test.proj.foofeature.adoc_
