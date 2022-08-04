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

import java.util.List;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;

public abstract class AbstractAnnotatedConstructBuilder<T extends AnnotatedConstruct, B extends AbstractAnnotatedConstructBuilder<T, B>> extends AbstractBuilder<T, B> {

  private List<? extends AnnotationMirror> annotations;
  
  protected AbstractAnnotatedConstructBuilder() {
    super();
    this.annotations = List.of();
  }
  
  public final List<? extends AnnotationMirror> annotations() {
    return this.annotations;
  }
  
  public B withAnnotations(final List<? extends AnnotationMirror> annotations) {
    this.annotations = List.copyOf(annotations);
    return self();
  }
  
}
