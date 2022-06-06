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

import java.io.Writer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.util.Elements;

public final class DefaultElements implements Elements {

  private DefaultElements() {
    super();
  }

  @Override // Elements
  public final List<? extends AnnotationMirror> getAllAnnotationMirrors(final Element e) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final List<? extends Element> getAllMembers(final TypeElement typeElement) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final Name getBinaryName(final TypeElement typeElement) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final String getConstantExpression(final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final String getDocComment(final Element e) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final Name getName(final CharSequence cs) {
    return DefaultName.of(cs);
  }

  @Override // Elements
  public final PackageElement getPackageElement(final CharSequence cs) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final PackageElement getPackageOf(final Element e) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final TypeElement getTypeElement(final CharSequence name) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final boolean hides(final Element hider, final Element hidden) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final boolean isDeprecated(final Element element) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final boolean isFunctionalInterface(final TypeElement type) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final boolean overrides(final ExecutableElement overrider, final ExecutableElement overridden, final TypeElement type) {
    throw new UnsupportedOperationException();
  }

  @Override // Elements
  public final void printElements(final Writer w, final Element... elements) {
    throw new UnsupportedOperationException();
  }

  
}
