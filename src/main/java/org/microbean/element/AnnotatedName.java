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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;

public final class AnnotatedName extends AbstractAnnotatedConstruct {

  private final Name name;
  
  public AnnotatedName(final List<? extends AnnotationMirror> annotationMirrors,
                       final Name name) {
    super(annotationMirrors);
    this.name = Objects.requireNonNull(name);
  }

  public final Name getName() {
    return this.name;
  }

  @Override // Object
  public final int hashCode() {
    return Equality.hashCode(this, true);
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return Equality.equals(this, other, true);
    } else {
      return false;
    }
  }

  @Override // Object
  public final String toString() {
    return this.name.toString();
  }
  
}
