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

import java.lang.reflect.RecordComponent;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeMirror;

public class DefaultRecordComponentElement extends AbstractElement implements RecordComponentElement {

  private final ExecutableElement accessor;

  public DefaultRecordComponentElement(final AnnotatedName simpleName,
                                       final TypeMirror type,
                                       final Set<? extends Modifier> modifiers,
                                       final ExecutableElement accessor) {
    super(simpleName,
          ElementKind.RECORD_COMPONENT,
          validate(type),
          modifiers,
          null,
          List::of);
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

  @Override // AbstractElement
  public final void setEnclosingElement(final Element enclosingElement) {
    super.setEnclosingElement(validate(enclosingElement));
  }


  /*
   * Static methods.
   */


  private static final <T extends TypeMirror> T validate(final T type) {
    switch (type.getKind()) {
    case DECLARED:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  private static final <E extends Element> E validate(final E enclosingElement) {
    switch (enclosingElement.getKind()) {
    case RECORD:
      return enclosingElement;
    default:
      throw new IllegalArgumentException("enclosingElement: " + enclosingElement);
    }
  }

  public static final DefaultRecordComponentElement of(final RecordComponentElement r) {
    if (r instanceof DefaultRecordComponentElement drce) {
      return drce;
    } else {
      return
        new DefaultRecordComponentElement(AnnotatedName.of(r.getAnnotationMirrors(),
                                                           r.getSimpleName()),
                                          r.asType(),
                                          r.getModifiers(),
                                          r.getAccessor());
    }
  }
  
  public static final DefaultRecordComponentElement of(final RecordComponent r) {
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(r.getName()));
    final TypeMirror type = AbstractTypeMirror.of(r.getAnnotatedType());
    // The compiler will say "record components cannot have modifiers"
    // and then if you actually ask the compiler's version of all this
    // it will return [public].
    final Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
    final ExecutableElement accessor = DefaultExecutableElement.of(r.getAccessor());
    return new DefaultRecordComponentElement(simpleName, type, modifiers, accessor);
  }

}
