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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiPredicate;

import javax.lang.model.element.Element;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

// A type closure list builder.
public final class TypeClosure {

  // DefaultTypeMirror so things like list.contains(t) will work with
  // arbitrary TypeMirror implementations
  private List<DefaultTypeMirror> list;

  private final BiPredicate<? super Element, ? super Element> precedesPredicate;

  private final BiPredicate<? super Element, ? super Element> equalsPredicate;

  TypeClosure(final SupertypeVisitor supertypeVisitor, final SubtypeVisitor subtypeVisitor) {
    this(new PrecedesPredicate(supertypeVisitor, subtypeVisitor), null);
  }

  TypeClosure(final BiPredicate<? super Element, ? super Element> precedesPredicate) {
    this(precedesPredicate, null);
  }

  TypeClosure(final BiPredicate<? super Element, ? super Element> precedesPredicate,
              final BiPredicate<? super Element, ? super Element> equalsPredicate) {
    super();
    this.list = new ArrayList<>(10);
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
    this.equalsPredicate = equalsPredicate == null ? (e, f) -> Equality.equals(e, f, true) : equalsPredicate;
  }

  final boolean insert(final TypeMirror t) {
    return this.insert(this.list, DefaultTypeMirror.of(t));
  }

  private final boolean insert(final List<DefaultTypeMirror> list, final TypeMirror t) {
    if (list.isEmpty()) {
      list.add(0, DefaultTypeMirror.of(t));
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
      // Already have it.
      return false;
    } else if (this.precedesPredicate.test(e, headE)) {
      list.add(0, DefaultTypeMirror.of(t));
      return true;
    } else {
      return this.insert(list.subList(1, list.size()), t); // RECURSIVE
    }
  }

  final void union(final TypeClosure c2) {
    this.union(c2.list);
  }

  final void union(final List<? extends TypeMirror> c2) {
    final List<DefaultTypeMirror> unionList = this.union(this.list, c2);
    if (unionList != this.list) {
      this.list = unionList;
    }
  }

  private final List<DefaultTypeMirror> union(List<DefaultTypeMirror> c1, final List<? extends TypeMirror> c2) {
    if (c1.isEmpty()) {
      for (final TypeMirror t : c2) {
        c1.add(DefaultTypeMirror.of(t));
      }
      return c1;
    } else if (c2.isEmpty()) {
      return c1;
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
      c1.remove(0);
      c1 = this.union(c1, c2.subList(1, c2.size())); // RECURSIVE
      // c1 = this.union(new ArrayList<>(c1.subList(1, c1.size())), c2.subList(1, c2.size())); // RECURSIVE
      c1.add(0, DefaultTypeMirror.of(head1));
    } else if (this.precedesPredicate.test(head2E, head1E)) {
      this.union(c1, c2.subList(1, c2.size())); // RECURSIVE
      c1.add(0, DefaultTypeMirror.of(head2));
    } else {
      c1.remove(0);
      c1 = this.union(c1, c2); // RECURSIVE
      // c1 = this.union(new ArrayList<>(c1.subList(1, c1.size())), c2); // RECURSIVE
      c1.add(0, DefaultTypeMirror.of(head1));
    }
    return c1;
  }

  // Weird case. See
  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3717-L3718.
  final void prepend(final TypeVariable t) {
    assert t.getKind() == TypeKind.TYPEVAR;
    this.list.add(0, DefaultTypeMirror.of(t));
  }

  // Port of javac's Types#closureMin(List<Type>)
  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3915-L3945
  final List<TypeMirror> toMinimumTypes(final SubtypeVisitor subtypeVisitor) {
    final List<TypeMirror> classes = new ArrayList<>();
    final List<TypeMirror> interfaces = new ArrayList<>();
    final Set<DefaultTypeMirror> toSkip = new HashSet<>();

    final int size = this.list.size();

    for (int i = 0, next = 1; i < size; i++, next++) {
      final TypeMirror current = this.list.get(i);
      boolean keep = !toSkip.contains(DefaultTypeMirror.of(current));
      if (keep && current.getKind() == TypeKind.TYPEVAR && next < size) {
        for (int j = next; j < size; j++) {
          if (subtypeVisitor.withCapture(false).visit(this.list.get(j), current)) {
            // If there's a subtype of current "later" in the list, then
            // there's no need to "keep" current; it will be implied by
            // the subtype's supertype hierarchy.
            keep = false;
            break;
          }
        }
      }
      if (keep) {
        if (current.getKind() == TypeKind.DECLARED && ((DeclaredType)current).asElement().getKind().isInterface()) {
          interfaces.add(current);
        } else {
          classes.add(current);
        }
        if (next < size) {
          for (int j = next; j < size; j++) {
            final TypeMirror t = this.list.get(j);
            if (subtypeVisitor.withCapture(false).visit(current, t)) {
              // As we're processing this.list, we can skip supertypes
              // of current (t, here) because we know current is
              // already more specialized than they are.
              toSkip.add(DefaultTypeMirror.of(t));
            }
          }
        }
      }
    }
    classes.addAll(interfaces);
    return Collections.unmodifiableList(classes);
  }

  public final List<? extends TypeMirror> toList() {
    return List.copyOf(this.list);
  }

}
