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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiPredicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

final class Closure {

  private final List<TypeMirror> list;

  private final BiPredicate<? super Element, ? super Element> precedesPredicate;

  private final BiPredicate<? super Element, ? super Element> equalsPredicate;

  Closure(final BiPredicate<? super Element, ? super Element> precedesPredicate) {
    this(precedesPredicate, null);
  }
  
  Closure(final BiPredicate<? super Element, ? super Element> precedesPredicate,
          final BiPredicate<? super Element, ? super Element> equalsPredicate) {
    super();
    this.list = new ArrayList<>(10);
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
    this.equalsPredicate = equalsPredicate == null ? (e, f) -> Equality.equals(e, f, true) : equalsPredicate;
  }

  final boolean insert(final TypeMirror t) {
    return this.insert(this.list, t);
  }

  private final boolean insert(final List<TypeMirror> list, final TypeMirror t) {
    if (list.isEmpty()) {
      list.add(0, t);
      return true;
    }
    final Element e;
    switch (t.getKind()) {
    case DECLARED:
      e = ((DeclaredType)t).asElement();
      break;
    case TYPEVAR:
      e = ((TypeVariable)t).asElement();
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeMirror head = list.get(0);
    final Element headE;
    switch (head.getKind()) {
    case DECLARED:
      headE = ((DeclaredType)head).asElement();
      break;
    case TYPEVAR:
      headE = ((TypeVariable)head).asElement();
      break;
    default:
      throw new IllegalStateException();
    }
    if (this.equalsPredicate.test(e, headE)) {
      return false;
    } else if (this.precedesPredicate.test(e, headE)) {
      list.add(0, t);
      return true;
    }
    return this.insert(list.subList(1, list.size()), t); // RECURSIVE
  }

  final void union(final List<TypeMirror> c2) {
    this.union(this.list, c2);
  }

  private final void union(final List<TypeMirror> c1, final List<TypeMirror> c2) {
    if (c1.isEmpty()) {
      c1.addAll(c2);
      return;
    } else if (c2.isEmpty()) {
      return;
    }
    final TypeMirror head1 = c1.get(0);
    final Element head1E;
    switch (head1.getKind()) {
    case DECLARED:
      head1E = ((DeclaredType)head1).asElement();
      break;
    case TYPEVAR:
      head1E = ((TypeVariable)head1).asElement();
      break;
    default:
      throw new IllegalArgumentException("c1: " + c1);
    }
    final TypeMirror head2 = c2.get(0);
    final Element head2E;
    switch (head2.getKind()) {
    case DECLARED:
      head2E = ((DeclaredType)head2).asElement();
      break;
    case TYPEVAR:
      head2E = ((TypeVariable)head2).asElement();
      break;
    default:
      throw new IllegalArgumentException("c2: " + c2);
    }
    if (this.equalsPredicate.test(head1E, head2E)) {
      this.union(c1.subList(1, c1.size()), c2.subList(1, c2.size())); // RECURSIVE
      c1.add(0, head1);
    } else if (this.precedesPredicate.test(head2E, head1E)) {
      this.union(c1, c2.subList(1, c2.size())); // RECURSIVE
      c1.add(0, head2);
    } else {
      this.union(c1.subList(1, c1.size()), c2); // RECURSIVE
      c1.add(0, head1);
    }
  }

  final List<TypeMirror> toMinimumTypes(final SubtypeVisitor subtypeVisitor) {
    final List<TypeMirror> classes = new ArrayList<>();
    final List<TypeMirror> interfaces = new ArrayList<>();
    final Set<TypeMirror> toSkip = new HashSet<>();

    final int size = this.list.size();

    for (int i = 0; i < size; i++) {
      final TypeMirror type = this.list.get(i);
      final DefaultTypeMirror current = DefaultTypeMirror.of(type);
      boolean keep = !toSkip.contains(current);
      final int next = i + 1;
      if (keep && current.getKind() == TypeKind.TYPEVAR && next < size) {
        for (int j = next; j < size; j++) {
          if (subtypeVisitor.withCapture(false).visit(this.list.get(j), current)) {
            keep = false;
            break;
          }
        }
      }
      if (keep) {
        if (current.getKind() == TypeKind.DECLARED &&
            ((DeclaredType)current).asElement() instanceof TypeElement te &&
            te.getKind().isInterface()) {
          interfaces.add(current);
        } else {
          classes.add(current);
        }
        if (next < size) {
          for (int j = next; j < size; j++) {
            final TypeMirror t = this.list.get(j);
            if (subtypeVisitor.withCapture(false).visit(current, t)) {
              toSkip.add(DefaultTypeMirror.of(t));
            }
          }
        }
      }      
    }
    classes.addAll(interfaces);
    return Collections.unmodifiableList(classes);
  }

  final List<TypeMirror> toList() {
    return List.copyOf(this.list);
  }
  
}
