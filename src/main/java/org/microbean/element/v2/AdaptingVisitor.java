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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

// Basically done
// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4590-L4690
final class AdaptingVisitor extends SimpleTypeVisitor14<Void, TypeMirror> {

  // The compiler's implementation mutates this list.
  private final List<TypeMirror> from;

  // The compiler's implementation mutates this list.
  private final List<TypeMirror> to;

  private final Types2 types2;
  
  private final Map<DefaultElement, TypeMirror> mapping;

  private final IsSameTypeVisitor isSameTypeVisitor;

  private final SubtypeVisitor subtypeVisitor;

  private final Set<TypeMirrorPair> cache;
  
  AdaptingVisitor(final Types2 types2,
                  final IsSameTypeVisitor isSameTypeVisitor,
                  final SubtypeVisitor subtypeVisitor,
                  final List<TypeMirror> from, // mutated
                  final List<TypeMirror> to) { // mutated
    super();
    this.types2 = Objects.requireNonNull(types2, "types2");
    this.isSameTypeVisitor = Objects.requireNonNull(isSameTypeVisitor, "isSameTypeVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
    this.mapping = new HashMap<>();
    this.cache = new HashSet<>();
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
  }

  public final void adapt(final TypeMirror source, final TypeMirror target) {
    this.visit(source, target);
    final int fromSize = this.from.size();    
    for (int i = 0; i < fromSize; i++) {
      final TypeMirror val = this.mapping.get(DefaultElement.of(this.types2.asElement(this.from.get(i), true)));
      if (this.to.get(i) != val) {
        this.to.set(i, val);
      }
    }
  }
  
  @Override
  public final Void visitArray(final ArrayType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.ARRAY;
    if (target.getKind() == TypeKind.ARRAY) {
      this.adaptRecursive(source.getComponentType(), ((ArrayType)target).getComponentType());
    }
    return null;
  }
  
  @Override
  public final Void visitDeclared(final DeclaredType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.DECLARED;
    if (target.getKind() == TypeKind.DECLARED) {
      this.adaptRecursive(this.types2.allTypeArguments(source), this.types2.allTypeArguments(target));
    }
    return null;
  }
  
  @Override
  public final Void visitTypeVariable(final TypeVariable source, final TypeMirror target) {
    assert source.getKind() == TypeKind.TYPEVAR;
    final DefaultElement sourceElement = DefaultElement.of(source.asElement());
    TypeMirror val = this.mapping.get(sourceElement);
    if (val == null) {
      val = target;
      this.from.add(source);
      this.to.add(target);
    } else if (val.getKind() == TypeKind.WILDCARD && target.getKind() == TypeKind.WILDCARD) {
      final WildcardType valWc = (WildcardType)val;
      final TypeMirror valSuperBound = valWc.getSuperBound();
      final TypeMirror valExtendsBound = valWc.getExtendsBound();
      
      final WildcardType targetWc = (WildcardType)target;
      final TypeMirror targetSuperBound = targetWc.getSuperBound();
      final TypeMirror targetExtendsBound = targetWc.getExtendsBound();

      if (valSuperBound == null) {
        if (valExtendsBound == null) {
          // valWc is lower-bounded (and upper-bounded)
          if (targetExtendsBound == null &&
              this.subtypeVisitor.withCapture(true).visit(this.types2.superBound(val), this.types2.superBound(target))) {
            // targetWc is lower-bounded (and maybe unbounded)
            val = target;
          }
        } else if (targetSuperBound == null &&
                   !this.subtypeVisitor.withCapture(true).visit(this.types2.extendsBound(val), this.types2.extendsBound(target))) {
          // valWc is upper-bounded
          // targetWc is upper-bounded (and maybe unbounded)
          val = target;
        }
      } else if (valExtendsBound == null) {
        // valWc is lower-bounded
        if (targetExtendsBound == null &&
            this.subtypeVisitor.withCapture(true).visit(this.types2.superBound(val), this.types2.superBound(target))) {
          // targetWc is lower-bounded (and maybe unbounded)
          val = target;
        }
      } else {
        throw new IllegalStateException("val: " + val);
      }
    } else if (!this.isSameTypeVisitor.visit(val, target)) {
      throw new IllegalStateException();
    }
    this.mapping.put(sourceElement, val);
    return null;
  }

  @Override
  public final Void visitWildcard(final WildcardType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.WILDCARD;
    final TypeMirror extendsBound = source.getExtendsBound();
    final TypeMirror superBound = source.getSuperBound();
    if (extendsBound == null) {
      if (superBound == null) {
        this.adaptRecursive(this.types2.extendsBound(source), this.types2.extendsBound(target));
      } else {
        this.adaptRecursive(this.types2.superBound(source), this.types2.superBound(target));
      }
    } else if (superBound == null) {
      this.adaptRecursive(this.types2.extendsBound(source), this.types2.extendsBound(target));
    } else {
      throw new IllegalArgumentException("source: " + source);
    }
    return null;
  }

  private final void adaptRecursive(final TypeMirror source, final TypeMirror target) {
    final TypeMirrorPair pair = new TypeMirrorPair(this.types2, this.isSameTypeVisitor, source, target);
    if (this.cache.add(pair)) {
      try {
        this.visit(source, target);
      } finally {
        this.cache.remove(pair);
      }
    }
  }

  private final void adaptRecursive(final Collection<? extends TypeMirror> source, final Collection<? extends TypeMirror> target) {
    if (source.size() == target.size()) {
      final Iterator<? extends TypeMirror> sourceIterator = source.iterator();
      final Iterator<? extends TypeMirror> targetIterator = target.iterator();
      while (sourceIterator.hasNext()) {
        assert targetIterator.hasNext();
        this.adaptRecursive(sourceIterator.next(), targetIterator.next());
      }
    }
  }
  
}
