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

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DeferredList<E> extends AbstractList<E> {

  private List<E> list;

  private final Consumer<? super E> transformer;
  
  private final Supplier<? extends List<E>> supplier;

  public DeferredList(final Supplier<? extends List<E>> supplier) {
    this(supplier, null);
  }
  
  public DeferredList(final Supplier<? extends List<E>> supplier,
                      final Consumer<? super E> transformer) {
    super();
    this.supplier = Objects.requireNonNull(supplier, "supplier");
    this.transformer = transformer;
  }

  public final DeferredList<E> withTransformation(final Consumer<? super E> transformer) {
    return new DeferredList<>(this.supplier, transformer);
  }

  @Override
  public final E get(final int index) {
    return this.list().get(index);
  }

  @Override
  public final int size() {
    return this.list().size();
  }

  @Override
  public final boolean isEmpty() {
    return this.list().isEmpty();
  }

  private final List<E> list() {
    if (this.list == null) {
      if (this.transformer == null) {
        this.list = List.copyOf(this.supplier.get());
      } else {
        final List<E> listCopy = List.copyOf(this.supplier.get());
        for (final E element : listCopy) {
          this.transformer.accept(element);
        }
        this.list = listCopy;
      }
    }
    return this.list;
  }

}
