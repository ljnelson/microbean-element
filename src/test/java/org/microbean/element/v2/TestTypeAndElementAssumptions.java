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
package org.microbean.element.v2;

import java.io.IOException;

import java.net.URI;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.tools.SimpleJavaFileObject;

import javax.tools.JavaCompiler.CompilationTask;

import org.junit.jupiter.api.Test;

import static javax.lang.model.SourceVersion.RELEASE_17;

import static javax.tools.ToolProvider.getSystemJavaCompiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestTypeAndElementAssumptions {

  private TestTypeAndElementAssumptions() {
    super();
  }


  /*
   * Instance methods.
   */


  private final void testAssumptions(final ProcessingEnvironment env) {
    final javax.lang.model.util.Elements elements = env.getElementUtils();

    // Here we have an element representing a declaration,
    // i.e. "public interface Comparable<T>".
    final TypeElement comparableElement = elements.getTypeElement("java.lang.Comparable");
    assertEquals(ElementKind.INTERFACE, comparableElement.getKind());

    // The element declares a type, of course.
    final DeclaredType elementType = (DeclaredType)comparableElement.asType();
    assertEquals(TypeKind.DECLARED, elementType.getKind());
    
    // The declared type has one type argument.  The sole type
    // argument is definitionally a TypeVariable.
    final List<? extends TypeMirror> typeArguments = elementType.getTypeArguments();
    assertEquals(1, typeArguments.size());
    final TypeVariable soleTypeArgument = (TypeVariable)typeArguments.get(0);
    assertEquals(TypeKind.TYPEVAR, soleTypeArgument.getKind());

    // The element has a type parameter, which is an element itself.
    final List<? extends Element> typeParameters = comparableElement.getTypeParameters();
    assertEquals(1, typeParameters.size());
    final TypeParameterElement soleTypeParameter = (TypeParameterElement)typeParameters.get(0);
    assertSame(soleTypeArgument.asElement(), soleTypeParameter);

    // The sole type parameter element declares a type, which is the
    // type variable we discussed above.
    assertSame(soleTypeArgument, soleTypeParameter.asType());
    assertTrue(soleTypeParameter.getEnclosedElements().isEmpty());
    assertSame(comparableElement, soleTypeParameter.getEnclosingElement());
    assertSame(comparableElement, soleTypeParameter.getGenericElement());

    // Now let's look at a place where a related type is used.
    final TypeElement stringElement = elements.getTypeElement("java.lang.String");
    final TypeMirror comparableStringType = stringElement.getInterfaces().stream()
      .filter(i -> i.getKind() == TypeKind.DECLARED && i instanceof DeclaredType d && d.asElement().getSimpleName().contentEquals("Comparable"))
      .findFirst()
      .orElseThrow();
    assertNotSame(elementType, comparableStringType);
    final TypeElement comparableStringElement = (TypeElement)((DeclaredType)comparableStringType).asElement();
    assertSame(comparableElement, comparableStringElement);

    // So that's a case where the
    // Comparable<T>-type-declared-by-the-Comparable<T>-element is
    // (clearly) not the same as the
    // Comparable<String>-type-used-by-the-String-element.
    //
    // Important takeaways:
    //
    // * When you have a TypeElement that represents...I don't know
    //   how to talk about this.  When you have a TypeElement that
    //   represents the declaration of a type, its asType() method
    //   will return a DeclaredType.
    //
    // * The DeclaredType so returned will have type arguments.
    //
    // * The type arguments of that DeclaredType will always be
    //   TypeVariables.
    //
    // * Such a TypeVariable will always return the proper
    //   TypeParameterElement from its asElement() method (see below).
    //
    // * The TypeElement we're talking about will always have type
    //   parameters.
    //
    // * For any given TypeParameterElement, its asType() method will
    //   return a TypeVariable, namely a TypeVariable mentioned above.
    
  }



  /*
   * Boilerplate.
   */


  @Test
  final void bootstrap() throws IOException {
    final CompilationTask task = getSystemJavaCompiler()
      .getTask(null,
               null,
               null,
               List.of("-d", System.getProperty("project.build.testOutputDirectory")),
               null,
               List.of(new FrobFile()));
    task.setProcessors(List.of(new Processor()));
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
      super("Frob", "class Frob{}");
    }
  }

  private final class Processor extends AbstractProcessor {

    private Processor() {
      super();
    }

    @Override // AbstractProcessor
    public final SourceVersion getSupportedSourceVersion() {
      return RELEASE_17;
    }

    @Override // AbstractProcessor
    @SuppressWarnings("deprecation")
    public final void init(final ProcessingEnvironment processingEnvironment) {
      TestTypeAndElementAssumptions.this.testAssumptions(processingEnvironment);
    }

    @Override // AbstractProcessor
    public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
      return false;
    }

  }

}
