/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.element;

import java.io.IOException;

import java.net.URI;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.junit.jupiter.api.Test;

import static javax.lang.model.SourceVersion.RELEASE_17;

import static javax.tools.ToolProvider.getSystemJavaCompiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestORama {

  private TestORama() {
    super();
  }

  @Test
  final void testORama() throws IOException {
    final CompilationTask task = getSystemJavaCompiler()
      .getTask(null,
               null,
               null,
               List.of("-d", System.getProperty("project.build.testOutputDirectory")),
               null,
               List.of(new FrobFile(),
                       new AnnotatedTypeVariableCheck(),
                       new Record()));
    task.setProcessors(List.of(new StupidProcessor()));
    assertTrue(task.call());
  }

  private static class StringFile extends SimpleJavaFileObject {

    private final String sourceCode;

    protected StringFile(final String className, final String sourceCode) {
      super(URI.create("string:///" + className.replace('.', '/') + ".java"), SimpleJavaFileObject.Kind.SOURCE);
      this.sourceCode = sourceCode;
    }

    @Override
    public final CharSequence getCharContent(final boolean ignoreEncodingErrors) {
      return this.sourceCode;
    }

  }

  private static final class FrobFile extends StringFile {

    private FrobFile() {
      super("Frob",
            """
            public class Frob implements Comparable<Frob> {
              @Override
              public final int compareTo(final Frob other) {
                return 0;
              }
            }
            """);
    }
  }

  private static final class AnnotatedTypeVariableCheck extends StringFile {

    private AnnotatedTypeVariableCheck() {
      super("AnnotatedTypeVariableCheck",
            """
            class AnnotatedTypeVariableCheck {

              private static String[] strings() {
                return new String[0];
              }

              private static <@Gorp T> T frob() {
                return null;
              }

              @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
              private static @interface Gorp {}

            }
            """);
    }
  }

  private static final class Record extends StringFile {

    private Record() {
      super("Record",
            """
            public record Record(String foo) {
              @Override
              public final String foo() {
                return this.foo;
              }
            }
            """);
    }

  }

  private static final class StupidProcessor extends AbstractProcessor {

    private StupidProcessor() {
      super();
    }

    @Override // AbstractProcessor
    public final SourceVersion getSupportedSourceVersion() {
      return RELEASE_17;
    }

    @Override // AbstractProcessor
    public final void init(final ProcessingEnvironment processingEnvironment) {
      final javax.lang.model.util.Elements elements = processingEnvironment.getElementUtils();


      /*
       * Here's the compiler's version.
       */


      final TypeElement comparableElement = elements.getTypeElement("java.lang.Comparable");
      assertTrue(comparableElement.getSimpleName().contentEquals("Comparable"));

      final PackageElement javaLang = (PackageElement)comparableElement.getEnclosingElement();
      assertTrue(javaLang.getSimpleName().contentEquals("lang"));

      final ModuleElement javaBase = (ModuleElement)javaLang.getEnclosingElement();
      assertTrue(javaBase.getQualifiedName().contentEquals("java.base"));
      assertTrue(javaBase.getSimpleName().contentEquals("base")); // this is stupid
      assertNull(javaBase.getEnclosingElement());

      final DeclaredType comparableType = (DeclaredType)comparableElement.asType();
      assertSame(TypeKind.DECLARED, comparableType.getKind());

      final List<? extends TypeParameterElement> comparableTypeParameters = comparableElement.getTypeParameters();
      assertEquals(1, comparableTypeParameters.size());

      final TypeParameterElement tElement = comparableTypeParameters.get(0);
      assertTrue(tElement.getSimpleName().contentEquals("T"));

      final TypeVariable tType = (TypeVariable)tElement.asType();
      assertSame(tElement, tType.asElement());

      final DeclaredType upperBoundObjectType = (DeclaredType)tType.getUpperBound();
      assertSame(TypeKind.DECLARED, upperBoundObjectType.getKind());

      final TypeElement upperBoundObjectElement = (TypeElement)upperBoundObjectType.asElement();
      assertSame(ElementKind.CLASS, upperBoundObjectElement.getKind());
      assertTrue(upperBoundObjectElement.getQualifiedName().contentEquals("java.lang.Object"));

      // Interesting.  This is probably because strictly speaking the
      // TypeParameterElement named "T" defines a TypeVariable with
      // *no* bounds, and then something in the compiler probably
      // comes along later and sets an *inferred* upper bound
      // DeclaredType backed by the TypeElement named
      // "java.lang.Object".  So the upper bound DeclaredType in this
      // case will not be at the same memory address as the
      // DeclaredType backing the TypeElement manifested by "public
      // class Object", even though both will share the same
      // TypeElement.
      //
      // That's kind of weird.  That means that strictly speaking any
      // given TypeElement may be conceptually affiliated with 1..n
      // DeclaredType instances, despite the 1..1 cardinality implied
      // by javax.lang.model.
      assertNotSame(upperBoundObjectType, upperBoundObjectElement.asType()); // NOTE: surprising

      // So even though the *type* of the upper bound is not the same
      // as the *type* that backs its element, the type that backs its
      // element is the same as the one backing the element manifested
      // by "public class Object".
      assertSame(upperBoundObjectElement, elements.getTypeElement("java.lang.Object"));

      // isSameType() basically returns true for two "boring" declared
      // types (no type arguments, no enclosing type) if their symbols
      // (Elements) are identical. (!)
      assertTrue(processingEnvironment.getTypeUtils().isSameType(upperBoundObjectType, upperBoundObjectElement.asType()));

      // This will return true in lots of cases because it only
      // compares "type stuff", i.e. never asElement() stuff.
      assertTrue(Identity.identical(upperBoundObjectType, upperBoundObjectElement.asType(), true));

      final List<? extends TypeMirror> comparableTypeTypeArguments = comparableType.getTypeArguments();
      assertEquals(1, comparableTypeTypeArguments.size());

      // When you're looking at java.lang.Comparable itself, its sole
      // type argument simply is the TypeVariable type backing its
      // sole type parameter TypeParameterElement.
      assertSame(tType, comparableTypeTypeArguments.get(0));


      /*
       * Let's see if we can replicate it.
       */


      // Let's work inside out.  First the type variable.
      final DefaultTypeVariable defaultTType = DefaultTypeVariable.of();

      // We can't do the TypeParameterElement representing (just) T
      // yet, because of enclosing rules.  Its enclosing element will
      // be a TypeElement representing (just) Comparable<T>.

      // We can't do the TypeElement representing (just) Comparable<T>
      // yet because it needs a backing DeclaredType with defaultTType
      // as its sole type argument.
      final DefaultDeclaredType defaultComparableType = new DefaultDeclaredType(defaultTType);

      // We can't do the TypeElement representing (just) Comparable<T>
      // yet because it needs an enclosing PackageElement.

      // We can't do the PackageElement yet because we need a ModuleElement.
      final DefaultModuleElement defaultJavaBase =
        new DefaultModuleElement(DefaultName.of("java.base"));


      final DefaultPackageElement defaultJavaLang =
        new DefaultPackageElement(DefaultName.of("java.lang"),
                                  DefaultNoType.PACKAGE,
                                  Set.of(),
                                  defaultJavaBase,
                                  null);

      // Now we can do the main Comparable element:
      final DefaultTypeElement defaultComparableElement =
        new DefaultTypeElement(DefaultName.of("java.lang.Comparable"),
                               ElementKind.INTERFACE,
                               defaultComparableType, // here's the backing type
                               DefaultTypeElement.PUBLIC,
                               defaultJavaLang,
                               NestingKind.TOP_LEVEL,
                               null,
                               List.of(),
                               List.of(),
                               null);

      // ...and now we can do a TypeParameterElement:
      final DefaultTypeParameterElement defaultTElement =
        new DefaultTypeParameterElement(DefaultName.of("T"),
                                        defaultTType, // here's the backing type
                                        Set.of(), // no modifiers
                                        defaultComparableElement, // here's the enclosing TypeElement
                                        null);

      assertEquals(1, defaultComparableElement.getTypeParameters().size());
      assertSame(defaultTElement, defaultComparableElement.getTypeParameters().get(0));

      // Let's see if Equality thinks this is good:
      assertTrue(Equality.equals(tType, defaultTType, true));
      assertTrue(Equality.equals(tElement, defaultTElement, true));


      /*
       * On to Frob.
       */


      // The compiler's version:

      final TypeElement frobElement = elements.getTypeElement("Frob");
      assertNotNull(frobElement);
      assertTrue(frobElement.getTypeParameters().isEmpty());

      final DeclaredType frobType = (DeclaredType)frobElement.asType();
      assertSame(TypeKind.DECLARED, frobType.getKind());
      assertSame(frobElement, frobType.asElement());
      assertTrue(frobType.getTypeArguments().isEmpty());

      final List<? extends TypeMirror> frobInterfaces = frobElement.getInterfaces();
      assertEquals(1, frobInterfaces.size());

      // Comparable<Frob> != Comparable<T>
      final DeclaredType comparableFrobType = (DeclaredType)frobInterfaces.get(0);
      assertNotSame(comparableType, comparableFrobType);

      // When you're looking at public class Frob implements
      // Comparable<Frob>, the sole type argument in Comparable<Frob>
      // is the DeclaredType defined by the Frob TypeElement.
      assertSame(frobType, comparableFrobType.getTypeArguments().get(0));

      // Our version:

      // First, let's do the very simple Frob type (remember,
      // TypeMirrors don't have supertypes, even though in the JLS
      // types have supertypes):
      final DefaultDeclaredType defaultFrobType = new DefaultDeclaredType();

      // Now we can do a TypeMirror representing Comparable<Frob>:
      final DefaultDeclaredType defaultComparableFrobType = new DefaultDeclaredType(frobType);

      final DefaultPackageElement unnamedPackage =
        new DefaultPackageElement(DefaultName.of(),
                                  DefaultNoType.PACKAGE,
                                  Set.of(),
                                  null,
                                  null);

      final DefaultTypeElement defaultFrobElement =
        new DefaultTypeElement(DefaultName.of("Frob"),
                               ElementKind.CLASS,
                               defaultFrobType,
                               DefaultTypeElement.PUBLIC,
                               unnamedPackage,
                               NestingKind.TOP_LEVEL,
                               null, // no superclass,
                               List.of(), // permitted subclass types
                               List.of(defaultComparableFrobType), // interface types
                               null);

      assertTrue(Equality.equals(frobElement, defaultFrobElement, true));

      // What's our module, incidentally?
      final DefaultModuleElement orgMicrobeanElement = DefaultModuleElement.of(this.getClass().getModule());
      System.out.println(orgMicrobeanElement.getQualifiedName());
      System.out.println("*** module: " + this.getClass().getModule());

      final TypeElement annotatedTypeVariableCheck = elements.getTypeElement("AnnotatedTypeVariableCheck");
      assertNotNull(annotatedTypeVariableCheck);
      for (final Element e : annotatedTypeVariableCheck.getEnclosedElements()) {
        if (e.getSimpleName().contentEquals("frob")) {
          final ExecutableElement frobMethod = (ExecutableElement)e;
          final ExecutableType t = (ExecutableType)frobMethod.asType();
          final List<? extends TypeVariable> tvs = t.getTypeVariables();
          for (final TypeVariable tv : tvs) {
            System.out.println("*** frob type variable: " + tv);
            System.out.println("    frob type variable annotations: " + tv.getAnnotationMirrors());
          }
        } else if (e.getSimpleName().contentEquals("strings")) {
          final ExecutableElement stringsMethod = (ExecutableElement)e;
          final ExecutableType t = (ExecutableType)stringsMethod.asType();
          final TypeMirror returnType = t.getReturnType();
          System.out.println("*** strings return type kind: " + returnType.getKind());
        }
      }

      final TypeElement record = elements.getTypeElement("Record");
      for (final Element e : record.getEnclosedElements()) {
        if (e.getKind() == ElementKind.RECORD_COMPONENT) {
          final RecordComponentElement rce = (RecordComponentElement)e;
          final TypeMirror rceType = rce.asType();
          System.out.println("*** rceType: " + rceType);
          System.out.println("    modifiers: " + rce.getModifiers());
        }
      }



    }

    @Override // Processor
    public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
      return false;
    }

  }

}
