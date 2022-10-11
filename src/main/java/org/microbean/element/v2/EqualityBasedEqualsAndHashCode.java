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

import javax.lang.model.type.TypeMirror;

public final class EqualityBasedEqualsAndHashCode implements EqualsAndHashCode<Object> {

  private final boolean includeAnnotations;

  public EqualityBasedEqualsAndHashCode(final boolean includeAnnotations) {
    super();
    this.includeAnnotations = includeAnnotations;
  }

  @Override
  public final boolean equals(final Object t, final Object o) {
    return Equality.equals(t, o, this.includeAnnotations);
  }
  
  @Override
  public final int hashCode(final Object t) {
    return Equality.hashCode(t, this.includeAnnotations);
  }  
  
}
