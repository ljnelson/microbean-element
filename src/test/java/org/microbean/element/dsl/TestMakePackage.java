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
package org.microbean.element.dsl;

import java.util.List;

import java.util.function.Supplier;

import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

final class TestMakePackage {

  private TestMakePackage() {
    super();
  }

  @Test
  final void test() throws Exception {
    
  }

  private static final class Packages
    implements PackageBuilder {

    /*
     * Things to think about:
     *
     * If I get one of these and just call build() right away: is that legal?
     *
     * * name would be empty (default) (OK)
     * * annotations would be empty (default) (OK)
     * * enclosed elements would be empty (default) (OK; we don't really care about an empty package)
     */

    private List<AnnotationMirror> annotations;

    private Stream<TypeElement> rawEnclosedElements;
    
    private Packages() {
      super();
      this.annotations = List.of();
      this.rawEnclosedElements = Stream.of();
    }
    
    @Override
    public final List<? extends AnnotationMirror> annotations() {
      return this.annotations;
    }

    @Override
    public final Packages annotations(final List<? extends AnnotationMirror> annotations) {
      this.annotations = annotations == null || annotations.isEmpty() ? List.of() : List.copyOf(annotations);
      return this;
    }

    @Override
    public final Supplier<? extends List<? extends TypeElement>> enclosedElements() {
      return this.rawEnclosedElements::toList;
    }

    @Override
    public final Packages enclosedElements(final Stream<TypeElement> enclosedElements) {
      this.rawEnclosedElements = enclosedElements == null ? Stream.of() : enclosedElements;
      return this;
    }

    @Override
    public final Name fullyQualifiedName() {
      return null;
    }

    @Override
    public final Packages fullyQualifiedName(final Name name) {
      return this;
    }

    @Override
    public final PackageElement build() {
      return null;
    }
    
  }
  
}
