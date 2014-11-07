# Docgen

## Description

Create an Asciidoc document from package Javadoc. It comes as an annotation processor that process the various
package Javadoc and create a single Asciidoc file during the compilation phase.

## Features

### Doc inclusion

The `{@link }` Javadoc tag includes the target doc when the target is a package.

### Program references

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