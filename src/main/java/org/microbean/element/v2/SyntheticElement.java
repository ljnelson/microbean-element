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

import java.util.List;
import java.util.Set;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import javax.lang.model.element.ElementKind;

import javax.lang.model.type.TypeMirror;

final class SyntheticElement extends AbstractElement {

  private final Reference<TypeMirror> type;

  SyntheticElement(final TypeMirror type) {
    this(generateName(type), type);
  }

  SyntheticElement(final AnnotatedName name, final TypeMirror type) {
    super(name, ElementKind.OTHER, null, Set.of(), null, null);
    this.type = new WeakReference<>(type);
  }

  @Override
  public final TypeMirror asType() {
    final TypeMirror t = this.type.get();
    return t == null ? DefaultNoType.NONE : t;
  }

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public final boolean equals(final Object other) {
    return this == other;
  }

  static final AnnotatedName generateName(final TypeMirror t) {
    return AnnotatedName.of(DefaultName.of()); // TODO if it turns out to be important
  }

}
