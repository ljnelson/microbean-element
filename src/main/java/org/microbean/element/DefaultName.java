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

import java.util.stream.IntStream;

import javax.lang.model.element.Name;

public final class DefaultName implements Name {

  public static final DefaultName EMPTY = new DefaultName("");

  public static final DefaultName JAVA_LANG_OBJECT = DefaultName.of("java.lang.Object");

  private final String name;

  private DefaultName(final String s) {
    super();
    this.name = s;
  }

  @Override // CharSequence
  public final char charAt(final int index) {
    return this.name.charAt(index);
  }

  @Override // CharSequence
  public final IntStream chars() {
    return this.name.chars();
  }

  @Override // CharSequence
  public final IntStream codePoints() {
    return this.name.codePoints();
  }

  @Override // CharSequence
  public final int length() {
    return this.name.length();
  }

  @Override // CharSequence
  public final DefaultName subSequence(final int start, final int end) {
    return of(this.name.subSequence(start, end));
  }

  @Override // Name
  public final boolean contentEquals(final CharSequence cs) {
    return this.name.equals(cs.toString());
  }

  @Override // Object
  public final int hashCode() {
    return this.name.hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return this.contentEquals((DefaultName)other);
    } else {
      return false;
    }
  }

  @Override // CharSequence
  public final String toString() {
    return this.name;
  }


  /*
   * Static methods.
   */


  public static final DefaultName of() {
    return EMPTY;
  }

  public static final DefaultName of(final CharSequence cs) {
    if (cs instanceof DefaultName dn) {
      return dn;
    } else if (cs == null || cs.length() <= 0) {
      return of();
    }
    return new DefaultName(cs.toString());
  }

  public static final DefaultName ofSimple(final CharSequence cs) {
    final int lastDotIndex = cs == null ? -1 : cs.toString().lastIndexOf('.');
    if (lastDotIndex > 0 && cs.length() > 2) {
      return of(cs.subSequence(lastDotIndex + 1, cs.length()));
    }
    return of(cs);
  }


}
