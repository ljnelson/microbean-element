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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import org.microbean.development.annotation.Convenience;

// The spirit of this class and this whole exercise is to ultimately
// implement the javac type assignability rules on top of the java
// lang model while preserving the structural components of a type.
//
// Some of the rules are javac's and some of them are defined by the
// Java Language Specification.  They don't always line up, somewhat
// surprisingly.  In general, I try to follow javac, not the
// specification.
//
// In general, the spirit of the methods in this class is a faithful
// port with edits and the occasional rename. javac, for example, uses
// elemtype to refer to an array type's component type; such a
// construct is referred to as a componentType in this class.  javac
// sometimes names a Type-typed parameter "elem"; this class does not
// follow suit.
//
// There seem to be an awful lot of substitutions of bounds in javac
// where strictly speaking none are called for; this class generally
// follows the javac path.
//
// It is also worth noting that javac itself has all kinds of
// problems:
//
// https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20JDK%20AND%20labels%20%3D%20jls-types
//
// https://bugs.openjdk.java.net/browse/JDK-8154901
//
// https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20JDK%20AND%20issuetype%20%3D%20Bug%20AND%20status%20%3D%20Open%20AND%20labels%20in%20(javac%2C%20javac-types%2C%20javac-wildcards%2C%20javac-check-spec)
//
// I've made no attempt to repair these.
//
// javac's type hierarchy is...interesting.  For example, an
// IntersectionClassType "is a" ClassType, whereas in the lang model
// IntersectionType is distinct from DeclaredType and
// TypeKind.DECLARED != TypeKind.INTERSECTION (obviously).
//
// Most types in the javac type model have symbols, even where such a
// thing doesn't make sense.  This is probably left over from Java's
// earliest more Smalltalky days.  The lang model, by contrast, does
// not provide asElement() methods on most of its types—even for those
// types where such a thing would make sense, like ExecutableType.
// Because javac still uses elements ("symbols") in many type
// calculations, I synthesize some elements where they are needed.
//
// javac, being a true compiler, of course, understandably does many
// fancy things with error types.  In general, this class bails out.
final class Types {

  private static final DefaultDeclaredType CLONEABLE_TYPE = DefaultDeclaredType.JAVA_LANG_CLONEABLE;

  private static final DefaultDeclaredType SERIALIZABLE_TYPE = DefaultDeclaredType.JAVA_IO_SERIALIZABLE;

  // @GuardedBy("itself")
  private static final WeakHashMap<TypeMirror, Element> syntheticElements = new WeakHashMap<>();

  // @GuardedBy("itself")
  private static final Set<TypeMirrorPair> containsRecursiveCache = new HashSet<>();

  // @GuardedBy("itself")
  private static final Set<Element> asSuperSeenTypes = new HashSet<>();

  /*
   * Constructors.
   */


  private Types() {
    super();
  }


  /*
   * Static methods.
   */

  private static final void adapt(final TypeMirror source,
                                  final TypeMirror target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to) {
    adapt(source, target, from, to, new HashMap<>(), new HashSet<>());
  }

  // I have no idea what this method is doing. Ported slavishly from javac.
  private static final void adapt(final TypeMirror source,
                                  final TypeMirror target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to,
                                  final Map<Element, TypeMirror> mapping,
                                  final Set<TypeMirrorPair> adaptCache) {
    // First do the visiting
    switch (source.getKind()) {
    case ARRAY:
      adapt((ArrayType)source, target, from, to, mapping, adaptCache);
      break;
    case DECLARED:
    case INTERSECTION:
      adapt0(source, target, from, to, mapping, adaptCache);
      break;
    case TYPEVAR:
      adapt((TypeVariable)source, target, from, to, mapping, adaptCache);
      break;
    case WILDCARD:
      adapt((WildcardType)source, target, from, to, mapping, adaptCache);
      break;
    default:
      break;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("source: " + source);
    }

    // Then do the mysterious symbol processing.
    /*
      List<Type> fromList = from.toList();
      List<Type> toList = to.toList();
      while (!fromList.isEmpty()) {
          Type val = mapping.get(fromList.head.tsym);
          if (toList.head != val)
              toList.head = val;
          fromList = fromList.tail;
          toList = toList.tail;
      }
    */
    final int size = from.size();
    for (int i = 0; i < size; i++) {
      final TypeMirror val = mapping.get(asElement(from.get(i), true));
      // Note that there's no check on the size of to
      if (val == null || i + 1 >= to.size() || !Equality.equals(val, to.get(i), true)) {
        to.set(i, val);
      }
    }
  }

  private static final void adapt(final ArrayType source,
                                  final TypeMirror target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to,
                                  final Map<Element, TypeMirror> mapping,
                                  final Set<TypeMirrorPair> adaptCache) {
    switch (target.getKind()) {
    case ARRAY:
      adapt(source, (ArrayType)target, from, to, mapping, adaptCache);
      break;
    default:
      break;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("target: " + target);
    }
  }

  private static final void adapt(final ArrayType source,
                                  final ArrayType target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to,
                                  final Map<Element, TypeMirror> mapping,
                                  final Set<TypeMirrorPair> adaptCache) {
    assert source.getKind() == TypeKind.ARRAY;
    assert target.getKind() == TypeKind.ARRAY;
    adaptRecursive(source.getComponentType(), target.getComponentType(), from, to, mapping, adaptCache);
  }

  private static final void adapt0(final TypeMirror source,
                                   final TypeMirror target,
                                   final List<TypeMirror> from,
                                   final List<TypeMirror> to,
                                   final Map<Element, TypeMirror> mapping,
                                   final Set<TypeMirrorPair> adaptCache) {
    assert source.getKind() == TypeKind.DECLARED || source.getKind() == TypeKind.INTERSECTION;
    switch (target.getKind()) {
    case DECLARED:
    case INTERSECTION:
      adaptRecursive(allTypeArguments(source), allTypeArguments(target), from, to, mapping, adaptCache);
      break;
    default:
      break;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("target: " + target);
    }
  }

  private static final void adapt(final TypeVariable source,
                                  final TypeMirror target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to,
                                  final Map<Element, TypeMirror> mapping,
                                  final Set<TypeMirrorPair> adaptCache) {
    assert source.getKind() == TypeKind.TYPEVAR;
    final Element sourceElement = asElement(source);
    TypeMirror val = mapping.get(sourceElement);
    if (val == null) {
      val = target;
      from.add(source);
      to.add(target);
    } else {
      switch (val.getKind()) {
      case WILDCARD:
        final WildcardType valW = (WildcardType)val;
        switch (target.getKind()) {
        case WILDCARD:
          final WildcardType targetW = (WildcardType)target;
          final TypeMirror valLowerBound = valW.getSuperBound();
          final TypeMirror valUpperBound = valW.getExtendsBound();
          final TypeMirror targetLowerBound = targetW.getSuperBound();
          final TypeMirror targetUpperBound = targetW.getExtendsBound();
          if (valLowerBound == null) {
            if (valUpperBound == null) {
              // valW is lower-bounded (and upper-bounded)
              if (targetUpperBound == null && subtype(wildcardLowerBound(val), wildcardLowerBound(target), true)) {
                // targetW is lower-bounded (and maybe unbounded)
                val = target;
              }
            } else if (targetLowerBound == null && !subtype(wildcardUpperBound(val), wildcardUpperBound(target), true)) {
              // valW is upper-bounded
              // targetW is upper-bounded (and maybe unbounded)
              val = target;
            }
          } else if (valUpperBound == null) {
            // valW is lower-bounded
            if (targetUpperBound == null && subtype(wildcardLowerBound(val), wildcardLowerBound(target), true)) {
              // targetW is lower-bounded (and maybe unbounded)
              val = target;
            }
          } else {
            throw new IllegalStateException("val: " + val);
          }
          break;
        default:
          break;
        case ERROR:
        case UNION:
          throw new IllegalArgumentException("target: " + target);
        }
        break;
      default:
        if (!models(val, target)) {
          throw new IllegalStateException();
        }
        break;
      case ERROR:
      case UNION:
        throw new IllegalStateException("val: " + val);
      }
    }
    mapping.put(sourceElement, val);
  }

  private static final void adapt(final WildcardType source,
                                  final TypeMirror target,
                                  final List<TypeMirror> from,
                                  final List<TypeMirror> to,
                                  final Map<Element, TypeMirror> mapping,
                                  final Set<TypeMirrorPair> adaptCache) {
    assert source.getKind() == TypeKind.WILDCARD;
    if (source.getSuperBound() == null) {
      // upper-bounded; maybe unbounded
      adaptRecursive(wildcardUpperBound(source), wildcardUpperBound(target), from, to, mapping, adaptCache);
    } else if (source.getExtendsBound() == null) {
      adaptRecursive(wildcardLowerBound(source), wildcardLowerBound(target), from, to, mapping, adaptCache);
    } else {
      throw new IllegalArgumentException("source: " + source);
    }
  }

  private static final void adaptRecursive(final TypeMirror source,
                                           final TypeMirror target,
                                           final List<TypeMirror> from,
                                           final List<TypeMirror> to,
                                           final Map<Element, TypeMirror> mapping,
                                           final Set<TypeMirrorPair> adaptCache) {
    final TypeMirrorPair pair = new TypeMirrorPair(source, target);
    if (adaptCache.add(pair)) {
      try {
        adapt(source, target, from, to, mapping, adaptCache);
      } finally {
        adaptCache.remove(pair);
      }
    }
  }

  private static final void adaptRecursive(final List<? extends TypeMirror> source,
                                           final List<? extends TypeMirror> target,
                                           final List<TypeMirror> from,
                                           final List<TypeMirror> to,
                                           final Map<Element, TypeMirror> mapping,
                                           final Set<TypeMirrorPair> adaptCache) {
    final int sourceSize = source.size();
    if (sourceSize > 0 && sourceSize == target.size()) {
      for (int i = 0; i < sourceSize; i++) {
        adaptRecursive(source.get(i), target.get(i), from, to, mapping, adaptCache);
      }
    }
  }

  private static final void adaptSelf(final TypeMirror t,
                                      final List<TypeMirror> from,
                                      final List<TypeMirror> to) {
    adaptSelf(t, from, to, new HashMap<>(), new HashSet<>());
  }

  private static final void adaptSelf(final TypeMirror t,
                                      final List<TypeMirror> from,
                                      final List<TypeMirror> to,
                                      final Map<Element, TypeMirror> mapping,
                                      final Set<TypeMirrorPair> adaptCache) {
    adapt(canonicalType(t), t, from, to, mapping, adaptCache);
  }

