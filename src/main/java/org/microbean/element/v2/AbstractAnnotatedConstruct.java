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

import java.lang.annotation.Annotation;

import java.util.List;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;

@SuppressWarnings("deprecation")
public abstract sealed class AbstractAnnotatedConstruct implements AnnotatedConstruct permits AbstractElement, AnnotatedName, AbstractTypeMirror {

  protected AbstractAnnotatedConstruct() {
    super();
  }

  @Override // AnnotatedConstruct
  public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return null;
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("unchecked")
  public <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return null;
  }
  
}
