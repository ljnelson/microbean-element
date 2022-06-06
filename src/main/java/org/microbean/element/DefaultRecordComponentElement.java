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
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;

import javax.lang.model.type.TypeMirror;

public class DefaultRecordComponentElement extends AbstractElement implements RecordComponentElement {

  private final ExecutableElement accessor;

   public DefaultRecordComponentElement(final Name simpleName,
                                       final TypeMirror type,
                                       final Set<? extends Modifier> modifiers,
                                       final AbstractElement enclosingElement, // the record element; TODO change to DefaultRecordElement
                                       final ExecutableElement accessor,
                                       final List<? extends AnnotationMirror> annotationMirrors) {
    super(simpleName,
          ElementKind.RECORD_COMPONENT,
          validate(type),
          modifiers,
          enclosingElement,
          annotationMirrors);
    this.accessor = Objects.requireNonNull(accessor, "accessor");
  }
  
  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitRecordComponent(this, p);
  }

  @Override // RecordComponentElement
  public final ExecutableElement getAccessor() {
    return this.accessor;
  }

  private static final TypeMirror validate(final TypeMirror type) {
    return type;
  }
  
}