  @Convenience
  // Not visitor-based in javac
  private static final List<? extends TypeMirror> allTypeArguments(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return allTypeArguments(((ArrayType)t).getComponentType()); // RECURSIVE
    case DECLARED:
      return allTypeArguments((DeclaredType)t);
    case INTERSECTION:
      // Verified; see
      // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1265
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final List<? extends TypeMirror> allTypeArguments(final DeclaredType t) {
    assert t.getKind() == TypeKind.DECLARED;
    final List<? extends TypeMirror> enclosingTypeTypeArguments = allTypeArguments(t.getEnclosingType());
    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    if (enclosingTypeTypeArguments.isEmpty()) {
      return typeArguments;
    } else if (typeArguments.isEmpty()) {
      return enclosingTypeTypeArguments;
    } else {
      final List<TypeMirror> list = new ArrayList<>(enclosingTypeTypeArguments.size() + typeArguments.size());
      list.addAll(enclosingTypeTypeArguments);
      list.addAll(typeArguments);
      return Collections.unmodifiableList(list);
    }
  }

  // Is at least one t in ts identical to s?
  private static final boolean anyIdentical(final Collection<? extends TypeMirror> ts,
                                            final TypeMirror s) {
    if (!ts.isEmpty()) {
      for (final TypeMirror t : ts) {
        if (Equality.equals(t, s, false)) {
          return true;
        }
      }
    }
    return false;
  }

  // Does at least one t in ts reference at least one s in ss?
  private static final boolean anyReferencesAny(final Collection<? extends TypeMirror> ts,
                                                final Collection<? extends TypeMirror> ss) {
    if (!ts.isEmpty() && !ss.isEmpty()) {
      for (final TypeMirror t : ts) {
        if (referencesAny(t, ss)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private static final ArrayType arrayType(final TypeMirror componentType) {
    return arrayType(componentType, null);
  }

  private static final ArrayType arrayType(final TypeMirror componentType,
                                           final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultArrayType.of(componentType, annotationMirrors);
  }

  private static final Element asElement(final TypeMirror t) {
    // "Returns the element corresponding to a type. The type may be a
    // DeclaredType or TypeVariable. Returns null if the type is not
    // one with a corresponding element."
    //
    // This does not correspond at all to the innards of javac, where
    // nearly every type has an associated element.  For example,
    // error types, intersection types, executable types, primitive
    // types and wildcard types (which aren't even types!) all have
    // elements somehow, but not in the lang model for some reason.
    //
    // Much of javac's algorithmic behavior is based on types having
    // elements.  In fact, the only types in javac that have no
    // elements at all are:
    //
    // * no type
    // * void type
    // * null type
    // * unknown type
    //
    // Symbols in javac do not override their equals()/hashCode()
    // methods, so no two symbols are ever the same.
    //
    // We can take advantage of all these facts and, perhaps, set up
    // synthetic elements for types that, in the lang model, don't
    // have them, but do have them behind the scenes in javac.  We'll
    // need a WeakHashMap to associate TypeMirror instances with their
    // synthetic elements.
    return asElement(t, true);
  }

  private static final Element asElement(final TypeMirror t, final boolean generateSyntheticElements) {
    if (t == null) {
      return null;
    }
    switch (t.getKind()) {

    case DECLARED:
      return ((DeclaredType)t).asElement();

    case TYPEVAR:
      return ((TypeVariable)t).asElement();

    case ARRAY:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> ArrayElement.INSTANCE);
        }
      }
      return null;

    case EXECUTABLE:
      // This is really problematic.  There *is* an ExecutableElement
      // in the lang model, and an ExecutableType, but they aren't
      // related in the way that, say, DeclaredType and TypeElement
      // are. javac seems to use a singleton synthetic ClassType (!)
      // for all method symbols.  I'm not sure what to do here.  I'm
      // going to leave it null for now.
      /*
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> ExecutableElement.INSTANCE);
        }
      }
      */
      return null;

    case WILDCARD:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> WildcardElement.INSTANCE);
        }
      }
      return null;

    case BOOLEAN:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.BOOLEAN);
        }
      }
      return null;

    case BYTE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.BYTE);
        }
      }
      return null;

    case CHAR:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.CHAR);
        }
      }
      return null;

    case DOUBLE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.DOUBLE);
        }
      }
      return null;

    case FLOAT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.FLOAT);
        }
      }
      return null;

    case INT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.INT);
        }
      }
      return null;

    case LONG:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.LONG);
        }
      }
      return null;

    case SHORT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> PrimitiveElement.SHORT);
        }
      }
      return null;

    case INTERSECTION:
    case MODULE:
    case PACKAGE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, SyntheticElement::new);
        }
      }
      return null;

    case OTHER:
    case NONE:
    case NULL:
    case VOID:
      return null;

    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // Not visitor-based in javac
  private static final TypeMirror asOuterSuper(final TypeMirror t, final Element e) {
    switch (t.getKind()) {
    case ARRAY:
      final TypeMirror et = e.asType();
      return subtype(t, et, true) ? et : null;
    case DECLARED:
    case INTERSECTION:
      return asOuterSuper0(t, e);
    case TYPEVAR:
      return asSuper(t, e);
    default:
      return null;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror asOuterSuper0(final TypeMirror t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION : t.getKind();
    TypeMirror x = t;
    WHILE_LOOP:
    while (x != null) {
      final TypeKind xk = x.getKind();
      if (xk == TypeKind.NONE) {
        // For some reason, javac's Types#asOuterSuper(Type, Type) uses null as a sentinel
        // value, even though NONE is used elsewhere.  We follow suit.
        return null;
      }
      final TypeMirror s = asSuper(x, e);
      if (s != null) {
        return s;
      }
      switch (xk) {
      case DECLARED:
        x = ((DeclaredType)x).getEnclosingType();
        continue WHILE_LOOP;
      default:
        return null;
      case NONE:
        // (We already checked this.)
        throw new AssertionError();
      case ERROR:
      case UNION:
        throw new IllegalStateException("x: " + x);
      }
    }
    return null;
  }

  // SimpleVisitor-based
  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2131-L2215
  private static final TypeMirror asSuper(final TypeMirror t, final Element e) {
    // TODO: optimize for when e.asType() is java.lang.Object
    switch (t.getKind()) {
    case ARRAY:
      return asSuper((ArrayType)t, e); // RECURSIVE
    case DECLARED:
    case INTERSECTION:
    case TYPEVAR:
      return asSuper0(t, e);
    default:
      return null; // Yes, really; not sure why NONE isn't used but this is critical
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror asSuper(final ArrayType t, final Element e) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror elementType = e.asType();
    return subtype(t, elementType, true) ? elementType : null;
  }

  private static final TypeMirror asSuper0(final TypeMirror t, final Element e) {
    final Element te = asElement(t, true);
    if (Equality.equals(te, e, true)) {
      return t;
    }
    if (t.getKind() == TypeKind.TYPEVAR) {
      // SimpleVisitor-based so also handles captured type variables
      return asSuper(((TypeVariable)t).getUpperBound(), e); // RECURSIVE, potentially
    }
    final DefaultElement teKey = DefaultElement.of(te);
    final boolean added;
    synchronized (asSuperSeenTypes) {
      added = asSuperSeenTypes.add(teKey);
    }
    if (added) {
      try {
        TypeMirror x = asSuper(supertype(t), e); // RECURSIVE, potentially
        if (x != null) {
          return x;
        }
        if (isInterface(e)) {
          for (final TypeMirror iface : interfaces(t)) {
            if (iface.getKind() != TypeKind.ERROR) {
              x = asSuper(iface, e); // RECURSIVE, potentially
              if (x != null) {
                return x;
              }
            }
          }
        }
        return null; // Yes, really.
      } finally {
        synchronized (asSuperSeenTypes) {
          asSuperSeenTypes.remove(teKey);
        }
      }
    }
    return t;
  }

  private static final boolean bottomType(final TypeMirror t) {
    return t == null || t.getKind() == TypeKind.NULL; // TODO: I think
  }

  private static final NullType bottomType() {
    return DefaultNullType.INSTANCE;
  }

  // Renamed from classBound()
  // UnaryVisitor-based
  private static final TypeMirror boundingClass(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      final DeclaredType dt = (DeclaredType)t;
      final TypeMirror enclosingType = dt.getEnclosingType();
      final TypeMirror boundingClass = boundingClass(enclosingType);
      return enclosingType == boundingClass ? t : declaredType(boundingClass, dt.getTypeArguments(), dt.getAnnotationMirrors());
    case TYPEVAR:
      // UnaryVisitor-based so also handles captured type variables
      return boundingClass(supertype(t));
    case INTERSECTION:
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeElement boxedClass(final TypeMirror t) {
    // TODO: resume
    throw new UnsupportedOperationException();
  }
  
  private static final boolean canonical(final TypeMirror t) {
    final Element e = asElement(t);
    return e == null || Equality.equals(t, e.asType(), true);
  }

  @SuppressWarnings("unchecked")
  private static final <T extends TypeMirror> T canonicalType(final T t) {
    final Element e = asElement(t);
    return e == null ? t : (T)e.asType();
  }

  // Not visitor-based; no isCompound() semantics
  private static final TypeMirror capture(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return capture((DeclaredType)t);
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror capture(DeclaredType t) {
    assert t.getKind() == TypeKind.DECLARED;
    TypeMirror enclosingType = enclosingType(t);
    if (enclosingType.getKind() != TypeKind.NONE) {
      final TypeMirror capturedEnclosingType = capture(enclosingType); // RECURSIVE
      if (capturedEnclosingType != enclosingType) {
        final Element element = asElement(t);
        final TypeMirror memberType = memberType(capturedEnclosingType, element);
        t = (DeclaredType)subst(memberType, ((DeclaredType)element.asType()).getTypeArguments(), t.getTypeArguments());
        assert t.getKind() == TypeKind.DECLARED;
        enclosingType = t.getEnclosingType();
      }
    }

    if (raw(t) || !parameterized(t)) {
      return t;
    }

    final DeclaredType G = canonicalType(t);
    final List<? extends TypeMirror> A = G.getTypeArguments();
    final List<? extends TypeMirror> T = t.getTypeArguments();
    final List<? extends TypeMirror> S = withFreshCapturedTypeVariables(T);

    assert A.size() == T.size();
    assert A.size() == S.size();
    boolean captured = false;
    for (int i = 0; i < A.size(); i++) {
      final TypeMirror currentAHead = A.get(i);
      final TypeMirror currentSHead = S.get(i);
      final TypeMirror currentTHead = T.get(i);
      if (currentSHead != currentTHead) {
        captured = true;
        TypeMirror Ui = currentAHead instanceof TypeVariable tv ? tv.getUpperBound() : null;
        if (Ui == null) {
          Ui = DefaultDeclaredType.JAVA_LANG_OBJECT;
        }
        final CapturedType Si = (CapturedType)currentSHead;
        final WildcardType Ti = (WildcardType)currentTHead;
        Si.setLowerBound(Ti.getSuperBound());
        final TypeMirror TiExtendsBound = Ti.getExtendsBound();
        if (TiExtendsBound == null) {
          Si.setUpperBound(subst(Ui, A, S));
        } else {
          Si.setUpperBound(glb(TiExtendsBound, subst(Ui, A, S)));
        }
      }
    }
    if (captured) {
      return syntheticDeclaredType(t, S);
    }
    return t;
  }

  private static final boolean capturedTypeVariable(final TypeMirror t) {
    switch (t.getKind()) {
    case TYPEVAR:
      return t instanceof CapturedType;
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror capturedTypeVariableLowerBound(final TypeMirror t) {
    if (capturedTypeVariable(t)) {
      final TypeMirror lowerBound = ((TypeVariable)t).getLowerBound();
      if (lowerBound == null) {
        return nullType();
      } else if (lowerBound.getKind() == TypeKind.NULL) {
        return lowerBound;
      } else {
        return capturedTypeVariableLowerBound(lowerBound);
      }
    }
    return t;
  }

  private static final TypeMirror capturedTypeVariableUpperBound(final TypeMirror t) {
    if (capturedTypeVariable(t)) {
      final TypeMirror upperBound = ((TypeVariable)t).getUpperBound();
      assert upperBound != null;
      return capturedTypeVariableUpperBound(upperBound);
    }
    return t;
  }

  // Port of Types#isCaptureOf(Type, WildcardType)
  private static final boolean captures(final TypeMirror tv, final TypeMirror w) {
    return
      tv.getKind() == TypeKind.TYPEVAR &&
      tv instanceof CapturedType ct &&
      w.getKind() == TypeKind.WILDCARD &&
      captures(ct, (WildcardType)w);
  }

  private static final boolean captures(final CapturedType ct, final WildcardType w) {
    assert ct.getKind() == TypeKind.TYPEVAR;
    assert w.getKind() == TypeKind.WILDCARD;
    // isSameWildcard(), you'll note, does not check annotations.
    return Equality.equals(w, ct.getWildcardType(), false);
  }

  // Returns a MODIFIABLE list
  private static final List<TypeMirror> closure(final TypeMirror t) {
    List<TypeMirror> cl;
    final TypeMirror st = supertype(t);
    switch (t.getKind()) {
    case INTERSECTION:
      cl = closure(st); // RECURSIVE
      break;
    case DECLARED:
    case TYPEVAR:
      switch (st.getKind()) {
      case DECLARED:
        // (Yes, it is OK that INTERSECTION is not present as a case.)
        cl = closureInsert(closure(st), t); // RECURSIVE
        break;
      case TYPEVAR:
        cl = new ArrayList<>();
        cl.add(t);
        cl.addAll(closure(st)); // RECURSIVE
        break;
      case ERROR:
      case UNION:
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      break;
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror iface : interfaces(t)) {
      cl = closureUnion(cl, closure(iface)); // RECURSIVE
    }
    return cl;
  }

  // Returns a MODIFIABLE list
  private static final List<TypeMirror> closureInsert(final List<TypeMirror> closure, final TypeMirror t) {
    if (closure.isEmpty()) {
      closure.add(t);
      return closure;
    }
    final Element e = asElement(t, false);
    if (e == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    final Element headE = asElement(closure.get(0), false);
    if (headE == null) {
      throw new IllegalArgumentException("closure: " + closure);
    }
    if (!Equality.equals(e, headE, true)) {
      if (precedes(e, headE)) {
        closure.add(0, t);
      } else {
        closureInsert(closure.subList(1, closure.size()), t); // RECURSIVE
      }
    }
    return closure;
  }

  // Returns a MODIFIABLE list
  private static final List<TypeMirror> closureUnion(final List<TypeMirror> c1, final List<TypeMirror> c2) {
    if (c1.isEmpty()) {
      return c2;
    } else if (c2.isEmpty()) {
      return c1;
    }
    final TypeMirror head1 = c1.get(0);
    final Element head1E = asElement(head1, false);
    if (head1E == null) {
      throw new IllegalArgumentException("c1: " + c1);
    }
    final TypeMirror head2 = c2.get(0);
    final Element head2E = asElement(head2, false);
    if (head2E == null) {
      throw new IllegalArgumentException("c2: " + c2);
    }
    if (Equality.equals(head1E, head2E, true)) {
      closureUnion(c1.subList(1, c1.size()), c2.subList(1, c2.size())); // RECURSIVE
      return c1;
    } else if (precedes(head2E, head1E)) {
      closureUnion(c1, c2.subList(1, c2.size())); // RECURSIVE
      return c2;
    } else {
      closureUnion(c1.subList(1, c1.size()), c2); // RECURSIVE
      return c1;
    }
  }

  // See Types#elemtype(Type); bonkers bananas crazy town
  private static final TypeMirror componentType(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      // Straightforward and makes sense.
      return ((ArrayType)t).getComponentType();
    case EXECUTABLE:
      // For some really weird reason elemtype(ForAll) yields
      // elemtype(((ForAll)t).qtype), which reduces to
      // elemtype(MethodType) which returns null.  Why this isn't just
      // handled by the default clause is beyond me.  I separate it
      // here in case I've misread things.
      return null;
    case TYPEVAR:
      // It seems weird to me that elemtype(Type) would explicitly
      // handle wildcards but not type variables.  Maybe I'm not
      // thinking clearly.  Can't you have T extends Object[]?
      // Wouldn't you want the component type returned to be Object
      // rather than null?  I return null here because the case is not
      // handled in javac.
      return null;
    case WILDCARD:
      // I guess this just erases the wildcardness out of the mix and
      // (in most cases) will return null again, unless of course it's
      // an array?
      return componentType(wildcardUpperBound(t)); // RECURSIVE
    default:
      return null;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean contains(final List<? extends TypeMirror> ts,
                                        final List<? extends TypeMirror> ss) {
    final Iterator<? extends TypeMirror> ti = ts.iterator();
    final Iterator<? extends TypeMirror> si = ss.iterator();
    while (ti.hasNext() && si.hasNext() && contains(ti.next(), si.next())) {
      // do nothing
    }
    return !ti.hasNext() && !si.hasNext();
  }

  // Models Types#containsType(Type, Type), not Type#contains(Type)
  private static final boolean contains(final TypeMirror t, final TypeMirror s) {
    switch (t.getKind()) {
    case WILDCARD:
      return contains((WildcardType)t, s);
    default:
      return models(t, s);
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean contains(final WildcardType w, final TypeMirror s) {
    assert w.getKind() == TypeKind.WILDCARD;
    if (Equality.equals(w, s, true)) {
      return true;
    }
    if (captures(s, w)) {
      return true;
    }
    if (w.getSuperBound() == null) {
      return subtype(wildcardUpperBound(s), wildcardUpperBound(w), false);
    }
    return subtype(wildcardLowerBound(w), wildcardLowerBound(s), false);
  }

  // Models Types#containsTypeEquivalent(Type, Type).
  // I don't entirely understand what this method is for.
  private static final boolean containsEquivalent(final TypeMirror t, final TypeMirror s) {
    // What a mealy-mouthed method. So if t models s, we return true
    // (in which case this method doesn't have to exist) or if t is a
    // wildcard AND it contains s AND s is a wildcard AND it contains
    // t then we return true.
    return models(t, s) || contains(t, s) && contains(s, t);
  }

  private static final boolean containsEquivalent(final List<? extends TypeMirror> ts, final List<? extends TypeMirror> ss) {
    final Iterator<? extends TypeMirror> ti = ts.iterator();
    final Iterator<? extends TypeMirror> si = ss.iterator();
    while (ti.hasNext() && si.hasNext() && containsEquivalent(ti.next(), si.next())) {
      // do nothing
    }
    return !ti.hasNext() && !si.hasNext();
  }

  // Called by subtype(TypeMirror, TypeMirror, boolean)
  // Models Types#containsTypeRecursive(Type, Type)
  //
  // "Recursive" really just seems to mean that it plows into type
  // arguments.
  //
  // The original code puts entries into the cache that are clearly
  // never going to return true.  This version tries to avoid mutating
  // the cache as much as possible.
  private static final boolean containsRecursive(final TypeMirror t, final TypeMirror s) {
    final List<? extends TypeMirror> tTypeArguments = typeArguments(t);
    final List<? extends TypeMirror> sTypeArguments = typeArguments(s);
    if (tTypeArguments.isEmpty()) {
      return sTypeArguments.isEmpty();
    } else if (sTypeArguments.isEmpty()) {
      return false;
    }
    final TypeMirrorPair pair = new TypeMirrorPair(t, s);
    final boolean added;
    synchronized (containsRecursiveCache) {
      added = containsRecursiveCache.add(pair);
    }
    if (added) {
      try {
        return contains(tTypeArguments, sTypeArguments);
      } finally {
        synchronized (containsRecursiveCache) {
          containsRecursiveCache.remove(pair);
        }
      }
    } else {
      return contains(tTypeArguments, typeArguments(rewriteSuperBoundedWildcardTypes(s)));
    }
  }

  private static final boolean convertible(final TypeMirror t, final TypeMirror s) {
    if (isPrimitive(t)) {
      if (isPrimitive(s)) {
        return subtypeUnchecked(t, s, true);
      }
      return subtype(boxedClass(t).asType(), s, true);
    } else if (isPrimitive(s)) {
      return subtype(unboxedType(t), s, true);
    } else {
      return subtypeUnchecked(t, s, true);
    }
  }
  
  private static final DeclaredType declaredType(final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType(noneType(), List.of(), annotationMirrors);
  }

  private static final DeclaredType declaredType(final List<? extends TypeMirror> typeArguments,
                                                final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType(noneType(), typeArguments, annotationMirrors);
  }

  private static final DeclaredType declaredType(final DeclaredType enclosingType,
                                                final List<? extends TypeMirror> typeArguments,
                                                final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType((TypeMirror)enclosingType, typeArguments, annotationMirrors);
  }

  private static final DeclaredType declaredType(final TypeMirror enclosingType,
                                                 final List<? extends TypeMirror> typeArguments,
                                                 final List<? extends AnnotationMirror> annotationMirrors) {
    if ((enclosingType == null || enclosingType.getKind() == TypeKind.NONE) &&
        (annotationMirrors == null || annotationMirrors.isEmpty()) &&
        (typeArguments == null || typeArguments.isEmpty())) {
      return objectType();
    }
    switch (enclosingType.getKind()) {
    case DECLARED:
    case NONE:
      break;
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("enclosingType: " + enclosingType);
    }
    return new DefaultDeclaredType(enclosingType, typeArguments, annotationMirrors);
  }

  private static final TypeMirror enclosingType(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return ((DeclaredType)t).getEnclosingType();
    default:
      return noneType();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // StructuralTypeMapping-based
  private static final TypeMirror erase(final TypeMirror t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // Type erasure is a mapping from types (possibly including
    // parameterized types and type variables) to types (that are
    // never parameterized types or type variables). We write |T| for
    // the erasure of type T. The erasure mapping is defined as
    // follows:
    //
    // The erasure of a parameterized type (§4.5) G<T1,…,Tn> is |G|.
    //
    // The erasure of a nested type T.C is |T|.C.
    //
    // The erasure of an array type T[] is |T|[].
    //
    // The erasure of a type variable (§4.4) is the erasure of its
    // leftmost bound.
    //
    // The erasure of every other type is the type itself.
    if (t == null) {
      return null;
    }
    switch (t.getKind()) {
    case ARRAY:
      return erase((ArrayType)t);
    case DECLARED:
      return erase((DeclaredType)t);
    case TYPEVAR:
      return erase((TypeVariable)t);
    case WILDCARD:
      // This seems to be what the compiler does, even though it
      // isn't discussed in the JLS excerpt above.
      return erase((WildcardType)t);
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // The original is StructuralTypeMapping-based, which means identity
  // is important in the return value.
  private static final List<? extends TypeMirror> erase(final List<? extends TypeMirror> ts) {
    if (ts.isEmpty()) {
      return ts;
    }
    boolean changed = false;
    final List<TypeMirror> newTs = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      final TypeMirror erasedT = erase(t);
      newTs.add(erasedT);
      if (!changed && t != erasedT) {
        changed = true;
      }
    }
    return changed ? Collections.unmodifiableList(newTs) : ts;
  }

  private static final DeclaredType erase(final DeclaredType t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of a parameterized type (§4.5) G<T1,…,Tn> is |G|.
    //
    // The erasure of a nested type T.C is |T|.C.
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror enclosingType = t.getEnclosingType();
    switch (enclosingType.getKind()) {
    case DECLARED:
      return declaredType(erase((DeclaredType)enclosingType), List.of(), t.getAnnotationMirrors());
    case NONE:
      return t.getTypeArguments().isEmpty() ? t : declaredType(enclosingType, List.of(), t.getAnnotationMirrors());
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror erase(final ArrayType t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of an array type T[] is |T|[].
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror ct = t.getComponentType();
    final TypeMirror ect = erase(ct);
    return ct == ect ? t : arrayType(ect, t.getAnnotationMirrors());
  }

  // StructuralTypeMapping-based, so this also handles captured type variables
  private static final TypeMirror erase(final TypeVariable t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of a type variable (§4.4) is the erasure of its
    // leftmost bound.
    assert t.getKind() == TypeKind.TYPEVAR;
    return erase(t.getUpperBound());
  }

  private static final TypeMirror erase(final WildcardType t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of a type variable (§4.4) is the erasure of its
    // leftmost bound.
    //
    // [I guess this is one of those cases where the compiler treats a
    // wildcard as a type variable. A wildcard type is not a type in
    // the JLS.]
    //
    // [Consider ? extends T, where T is a type variable that extends
    // Runnable.  The erasure, once all is said and done, of ? extends
    // T will be Runnable.]
    assert t.getKind() == TypeKind.WILDCARD;
    return erase(wildcardUpperBound(t));
  }

  private static final ExecutableType executableType(final List<? extends TypeMirror> parameterTypes,
                                                     final TypeMirror receiverType,
                                                     final TypeMirror returnType,
                                                     final List<? extends TypeMirror> thrownTypes,
                                                     final List<? extends TypeVariable> typeVariables,
                                                     final List<? extends AnnotationMirror> annotationMirrors) {
    return
      DefaultExecutableType.of(parameterTypes,
                               receiverType,
                               returnType,
                               thrownTypes,
                               typeVariables,
                               annotationMirrors);
  }

  private static final Name flatName(final Element e) {
    /*
        // form a fully qualified name from a name and an owner, after
        // converting to flat representation
        public static Name formFlatName(Name name, Symbol owner) {
            if (owner == null || owner.kind.matches(KindSelector.VAL_MTH) ||
                (owner.kind == TYP && owner.type.hasTag(TYPEVAR))
                ) return name;
            char sep = owner.kind == TYP ? '$' : '.';
            Name prefix = owner.flatName();
            if (prefix == null || prefix == prefix.table.names.empty)
                return name;
            else return prefix.append(sep, name);
        }
    */
    final Element enclosingElement = e.getEnclosingElement();
    if (enclosingElement == null) {
      return e.getSimpleName();
    }
    char separator;
    switch (enclosingElement.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      switch (enclosingElement.asType().getKind()) {
      case TYPEVAR:
        return e.getSimpleName();
      default:
        separator = '$';
        break;
      case ERROR:
      case UNION:
        throw new IllegalStateException("enclosingElement: " + enclosingElement);
      }
      break;
    case CONSTRUCTOR:
    case METHOD:
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return e.getSimpleName();
    default:
      separator = '.';
      break;
    }
    final Name prefix = flatName(enclosingElement); // RECURSIVE
    if (prefix == null || prefix.isEmpty()) {
      return e.getSimpleName();
    }
    return DefaultName.of(new StringBuilder(prefix).append(separator).append(e.getSimpleName().toString()).toString());
  }

  private static final TypeMirror glb(final List<? extends TypeMirror> ts) {
    final int size = ts.size();
    TypeMirror t;
    switch (size) {
    case 0:
      throw new IllegalArgumentException("ts.isEmpty()");
    default:
      t = ts.get(0);
      break;
    }
    for (int i = 1; i < size; i++) {
      t = glb(t, ts.get(i));
    }
    return t;
  }

  // "greatest lower bound"
  // Not visitor-based
  // Combines glb and glbFlattened
  private static final TypeMirror glb(final TypeMirror t, final TypeMirror s) {
    if (s == null) {
      return t;
    } else if (isPrimitive(t)) {
      throw new IllegalArgumentException("t: " + t);
    } else if (isPrimitive(s)) {
      throw new IllegalArgumentException("s: " + s);
    } else if (subtype(t, s, false)) {
      return t;
    } else if (subtype(s, t, false)) {
      return s;
    }
    final ArrayList<TypeMirror> bounds = new ArrayList<>(minimumTypes(closureUnion(closure(t), closure(s))));
    final int size = bounds.size();
    switch (size) {
    case 0:
      return objectType();
    case 1:
      return bounds.get(0);
    default:
      int classCount = 0;
      ArrayList<TypeMirror> capturedTypeVariables = new ArrayList<>();
      ArrayList<TypeMirror> lowers = new ArrayList<>();
      for (final TypeMirror bound : bounds) {
        if (!isInterface(bound)) {
          classCount++;
          final TypeMirror lower = capturedTypeVariableLowerBound(bound);
          if (bound != lower && lower.getKind() != TypeKind.NULL) {
            capturedTypeVariables.add(bound);
            lowers.add(lower);
          }
        }
      }
      if (classCount > 1) {
        if (lowers.isEmpty()) {
          throw new IllegalArgumentException("t: " + t);
        }
        bounds.removeIf(capturedTypeVariables::contains);
        bounds.addAll(lowers);
        return glb(bounds); // RECURSIVE, in a way
      }
    }
    return intersectionType(bounds);
  }

  private static final boolean hasTypeArguments(final TypeMirror t) {
    // This is modeled after javac's allparams() method.  javac
    // frequently confuses type parameters and type arguments in its
    // terminology.  This implementation could probably be made more
    // efficient. See
    // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1137
    switch (t.getKind()) {
    case ARRAY:
    case DECLARED:
      return !allTypeArguments(t).isEmpty();
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // UnaryVisitor-based
  private static final List<? extends TypeMirror> interfaces(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return interfaces((DeclaredType)t);
    case INTERSECTION:
      return interfaces((IntersectionType)t);
    case TYPEVAR:
      return interfaces((TypeVariable)t);
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final List<? extends TypeMirror> interfaces(final DeclaredType t) {
    assert t.getKind() == TypeKind.DECLARED;
    final Element e = asElement(t);
    if (e == null) {
      return List.of();
    }
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      final List<? extends TypeMirror> interfaces = ((TypeElement)e).getInterfaces();
      if (raw(t)) {
        return erase(interfaces);
      }
      final List<? extends TypeMirror> formals = allTypeArguments(canonicalType(t));
      return formals.isEmpty() ? interfaces : subst(interfaces, formals, allTypeArguments(t));
    default:
      return List.of();
    }
  }

  private static final List<? extends TypeMirror> interfaces(final IntersectionType t) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // Here the porting is a little trickier.  It turns out that an
    // intersection type caches its supertype and its interfaces at
    // construction time, and there's only one place where
    // intersection types are created.  In the lang model, that means
    // that an IntersectionType's bounds are its supertype followed by
    // its interfaces.  So we will hand-tool this.
    final List<? extends TypeMirror> bounds = t.getBounds();
    final int size = bounds.size();
    switch (size) {
    case 0:
      // (Technically an illegal state.)
      return List.of();
    case 1:
      return isInterface(bounds.get(0)) ? bounds : List.of();
    default:
      return isInterface(bounds.get(0)) ? bounds : bounds.subList(1, size);
    }
  }

  // UnaryVisitor-based so also handles captured type variables
  private static final List<? extends TypeMirror> interfaces(final TypeVariable t) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    switch (upperBound.getKind()) {
    case DECLARED:
      return ((DeclaredType)upperBound).asElement().getKind().isInterface() ? List.of(upperBound) : List.of();
    case INTERSECTION:
      return interfaces(upperBound);
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // Note that bounds order is extremely, extremely important as it
  // turns out that the supertype of an intersection type, which is
  // not defined in the JLS, is apparently, in javac, its first bound.
  private static final IntersectionType intersectionType(final List<? extends TypeMirror> bounds) {
    return DefaultIntersectionType.of(bounds);
  }

  private static final boolean isInterface(final Element e) {
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case INTERFACE:
      assert e.getKind().isInterface();
      return true;
    default:
      return false;
    }
  }

  private static final boolean isInterface(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return isInterface(((DeclaredType)t).asElement());
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean isPrimitive(final Element e) {
    return isPrimitive(e.asType());
  }

  private static final boolean isPrimitive(final TypeMirror t) {
    return t.getKind().isPrimitive();
  }

  private static final boolean isStatic(final Element e) {
    return e.getModifiers().contains(Modifier.STATIC);
  }

  private static final WildcardType lowerBoundedWildcardType(final TypeMirror lowerBound,
                                                             final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.lowerBoundedWildcardType(lowerBound, annotationMirrors);
  }

  // SimpleVisitor-based
  private static final TypeMirror memberType(final TypeMirror t, final Element e) {
    switch (t.getKind()) {
    case DECLARED:
      return memberType((DeclaredType)t, e);
    case INTERSECTION:
      return memberType((IntersectionType)t, e);
    case TYPEVAR:
      return memberType((TypeVariable)t, e);
    case WILDCARD:
      return memberType((WildcardType)t, e);
    default:
      return e.asType();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror memberType(final DeclaredType t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED;
    return memberType(t, e, Types::asOuterSuper);
  }

  private static final TypeMirror memberType(final ErrorType t, final Element e) {
    assert t.getKind() == TypeKind.ERROR;
    throw new UnsupportedOperationException();
  }

  private static final TypeMirror memberType(final IntersectionType t, final Element e) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return memberType(t, e, (t1, e1) -> capture(asOuterSuper(t1, e1)));
  }

  private static final TypeMirror memberType(final TypeMirror t, final Element e, final BiFunction<? super TypeMirror, ? super Element, ? extends TypeMirror> asOuterSuperFunction) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    if (!isStatic(e)) {
      final Element enclosingElement = e.getEnclosingElement();
      final TypeMirror enclosingType = enclosingElement.asType();
      if (parameterized(enclosingType)) {
        final TypeMirror baseType = asOuterSuperFunction.apply(t, enclosingElement);
        if (baseType != null) {
          final List<? extends TypeMirror> enclosingTypeTypeArguments = allTypeArguments(enclosingType);
          if (!enclosingTypeTypeArguments.isEmpty()) {
            final List<? extends TypeMirror> baseTypeTypeArguments = allTypeArguments(baseType);
            if (baseTypeTypeArguments.isEmpty()) {
              // baseType is raw
              return erase(e.asType());
            } else {
              return subst(e.asType(), enclosingTypeTypeArguments, baseTypeTypeArguments);
            }
          }
        }
      }
    }
    return e.asType();
  }

  // SimpleVisitor-based so works for captured type variables too.
  private static final TypeMirror memberType(final TypeVariable t, final Element e) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return memberType(t.getUpperBound(), e); // RECURSIVE
  }

  private static final TypeMirror memberType(final WildcardType w, final Element e) {
    assert w.getKind() == TypeKind.WILDCARD;
    return memberType(wildcardUpperBound(w), e); // RECURSIVE
  }

  // compoundMin
  private static final TypeMirror minimumType(final List<? extends TypeMirror> ts) {
    if (ts.isEmpty()) {
      return objectType();
    }
    final List<? extends TypeMirror> minimum = minimumTypes(ts);
    final int size = minimum.size();
    switch (size) {
    case 0:
      // From the documentation of lub(), which calls compoundMin():
      // "If the lub does not exist return the type of null (bottom)."
      return bottomType();
    case 1:
      return minimum.get(0);
    default:
      return intersectionType(minimum);
    }
  }

  // closureMin
  private static final List<? extends TypeMirror> minimumTypes(final List<? extends TypeMirror> ts) {
    final int size = ts.size();
    if (size <= 1) {
      return ts;
    }
    final ArrayList<TypeMirror> classes = new ArrayList<>(7);
    final ArrayList<TypeMirror> interfaces = new ArrayList<>(7);
    final Set<DefaultTypeMirror> skip = new HashSet<>();
    OUTER_LOOP:
    for (int i = 0; i < size; i++) {
      final Element e;
      final TypeMirror t = ts.get(i);
      switch (t.getKind()) {
      case DECLARED:
        e = ((DeclaredType)t).asElement();
        break;
      case TYPEVAR:
        e = ((TypeVariable)t).asElement();
        break;
      case ERROR:
      case UNION:
      default:
        throw new IllegalArgumentException("ts: " + ts);
      }
      if (skip.contains(DefaultTypeMirror.of(t)) && t.getKind() == TypeKind.TYPEVAR) {
        for (int j = 0; j < size; j++) {
          if (subtype(ts.get(j), t, false)) {
            continue OUTER_LOOP;
          }
        }
      }
      if (e.getKind().isInterface()) {
        interfaces.add(t);
      } else {
        classes.add(t);
      }
      for (int j = 0; j < size; j++) {
        final TypeMirror candidateSupertype = ts.get(j);
        if (subtype(t, candidateSupertype, false)) {
          skip.add(DefaultTypeMirror.of(candidateSupertype));
        }
      }
    }
    if (classes.isEmpty()) {
      if (interfaces.isEmpty()) {
        return List.of();
      }
      interfaces.trimToSize();
      return Collections.unmodifiableList(interfaces);
    } else {
      if (!interfaces.isEmpty()) {
        classes.addAll(interfaces);
      }
      classes.trimToSize();
      return Collections.unmodifiableList(classes);
    }
  }

  // Port of isSameType()
  // TypeRelation-based
  private static final boolean models(final TypeMirror t, final TypeMirror s) {
    final TypeKind tk = t.getKind();
    switch (tk) {
    case ARRAY:
      return models((ArrayType)t, s);
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case NONE:
    case NULL:
    case SHORT:
    case VOID:
      return tk == s.getKind();
    case DECLARED:
      return models((DeclaredType)t, s);
    case EXECUTABLE:
      return models((ExecutableType)t, s);
    case INTERSECTION:
      return models((IntersectionType)t, s);
    case PACKAGE:
      // I wonder why this is called out/allowed.
      return Equality.equals(t, s, true);
    case TYPEVAR:
      return models((TypeVariable)t, s);
    case WILDCARD:
      return models((WildcardType)t, s);
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean models(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    return s.getKind() == TypeKind.ARRAY && models(t, (ArrayType)s);
  }

  private static final boolean models(final ArrayType t, final ArrayType s) {
    assert t.getKind() == TypeKind.ARRAY;
    assert s.getKind() == TypeKind.ARRAY;
    return containsEquivalent(t.getComponentType(), componentType(s));
  }

  private static final boolean models(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    switch (s.getKind()) {
    case WILDCARD:
      return models(t, (WildcardType)s);
    default:
      return
        Equality.equals(asElement(t, false), asElement(s, true), true) &&
        models(enclosingType(t), enclosingType(s)) &&
        containsEquivalent(typeArguments(t), typeArguments(s));
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);

    }
  }

  private static final boolean models(final DeclaredType t, final WildcardType s) {
    assert t.getKind() == TypeKind.DECLARED;
    assert s.getKind() == TypeKind.WILDCARD;
    if (s.getSuperBound() != null) {
      return
        models(t, wildcardUpperBound(s)) &&
        models(t, wildcardLowerBound(s));
    }
    return
      Equality.equals(asElement(t, false), asElement(s, true), true) &&
      models(enclosingType(t), enclosingType(s)) &&
      containsEquivalent(typeArguments(t), typeArguments(s));
  }

  private static final boolean models(final ExecutableType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    return s.getKind() == TypeKind.EXECUTABLE && models(t, (ExecutableType)s);
  }

  private static final boolean models(final ExecutableType t, final ExecutableType s) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    assert s.getKind() == TypeKind.EXECUTABLE;
    return
      modelsTypeVariablesOf(t, s) &&
      models(t, subst(s, s.getTypeVariables(), t.getTypeVariables())) &&
      modelsParametersOf(t, s) &&
      models(t.getReturnType(), s.getReturnType());
  }

  private static final boolean models(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    switch (s.getKind()) {
    case INTERSECTION:
      return models(t, (IntersectionType)s);
    default:
      return
        Equality.equals(asElement(t, false), asElement(s, true), true) &&
        models(enclosingType(t), enclosingType(s)) &&
        containsEquivalent(typeArguments(t), typeArguments(s));
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    }
  }

  private static final boolean models(final IntersectionType t, final IntersectionType s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    assert s.getKind() == TypeKind.INTERSECTION;
    if (!models(supertype(t), supertype(s))) {
      return false;
    }

    final Map<DefaultElement, TypeMirror> map = new HashMap<>();
    for (final TypeMirror tiface : interfaces(t)) {
      map.put(DefaultElement.of(asElement(tiface, true)), tiface);
    }
    for (final TypeMirror siface : interfaces(s)) {
      final TypeMirror tiface = map.remove(DefaultElement.of(asElement(siface, true)));
      if (tiface == null || !models(tiface, siface)) {
        return false;
      }
    }
    return map.isEmpty();
  }

  private static final boolean models(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    switch (s.getKind()) {
    case TYPEVAR:
      return Equality.equals(t, s, true);
    case WILDCARD:
      return models(t, (WildcardType)s);
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    }
  }

  private static final boolean models(final TypeVariable t, final WildcardType s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    assert s.getKind() == TypeKind.WILDCARD;
    return s.getExtendsBound() == null && models(t, wildcardUpperBound(s));
  }

  private static final boolean models(final WildcardType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.WILDCARD;
    return s.getKind() == TypeKind.WILDCARD && models(t, (WildcardType)s);
  }

  private static final boolean models(final WildcardType t, final WildcardType s) {
    assert t.getKind() == TypeKind.WILDCARD;
    assert s.getKind() == TypeKind.WILDCARD;
    final TypeMirror tLower = t.getSuperBound();
    if (tLower == null) {
      final TypeMirror tUpper = t.getExtendsBound();
      final TypeMirror sLower = s.getSuperBound();
      final TypeMirror sUpper = s.getExtendsBound();
      if (tUpper == null) {
        if (sLower == null) {
          return sUpper == null || models(objectType(), sUpper);
        }
      } else if (sLower == null) {
        return models(tUpper, sUpper == null ? objectType() : sUpper);
      }
    }
    return false;
  }

  // Port of Types#hasSameArgs(Type, Type)
  private static final boolean modelsParametersOf(final ExecutableType t, final ExecutableType s) {
    if (t.getKind() != TypeKind.EXECUTABLE || t.getKind() != TypeKind.EXECUTABLE) {
      return false;
    }
    return
      containsEquivalent(t.getParameterTypes(), s.getParameterTypes()) &&
      modelsTypeVariablesOf(t, s) &&
      modelsParametersOf(t, subst(s, s.getTypeVariables(), t.getTypeVariables())); // RECURSIVE
  }

  // Port of Types#hasSameBounds(ForAll, ForAll)
  private static final boolean modelsTypeVariablesOf(final ExecutableType t, final ExecutableType s) {
    if (t.getKind() != TypeKind.EXECUTABLE || t.getKind() != TypeKind.EXECUTABLE) {
      return false;
    }
    final List<? extends TypeVariable> tvt = t.getTypeVariables();
    final List<? extends TypeVariable> tvs = s.getTypeVariables();
    final Iterator<? extends TypeVariable> ti = tvt.iterator();
    final Iterator<? extends TypeVariable> si = tvs.iterator();
    while (ti.hasNext() &&
           si.hasNext() &&
           models(ti.next().getUpperBound(),
                  subst(si.next().getUpperBound(),
                        tvs,
                        tvt))) {
      // do nothing
    }
    return !ti.hasNext() && !si.hasNext();
  }

  private static final NoType noneType() {
    return DefaultNoType.NONE;
  }

  private static final NullType nullType() {
    return DefaultNullType.INSTANCE;
  }

  private static final DeclaredType objectType() {
    return DefaultDeclaredType.JAVA_LANG_OBJECT;
  }

  private static final boolean parameterized(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return parameterized(((ArrayType)t).getComponentType());
    case DECLARED:
      return !allTypeArguments(t).isEmpty();
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // Symbol#precedes
  private static final boolean precedes(final Element e, final Element f) {
    final TypeMirror t = e.asType();
    final TypeMirror s = f.asType();
    switch (t.getKind()) {
    case DECLARED:
      switch (s.getKind()) {
      case DECLARED:
        if (Equality.equals(e, f, true)) {
          return false;
        }
        final int rt = rank(t);
        final int rs = rank(s);
        return
          rs < rt ||
          rs == rt &&
          ((TypeElement)f).getQualifiedName().toString().compareTo(((TypeElement)e).getQualifiedName().toString()) < 0;
      case TYPEVAR:
        return true;
      case ERROR:
      case UNION:
      default:
        throw new IllegalArgumentException("f: " + f);
      }
    case TYPEVAR:
      switch (s.getKind()) {
      case TYPEVAR:
        if (Equality.equals(e, f, true)) {
          return false;
        }
        return subtype(t, s, true);
      default:
        return true;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("f: " + f);
      }
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("e: " + e);
    }
  }

  // Should only accept reference types, or so says javac, but then it
  // has a case to deal with the NONE type, and if you were to pass an
  // intersection type to it you'd get an NPE.  Additionally, the null
  // type is (maybe? the spec is unclear?) a reference type, but it
  // isn't accepted by javac.  Who knows what it accepts.
  @SuppressWarnings("fallthrough")
  private static final int rank(final TypeMirror t) {
    int r = 0;
    switch (t.getKind()) {
    case DECLARED:
      if (((TypeElement)((DeclaredType)t).asElement()).getQualifiedName().contentEquals("java.lang.Object")) {
        return 0;
      }
      // fall through
    case INTERSECTION:
    case TYPEVAR:
      r = rank(supertype(t));
      for (final TypeMirror iface : interfaces(t)) {
        r = Math.max(r, rank(iface));
      }
      return r + 1;
    case NONE:
      return 0;
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean raw(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return raw((ArrayType)t);
    case DECLARED:
      return raw((DeclaredType)t);
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // See
  // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1413-L1415
  private static final boolean raw(final ArrayType t) {
    assert t.getKind() == TypeKind.ARRAY;
    return raw(t.getComponentType());
  }

  // I'm mostly parroting this.  I don't *really* know what it does.
  // The compiler does this:
  //
  // For a ClassType:
  //
  //  return
  //      this != tsym.type && // the ClassType in question (this) is synthetic and
  //      tsym.type.allparams().nonEmpty() && // the canonical type "under" it has type arguments and
  //      allparams().isEmpty(); // the ClassType in question (this) has no type arguments
  //
  // So along the way a ClassType was synthesized, and it deliberately
  // did not have its type arguments set.
  //
  // See
  // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1154-L1164
  private static final boolean raw(final DeclaredType t) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror canonicalType = canonicalType(t);
    return
      t != canonicalType && // t is synthetic and
      hasTypeArguments(canonicalType) && // the canonical type has type arguments and
      !hasTypeArguments(t); // t does not have type arguments
  }

    /**
   * Returns {@code true} if {@code t1} <em>references</em> {@code
   * t2}.
   *
   * <p>Loosely speaking: does {@code t1} have a {@link TypeMirror}
   * affiliated with it that is {@linkplain
   * Identity#identical(TypeMirror, TypeMirror, boolean) identical} to
   * {@code t2}?</p>
   *
   * @param t1 a {@link TypeMirror}; must not be {@code null}
   *
   * @param t2 another {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if {@code t1} references {@code t2}
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if a {@link WildcardType} is
   * encountered that has both an upper and a lower bound
   */
  // Not visitor-based.
  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final TypeMirror t1, final TypeMirror t2) {
    switch (t1.getKind()) {
    case ARRAY:
      return references((ArrayType)t1, t2);
    case DECLARED:
      return references((DeclaredType)t1, t2);
    case EXECUTABLE:
      return references((ExecutableType)t1, t2);
    case INTERSECTION:
      return references((IntersectionType)t1, t2);
    case WILDCARD:
      return references((WildcardType)t1, t2);
    default:
      return Equality.equals(t1, t2, false);
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t1: " + t1);
    }
  }

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final ArrayType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.ARRAY;
    return
      Equality.equals(t1, t2, false) ||
      references(t1.getComponentType(), t2); // RECURSIVE
  }

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final DeclaredType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.DECLARED;
    return
      Equality.equals(t1, t2, false) ||
      (hasTypeArguments(t1) && (references(t1.getEnclosingType(), t2) || anyIdentical(t1.getTypeArguments(), t2))); // RECURSIVE
  }

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final ExecutableType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.EXECUTABLE;
    return
      Equality.equals(t1, t2, false) ||
      anyIdentical(t1.getParameterTypes(), t2) ||
      references(t1.getReturnType(), t2) || // RECURSIVE
      anyIdentical(t1.getThrownTypes(), t2);
  }

  // Interestingly, the compiler does *not* test for equality/identity
  // in these three cases.  It makes a certain amount of sense when
  // you consider that all three of these are effectively just
  // collections of types with no real identity of their own.

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final IntersectionType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.INTERSECTION;
    return anyIdentical(t1.getBounds(), t2);
  }

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final UnionType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.UNION;
    return anyIdentical(t1.getAlternatives(), t2);
  }

  // Models Type#contains(Type), not Types#containsType(Type, Type).
  private static final boolean references(final WildcardType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.WILDCARD;
    final TypeMirror upperBound = t1.getExtendsBound();
    final TypeMirror lowerBound = t1.getSuperBound();
    if (upperBound == null) {
      return lowerBound != null && references(lowerBound, t2); // RECURSIVE
    } else if (lowerBound == null) {
      return references(upperBound, t2); // RECURSIVE
    } else {
      throw new IllegalArgumentException("t1: " + t1);
    }
  }

  // Does t reference at least one s in ss?
  private static final boolean referencesAny(final TypeMirror t,
                                             final Collection<? extends TypeMirror> ss) {
    if (!ss.isEmpty()) {
      for (final TypeMirror s : ss) {
        if (references(t, s)) {
          return true;
        }
      }
    }
    return false;
  }

  // Ported slavishly from javac.
  private static final TypeMirror rewriteSuperBoundedWildcardTypes(final TypeMirror t) {
    if (parameterized(t)) {
      List<TypeMirror> from = new ArrayList<>();
      List<TypeMirror> to = new ArrayList<>();
      adaptSelf(t, from, to);
      if (!from.isEmpty()) {
        final List<TypeMirror> rewrite = new ArrayList<>();
        boolean changed = false;
        for (final TypeMirror orig : to) {
          TypeMirror s = rewriteSuperBoundedWildcardTypes(orig); // RECURSIVE
          switch (s.getKind()) {
          case WILDCARD:
            if (((WildcardType)s).getSuperBound() != null) {
              // TODO: maybe need to somehow ensure this shows up as
              // non-canonical/synthetic
              s = unboundedWildcardType(s.getAnnotationMirrors());
              changed = true;
            }
            break;
          default:
            if (s != orig) { // Don't need Equality.equals() here
              // TODO: maybe need to somehow ensure this shows up as
              // non-canonical/synthetic
              s = upperBoundedWildcardType(wildcardUpperBound(s), s.getAnnotationMirrors());
              changed = true;
            }
            break;
          case ERROR:
          case UNION:
            throw new IllegalStateException("s: " + s);
          }
          rewrite.add(s);
        }
        if (changed) {
          return subst(canonicalType(t), from, rewrite);
        }
      }
    }
    return t;
  }

  // StructuralTypeMapping-based.
  //
  // TODO: There's "needsStripping", which I don't know what it does:
  // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L356-L361
  // needsStripping() seems to be used by javac's utility packages
  // that implement javax.lang.model.*.  I think the intent is to make
  // it so that
  // synthetic-types-wrapping-symbols-wrapping-canonical-types always
  // report zero annotations, which is also a way to tell that they're
  // synthetic.
  private static final TypeMirror subst(final TypeMirror t, List<? extends TypeMirror> from, List<? extends TypeMirror> to) {
    int fromSize = from.size();
    int toSize = to.size();
    if (fromSize != toSize) {
      if (fromSize > toSize) {
        from = from.subList(fromSize - toSize, fromSize);
        fromSize = from.size();
      }
      if (toSize > fromSize) {
        to = to.subList(toSize - fromSize, toSize);
        toSize = to.size();
      }
      assert fromSize == toSize;
    }
    switch (t.getKind()) {
    case ARRAY:
      return subst((ArrayType)t, from, to);
    case DECLARED:
      return subst((DeclaredType)t, from, to);
    case EXECUTABLE:
      return subst((ExecutableType)t, from, to);
    case INTERSECTION:
      return subst((IntersectionType)t, from, to);
    case TYPEVAR:
      return subst((TypeVariable)t, from, to);
    case WILDCARD:
      return subst((WildcardType)t, from, to);
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final List<? extends TypeMirror> subst(final List<? extends TypeMirror> types,
                                                        final List<? extends TypeMirror> from,
                                                        final List<? extends TypeMirror> to) {
    assert from.size() == to.size();
    if (types.isEmpty()) {
      return types;
    }
    final List<TypeMirror> list = new ArrayList<>(types.size());
    boolean changed = false;
    for (final TypeMirror t : types) {
      final TypeMirror x = subst(t, from, to); // RECURSIVE
      list.add(x);
      if (!changed) {
        changed = t != x;
      }
    }
    return changed ? Collections.unmodifiableList(list) : list;
  }

  private static final ArrayType subst(final ArrayType t,
                                       final List<? extends TypeMirror> from,
                                       final List<? extends TypeMirror> to) {
    assert t.getKind() == TypeKind.ARRAY;
    assert from.size() == to.size();
    final TypeMirror componentType = t.getComponentType();
    final TypeMirror x = subst(componentType, from, to); // RECURSIVE
    if (componentType == x) {
      return t;
    }
    return arrayType(componentType, t.getAnnotationMirrors());
  }

  private static final DeclaredType subst(final DeclaredType t,
                                          final List<? extends TypeMirror> from,
                                          final List<? extends TypeMirror> to) {
    assert t.getKind() == TypeKind.DECLARED;
    assert from.size() == to.size();

    final TypeMirror enclosingType = t.getEnclosingType();
    final TypeMirror substEnclosingType = subst(enclosingType, from, to); // RECURSIVE

    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    final List<? extends TypeMirror> substTypeArguments = subst(typeArguments, from, to); // RECURSIVE

    if (enclosingType == substEnclosingType &&
        typeArguments == substTypeArguments) {
      return t;
    }
    return declaredType(substEnclosingType, substTypeArguments, t.getAnnotationMirrors());
  }

  private static final ExecutableType subst(ExecutableType t,
                                            final List<? extends TypeMirror> from,
                                            final List<? extends TypeMirror> to) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    assert from.size() == to.size();

    // TODO: OK, the question is, as we de-visitor-ize subst(), does
    // the structural type mapping on executable types happen *before*
    // the specific subst() action on executable types, or does it
    // happen *after*?
    //
    // We start with Types.Subst.visitForAll().  Here, ForAll is
    // javac's name for an ExecutableType that has type variables.
    // (No such distinction between a generic method and a non-generic
    // method is made in the language model's ExecutableType
    // construct.)
    //
    // visitForAll() overrides StructuralTypeMapping.visitForAll().
    //
    // There is no super.visitForAll() in the original code but keep
    // reading.
    //
    // So visitForAll() *starts* with the specific behavior:
    //
    //  @Override // StructuralTypeMapping
    //  public Type visitForAll(ForAll t, Void ignored) {
    //      if (Type.containsAny(to, t.tvars)) {
    //          //perform alpha-renaming of free-variables in 't'
    //          //if 'to' types contain variables that are free in 't'
    //          List<Type> freevars = newInstances(t.tvars);
    //          t = new ForAll(freevars,
    //                         Types.this.subst(t.qtype, t.tvars, freevars));
    //      }
    //
    // (For more on "alpha-renaming [sic]" and "free-variables [sic]"
    // see
    // https://web.stanford.edu/class/cs242/materials/lectures/lecture04.pdf.)
    //
    // Note that t is at this point not necessarily the same t that
    // was passed in.
    //
    // Then it calls substBounds():
    //
    //      List<Type> tvars1 = substBounds(t.tvars, from, to);
    //
    // Next, it calls visit(), which is not overridden:
    //
    //      Type qtype1 = visit(t.qtype); // equal to super.visit(); see MapVisitor
    //
    // Let's try to unpack this.
    //
    // "qtype" is probably short for "quantified type", and represents
    // a javac MethodType, which is the thing presumably universally
    // quantified by a ForAll.
    //
    // The language model makes no distinction between these two
    // entities.
    //
    // So *first* we should do the alpha conversion/renaming:
    List<? extends TypeVariable> typeVariables = t.getTypeVariables();
    if (anyReferencesAny(to, typeVariables)) {
      t = executableType(t.getParameterTypes(),
                         t.getReceiverType(),
                         t.getReturnType(),
                         t.getThrownTypes(),
                         typeVariables(typeVariables), // NOTE; alpha conversion
                         t.getAnnotationMirrors());
      // Ensure we "pick up" the alpha conversion
      typeVariables = t.getTypeVariables();
    }

    // Now call substBounds() just like the compiler:
    final List<? extends TypeVariable> substTypeVariables = substBounds(typeVariables, from, to);

    // Now we've done the renaming so it's time to translate/port the
    // visit() call:
    //
    //     Type qtype1 = visit(t.qtype);
    //
    // So in the original code, this calls visit() directly because it
    // wants to run the visit() operation on a MethodType, not a
    // ForAll.  So we harvest the structural type mapping stuff for
    // MethodType only below.  This avoids doing that alpha renaming
    // step above again.

    final List<? extends TypeMirror> parameterTypes = t.getParameterTypes();
    final List<? extends TypeMirror> substParameterTypes = subst(parameterTypes, from, to); // RECURSIVE

    final TypeMirror returnType = t.getReturnType();
    final TypeMirror substReturnType = subst(returnType, from, to); // RECURSIVE

    final List<? extends TypeMirror> thrownTypes = t.getThrownTypes();
    final List<? extends TypeMirror> substThrownTypes = subst(thrownTypes, from, to); // RECURSIVE

    // We've done the substitutions represented by the "visit()" call.
    // Did they result in a new type?

    if (parameterTypes == substParameterTypes &&
        returnType == substReturnType &&
        thrownTypes == substThrownTypes) {
      if (typeVariables == substTypeVariables) {
        return t;
      }
    } else {
      t = executableType(substParameterTypes,
                         t.getReceiverType(),
                         substReturnType,
                         substThrownTypes,
                         typeVariables,
                         t.getAnnotationMirrors());
      assert typeVariables == t.getTypeVariables();
      if (typeVariables == substTypeVariables) {
        return t;
      }
    }
    // If we get here, we know that type variables changed.  Other
    // things may have changed, but t will reflect that already.  So
    // just call subst() on t with the type variable substitution
    // information and return the result.
    return subst(t, typeVariables, substTypeVariables); // RECURSIVE
  }

  private static final IntersectionType subst(final IntersectionType t,
                                              final List<? extends TypeMirror> from,
                                              final List<? extends TypeMirror> to) {
    assert t.getKind() == TypeKind.INTERSECTION;
    assert from.size() == to.size();

    final TypeMirror supertype = supertype(t);
    final TypeMirror substSupertype = subst(supertype, from, to); // RECURSIVE

    final List<? extends TypeMirror> interfaces = interfaces(t);
    final List<? extends TypeMirror> substInterfaces = subst(interfaces, from, to); // RECURSIVE

    if (supertype == substSupertype &&
        interfaces == substInterfaces) {
      return t;
    }

    final List<TypeMirror> bounds = new ArrayList<>(substInterfaces.size() + 1);
    bounds.add(substSupertype);
    bounds.addAll(substInterfaces);
    return intersectionType(bounds);
  }

  // Yuck. See
  // https://github.com/openjdk/jdk/blob/0f2113cee79b9645105b4753c7d7eacb83b872c2/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3341-L3351
  private static final TypeMirror subst(final TypeVariable tv,
                                        final List<? extends TypeMirror> from,
                                        final List<? extends TypeMirror> to) {
    assert tv.getKind() == TypeKind.TYPEVAR;
    if (capturedTypeVariable(tv)) {
      return tv;
    }
    final int size = from.size();
    assert size == to.size();
    for (int i = 0; i < size; i++) {
      final TypeMirror f = from.get(i);
      if (Equality.equals(f, tv, false)) {
        final TypeMirror t = to.get(i);
        return
          t.getKind() == TypeKind.WILDCARD ? wildcardTypeWithTypeVariableBound((WildcardType)t, tv) : t;
      }
    }
    return tv;
  }

  private static final WildcardType subst(final WildcardType wt,
                                          final List<? extends TypeMirror> from,
                                          final List<? extends TypeMirror> to) {
    assert wt.getKind() == TypeKind.WILDCARD;
    assert from.size() == to.size();
    // Structural type mapping adds complexity here.  Wildcard types
    // in the compiler are absolutely bonkers.  We have to translate
    // the logic into javax.lang.model-speak.  Our reference is
    // https://github.com/openjdk/jdk/blob/0f2113cee79b9645105b4753c7d7eacb83b872c2/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3367-L3374
    // which references
    // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L261-L275
    TypeMirror upperBound = wt.getExtendsBound();
    TypeMirror lowerBound = wt.getSuperBound();
    if (lowerBound == null) {
      if (upperBound == null) {
        // Unbounded.  No need to do anything else.
        return wt;
      }
      // Upper-bounded.
      final TypeMirror x = subst(upperBound, from, to); // RECURSIVE
      if (x == upperBound) {
        return wt;
      }
      return upperBoundedWildcardType(x, wt.getAnnotationMirrors());
    } else if (upperBound == null) {
      // Lower-bounded.
      final TypeMirror x = subst(lowerBound, from, to); // RECURSIVE
      if (x == lowerBound) {
        return wt;
      }
      return lowerBoundedWildcardType(x, wt.getAnnotationMirrors());
    } else {
      // Wildcards can only specify a single bound, either upper or
      // lower.
      throw new IllegalArgumentException("wt: " + wt);
    }
  }

  // This method appears to be unused
  // (https://github.com/openjdk/jdk/search?q=substBound).  Perhaps it
  // was intended to be used with substBounds? And just never folded
  // in?  Or maybe it was originally used, but then a requirement
  // showed up that if a subst operation doesn't "do" anything it
  // needs to return the original objects, and so this method would
  // become useless in such a case?  All speculation.
  // https://github.com/openjdk/jdk/blob/3cd3a83647297f525f5eab48ce688e024ca6b08c/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3454-L3467
  /*
  private static final TypeVariable substBound(final TypeVariable tv,
                                               final List<? extends TypeMirror> from,
                                               final List<? extends TypeMirror> to) {
    assert tv.getKind() == TypeKind.TYPEVAR;
    assert from.size() == to.size();
    final TypeMirror upperBound = tv.getUpperBound();
    final TypeMirror substUpperBound = subst(upperBound, from, to);
    if (upperBound == substUpperBound) {
      return tv;
    }
    final TypeVariable unbounded = typeVariable(tv, null);
    return typeVariable(tv, subst(substUpperBound, List.of(tv), List.of(unbounded)));
  }
  */

  // Not visitor-based.
  // https://github.com/openjdk/jdk/blob/3cd3a83647297f525f5eab48ce688e024ca6b08c/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3413-L3452
  private static final List<? extends TypeVariable> substBounds(final List<? extends TypeVariable> tvs,
                                                                final List<? extends TypeMirror> from,
                                                                final List<? extends TypeMirror> to) {
    assert from.size() == to.size();
    if (tvs.isEmpty()) {
      return tvs;
    }

    // Phase 1 (it appears): effectively call subst() on the upper
    // bound of all the type variables.  If this didn't result in any
    // changes, we're done.
    final List<TypeMirror> substUpperBounds = new ArrayList<>(tvs.size());
    boolean changed = false;
    for (final TypeVariable tv : tvs) {
      assert tv.getKind() == TypeKind.TYPEVAR;
      final TypeMirror upperBound = tv.getUpperBound();
      final TypeMirror substUpperBound = subst(upperBound, from, to); // RECURSIVE, sort of; note subst() call
      if (!changed) {
        if (upperBound != substUpperBound) {
          changed = true;
        }
      }
      substUpperBounds.add(substUpperBound);
    }
    if (!changed) {
      return tvs;
    }

    // Phase 2: Create a list of type variables without bounds.  We
    // will selectively reset certain elements of this list in phase
    // 3.
    final List<TypeVariable> newTvs = new ArrayList<>(tvs.size());
    for (final TypeVariable tv : tvs) {
      newTvs.add(typeVariable(tv, null));
    }

    // Phase 3: Perform substitution over the substituted bounds
    // themselves of the supplied type variables with the unbounded
    // type variables created in phase 2.
    for (int i = 0; i < substUpperBounds.size(); i++) {
      substUpperBounds.set(i, subst(substUpperBounds.get(i), tvs, newTvs)); // RECURSIVE, sort of; note subst() call
    }

    // Phase 4: Effectively replace the unlimited upper bounds with
    // the substituted bounds we calculated in phase 1.
    for (int i = 0; i < newTvs.size(); i++) {
      newTvs.set(i, typeVariable(newTvs.get(i), substUpperBounds.get(i)));
    }

    return Collections.unmodifiableList(newTvs);
  }

  // TypeRelation-based
  private static final boolean subtype(final TypeMirror t, final TypeMirror s, final boolean capture) {
    if (Equality.equals(t, s, false)) {
      return true;
    }
    switch (s.getKind()) {
    case INTERSECTION:
      return subtype(t, (IntersectionType)s, capture);
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    default:
      switch (t.getKind()) {
      case INTERSECTION:
        break;
      default:
        final TypeMirror lowerBound = capturedTypeVariableLowerBound(wildcardLowerBound(s));
        if (s != lowerBound && !bottomType(lowerBound)) {
          return subtype(capture ? capture(t) : t, lowerBound, false); // RECURSIVE
        }
        break;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    return subtype0(capture ? capture(t) : t, s); // NOTE: subtype0, not subtype
  }

  private static final boolean subtype(final TypeMirror t, final IntersectionType s, final boolean capture) {
    assert s.getKind() == TypeKind.INTERSECTION;
    // The implementation parroted from javac appears to be a clumsy
    // way of simply checking the bounds.  Basically, you check the
    // superclass first, then the interfaces.  But that is also
    // exactly how the bounds of an intersection type are required to
    // be organized, so we could probably just call getBounds() and
    // loop over that instead.
    for (final TypeMirror bound : s.getBounds()) {
      if (!subtype(t, bound, capture)) {
        return false;
      }
    }
    return true;
  }

  // TypeRelation-based
  // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1109-L1238
  private static final boolean subtype0(final TypeMirror t, final TypeMirror s) {
    final TypeKind tk = t.getKind();
    switch (tk) {
    case ARRAY:
      return subtype0((ArrayType)t, s);
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      assert tk.isPrimitive();
      return subtype0((PrimitiveType)t, s);
    case DECLARED:
    case INTERSECTION:
      return subtype0DeclaredOrIntersection(t, s);
    case NULL:
      return subtype0((NullType)t, s);
    case TYPEVAR:
      return subtype0((TypeVariable)t, s);
    case VOID:
      return s.getKind() == TypeKind.VOID;
    case NONE:
    case WILDCARD:
      return false;
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean subtype0(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    switch (s.getKind()) {
    case ARRAY:
      return subtype0(t, (ArrayType)s);
    case DECLARED:
    case INTERSECTION:
      final Element e = asElement(s, true);
      if (e == null) {
        return false;
      }
      switch (e.getKind()) {
      case CLASS:
      case INTERFACE:
        final Name n = ((TypeElement)e).getQualifiedName();
        return
          n.contentEquals("java.lang.Object") ||
          n.contentEquals("java.lang.Cloneable") ||
          n.contentEquals("java.io.Serializable");
      default:
        return false;
      }
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    }
  }

  private static final boolean subtype0(final ArrayType t, final ArrayType s) {
    assert t.getKind() == TypeKind.ARRAY;
    assert s.getKind() == TypeKind.ARRAY;
    final TypeMirror componentType = t.getComponentType();
    if (componentType.getKind().isPrimitive()) {
      return models(componentType, componentType(s));
    } else {
      return subtype(componentType, componentType(s), false);
    }
  }

  private static final boolean subtype0DeclaredOrIntersection(final TypeMirror t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION : t.getKind();
    final TypeMirror sup = asSuper(t, asElement(s));
    if (sup == null) {
      return false;
    }
    switch (sup.getKind()) {
    case DECLARED:
    case INTERSECTION:
      return
        Equality.equals(asElement(t), asElement(s), true) &&
        (!parameterized(s) || containsRecursive(s, sup)) &&
        subtype(enclosingType(sup), enclosingType(s), false);
    default:
      return subtype(sup, s, false);
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1125-L1128
  private static final boolean subtype0(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    switch (s.getKind()) {
    case ARRAY:
    case DECLARED:
    case INTERSECTION:
    case NULL:
    case TYPEVAR:
      return true;
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    }
  }

  private static final boolean subtype0(final PrimitiveType t, final TypeMirror s) {
    final TypeKind tk = t.getKind();
    assert tk.isPrimitive();
    switch (tk) {

    case BOOLEAN:
      switch (s.getKind()) {
      case BOOLEAN:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case BYTE:
      switch (s.getKind()) {
      case BYTE:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case CHAR:
      switch (s.getKind()) {
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case DOUBLE:
      switch (s.getKind()) {
      case DOUBLE:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case FLOAT:
      switch (s.getKind()) {
      case DOUBLE:
      case FLOAT:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case INT:
      switch (s.getKind()) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case LONG:
      switch (s.getKind()) {
      case DOUBLE:
      case FLOAT:
      case LONG:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    case SHORT:
      switch (s.getKind()) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("s: " + s);
      }

    default:
      throw new AssertionError();
    }
  }

  private static final boolean subtype0(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return subtype(t.getUpperBound(), s, false); // RECURSIVE, in a way
  }

  private static final boolean subtypeUnchecked(final TypeMirror t, final TypeMirror s, final boolean capture) {
    // It is not entirely clear to me that we need this since we're not a compiler (unlike javac).
    // TODO: resume
    throw new UnsupportedOperationException();
  }

  // UnaryVisitor-based
  private static final TypeMirror supertype(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return supertype((ArrayType)t);
    case DECLARED:
      return supertype((DeclaredType)t);
    case INTERSECTION:
      return supertype((IntersectionType)t);
    case TYPEVAR:
      return supertype((TypeVariable)t);
    default:
      return noneType();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror supertype(final ArrayType t) {
    assert t.getKind() == TypeKind.ARRAY;
    return
      intersectionType(List.of(objectType(),
                               DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                               DefaultDeclaredType.JAVA_LANG_CLONEABLE));
  }

  // This is frighteningly awful code.
  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2525-L2546
  private static final TypeMirror supertype(final DeclaredType t) {
    final TypeKind k = t.getKind();
    assert k == TypeKind.DECLARED;

    // Compiler:
    //
    // Calls ClassSymbol (TypeElement)'s getSuperclass() method.


    // Port without fully understanding and with some comments added:
    //
    //      @Override
    //      public Type visitClassType(ClassType t, Void ignored) {
    //          if (t.supertype_field == null) {
    //
    //              // This call can apparently set the supertype_field to NONE.
    //              Type supertype = ((ClassSymbol)t.tsym).getSuperclass();
    //
    //              // An interface has no superclass; its supertype is Object.
    //              if (t.isInterface())
    //                  supertype = ((ClassType)t.tsym.type).supertype_field;
    //
    //              // It seems the only way this could be true is if
    //              // t is an ErrorType (a subtype of ClassType).
    //              if (t.supertype_field == null) {
    //                  List<Type> actuals = classBound(t).allparams();
    //                  List<Type> formals = t.tsym.type.allparams();
    //                  if (t.hasErasedSupertypes()) {
    //                      t.supertype_field = erasureRecursive(supertype);
    //                  } else if (formals.nonEmpty()) {
    //                      t.supertype_field = subst(supertype, formals, actuals);
    //                  }
    //                  else {
    //                      t.supertype_field = supertype;
    //                  }
    //              }
    //          }
    //          return t.supertype_field;
    //      }
    //
    // It seems like this is a lazy mechanism to compute the
    // parameterized type that will serve as the class' supertype.
    //
    // In the runtime APIs, for example, if you call
    // someClass.getGenericSuperclass(), you get a ParameterizedType
    // with all the variables filled in for you.  Presumably this is
    // that mechanism.
    //
    // You'll note the t.supertype_field is checked for null twice.
    // This means that ClassSymbol#getSuperclass() (or, in the lang
    // model, Element#getSuperclass()) or ClassType#isInterface() must
    // be capable of setting it as some kind of awful side effect.  It
    // turns out it's not ClassType#isInterface(), so let's look at
    // ClassSymbol#getSuperclass():
    //
    //   @DefinedBy(Api.LANGUAGE_MODEL)
    //   public Type getSuperclass() {
    //       apiComplete();
    //       if (type instanceof ClassType classType) {
    //           if (classType.supertype_field == null) // FIXME: shouldn't be null
    //               classType.supertype_field = Type.noType;
    //           // An interface has no superclass; its supertype is Object.
    //           return classType.isInterface()
    //               ? Type.noType
    //               : classType.supertype_field.getModelType();
    //       } else {
    //           return Type.noType;
    //       }
    //   }
    //
    // So it looks like t.supertype_field can only be null after this
    // if the ClassType in question is actually an ErrorType.
    //
    // I have no idea how this actually is working.
    //
    // Finally from the API it's unclear whether the lang model
    // classes already do this computation for you or not.
    //
    // If they do, then since this operation is idempotent it
    // shouldn't do any harm.  If they don't, then since the types
    // involved in *this* project don't have anywhere to cache things,
    // then relying on fields is impossible, so we should just
    // probably fall through to the actual computation logic and hope
    // for the best.
    //
    // In short: I think the way the compiler has implemented this is
    // ferociously confusing and probably not at all necessary
    // anymore.  Since we're stateless, we can just skip to the heart
    // of it.
    final TypeMirror supertype = ((TypeElement)t.asElement()).getSuperclass();
    if (raw(t)) {
      return erase(supertype);
    }
    final List<? extends TypeMirror> formals = allTypeArguments(canonicalType(t));
    return formals.isEmpty() ? supertype : subst(supertype, formals, allTypeArguments(boundingClass(t)));
  }

  private static final TypeMirror supertype(final IntersectionType t) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // The supertype of an intersection type appears to be its first
    // bound, and there appears to be a mandated order to its bounds.
    // See
    // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1268.
    return t.getBounds().get(0);
  }

  // UnaryVisitor-based, so works on captured type variables too.
  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2548-L2562
  private static final TypeMirror supertype(final TypeVariable t) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    switch (upperBound.getKind()) {
    case TYPEVAR:
      return upperBound;
    case INTERSECTION:
      return supertype(upperBound);
    default:
      return isInterface(upperBound) ? supertype(upperBound) : upperBound;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final DeclaredType syntheticDeclaredType(final DeclaredType canonicalType,
                                                          final List<? extends TypeMirror> typeArguments) {
    final DefaultDeclaredType t = new DefaultDeclaredType(canonicalType.getEnclosingType(), typeArguments, List.of());
    t.element(canonicalType.asElement());
    return t;
  }

  // See also allTypeArguments(TypeMirror)
  private static final List<? extends TypeMirror> typeArguments(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return ((DeclaredType)t).getTypeArguments();
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeVariable typeVariable(final TypeVariable tv, final TypeMirror upperBound) {
    assert tv.getKind() == TypeKind.TYPEVAR;
    final DefaultTypeVariable returnValue = new DefaultTypeVariable(upperBound, tv.getLowerBound(), tv.getAnnotationMirrors());
    returnValue.element((TypeParameterElement)tv.asElement());
    assert returnValue.asElement().asType() == tv;
    return returnValue;
  }

  private static final TypeVariable typeVariable(final TypeMirror upperBound,
                                                 final TypeMirror lowerBound, // use with caution; normally null
                                                 final List<? extends AnnotationMirror> annotationMirrors) {
    return new DefaultTypeVariable(upperBound, lowerBound, annotationMirrors);
  }

  // A port/translation of Types#newInstances(List)
  // (https://github.com/openjdk/jdk/blob/3cd3a83647297f525f5eab48ce688e024ca6b08c/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3489-L3507)
  private static final List<? extends TypeVariable> typeVariables(final List<? extends TypeVariable> tvs) {
    if (tvs.isEmpty()) {
      // TODO: check: should this return tvs, or is it understood that
      // the return value will be a new List?
      return List.of();
    }
    final List<TypeVariable> newTvs = new ArrayList<>(tvs);
    for (int i = 0; i < newTvs.size(); i++) {
      final TypeVariable tv = tvs.get(i);
      newTvs.set(i, typeVariable(tv, (TypeVariable)subst(tv.getUpperBound(), tvs, newTvs))); // NOTE subst() call
    }
    return Collections.unmodifiableList(newTvs);
  }

  private static final WildcardType unboundedWildcardType() {
    return DefaultWildcardType.UNBOUNDED;
  }

  private static final WildcardType unboundedWildcardType(final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.unboundedWildcardType(annotationMirrors);
  }

  private static final TypeMirror unboxedType(final TypeMirror t) {
    // TODO: resume
    throw new UnsupportedOperationException();
  }
  
  private static final WildcardType upperBoundedWildcardType(final TypeMirror upperBound,
                                                             final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.upperBoundedWildcardType(upperBound, annotationMirrors);
  }

  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L157-L167
  private static final TypeMirror wildcardLowerBound(final TypeMirror t) {
    switch (t.getKind()) {
    case WILDCARD:
      return wildcardLowerBound((WildcardType)t);
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror wildcardLowerBound(final WildcardType w) {
    assert w.getKind() == TypeKind.WILDCARD;
    final TypeMirror lowerBound = w.getSuperBound();
    if (lowerBound == null) {
      return null;
    } else if (w.getExtendsBound() == null) {
      assert lowerBound.getKind() != TypeKind.WILDCARD;
      return lowerBound;
    } else {
      throw new IllegalArgumentException("w: " + w);
    }
  }

  // Translation of "withTypeVar"; see
  // https://github.com/openjdk/jdk/blob/0f2113cee79b9645105b4753c7d7eacb83b872c2/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L895-L902
  private static final WildcardType wildcardTypeWithTypeVariableBound(final WildcardType w, final TypeVariable tv) {
    assert w.getKind() == TypeKind.WILDCARD;
    assert tv.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = w.getExtendsBound();
    final TypeMirror lowerBound = w.getSuperBound();
    if (lowerBound == null) {
      if (upperBound == null || !Equality.equals(tv, upperBound, false)) {
        // Return a (normally new) wildcard type whose upper bound is
        // the TypeVariable.
        return upperBoundedWildcardType(tv, w.getAnnotationMirrors());
      }
      return w;
    } else if (upperBound == null) {
      if (Equality.equals(tv, lowerBound, false)) {
        return w;
      }
      // Return a (normally new) wildcard type whose lower bound is
      // the TypeVariable.
      return lowerBoundedWildcardType(tv, w.getAnnotationMirrors());
    } else {
      throw new IllegalArgumentException("w: " + w);
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143
  private static final TypeMirror wildcardUpperBound(final TypeMirror t) {
    switch (t.getKind()) {
    case WILDCARD:
      return wildcardUpperBound((WildcardType)t);
    default:
      return t;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final TypeMirror wildcardUpperBound(final WildcardType w) {
    final TypeMirror lowerBound = w.getSuperBound();
    if (lowerBound == null) {
      final TypeMirror upperBound = w.getExtendsBound();
      if (upperBound == null) {
        return objectType(); // e.g. java.lang.Object type
      } else {
        assert
          upperBound.getKind() == TypeKind.ARRAY ||
          upperBound.getKind() == TypeKind.DECLARED ||
          upperBound.getKind() == TypeKind.TYPEVAR;
        // No need to recurse, because we know upperBound cannot be
        // a wildcard, so we just return it.
        return upperBound;
      }
    } else if (lowerBound.getKind() == TypeKind.TYPEVAR) {
      return ((TypeVariable)lowerBound).getUpperBound();
    } else {
      return objectType(); // e.g. java.lang.Object type
    }
  }

  private static final List<? extends TypeMirror> withFreshCapturedTypeVariables(final List<? extends TypeMirror> typeArguments) {
    if (typeArguments.isEmpty()) {
      return List.of();
    }
    final List<TypeMirror> list = new ArrayList<>(typeArguments.size());
    for (final TypeMirror typeArgument : typeArguments) {
      switch (typeArgument.getKind()) {
      case WILDCARD:
        list.add(new DefaultCapturedType((WildcardType)typeArgument));
        break;
      default:
        list.add(typeArgument);
        break;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("typeArguments: " + typeArguments);
      }
    }
    return Collections.unmodifiableList(list);
  }


  /*
   * Inner and nested classes.
   */


  private static final class SyntheticElement extends AbstractElement {

    private final Reference<TypeMirror> type;

    private SyntheticElement(final TypeMirror type) {
      this(generateName(type), type);
    }

    private SyntheticElement(final AnnotatedName name, final TypeMirror type) {
      super(name, ElementKind.OTHER, noneType(), Set.of(), null, null);
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

    private static final AnnotatedName generateName(final TypeMirror t) {
      return AnnotatedName.of(DefaultName.EMPTY); // TODO if it turns out to be important
    }

  }

  private static final class TypeMirrorPair {

    private final TypeMirror t;

    private final TypeMirror s;

    private TypeMirrorPair(final TypeMirror t,
                           final TypeMirror s) {
      super();
      this.t = Objects.requireNonNull(t, "t");
      this.s = Objects.requireNonNull(s, "s");
    }

    @Override
    public final int hashCode() {
      return 127 * hashCode(this.t) + hashCode(this.s);
    }

    @Override
    public final boolean equals(final Object other) {
      if (this == other) {
        return true;
      } else if (other != null && this.getClass() == other.getClass()) {
        final TypeMirrorPair her = (TypeMirrorPair)other;
        return
          models(this.t, her.t) && models(this.s, her.s);
      } else {
        return false;
      }
    }

    private static final int hashCode(final TypeMirror t) {
      final TypeKind tk = t.getKind();
      switch (tk) {
      case ARRAY:
        return hashCode((ArrayType)t);
      case DECLARED:
      case INTERSECTION:
        return hashCode0(t);
      case EXECUTABLE:
        return hashCode((ExecutableType)t);
      case TYPEVAR:
        return hashCode((TypeVariable)t);
      case WILDCARD:
        return hashCode((WildcardType)t);
      default:
        return tk.ordinal();
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("t: " + t);
      }
    }

    private static final int hashCode0(final TypeMirror t) {
      final TypeMirror enclosingType = enclosingType(t);
      int result = 127 * (enclosingType == null ? 0 : hashCode(enclosingType));
      final Element e = asElement(t);
      if (e != null) {
        result += flatName(e).hashCode();
      }
      for (final TypeMirror typeArgument : typeArguments(t)) {
        result = 127 * result + hashCode(typeArgument);
      }
      return result;
    }

    private static final int hashCode(final ArrayType t) {
      assert t.getKind() == TypeKind.ARRAY;
      return hashCode(t.getComponentType()) + 12;
    }

    private static final int hashCode(final ExecutableType t) {
      final TypeKind tk = t.getKind();
      assert tk == TypeKind.EXECUTABLE;
      int result = tk.ordinal();
      for (final TypeMirror pt : t.getParameterTypes()) {
        result = (result << 5) + hashCode(pt);
      }
      return (result << 5) + hashCode(t.getReturnType());
    }

    private static final int hashCode(final TypeVariable t) {
      assert t.getKind() == TypeKind.TYPEVAR;
      // TODO: it sure would be nice if we could use
      // Identity.hashCode(t) here; maybe we can?
      return System.identityHashCode(t);
    }

    private static final int hashCode(final WildcardType t) {
      final TypeKind tk = t.getKind();
      assert tk == TypeKind.WILDCARD;
      int result = tk.hashCode();
      final TypeMirror lowerBound = t.getSuperBound();
      final TypeMirror upperBound = t.getExtendsBound();
      if (lowerBound == null) {
        if (upperBound != null) {
          result *= 127;
          result += hashCode(upperBound);
        }
      } else if (upperBound == null) {
        result *= 127;
        result += hashCode(lowerBound);
      } else {
        throw new IllegalArgumentException("t: " + t);
      }
      return result;
    }

  }

}
