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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.function.BiFunction;

import javax.lang.model.AnnotatedConstruct;

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

// Worth noting that javac itself has all kinds of problems:
//
// https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20JDK%20AND%20labels%20%3D%20jls-types
//
// https://bugs.openjdk.java.net/browse/JDK-8154901
//
// https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20JDK%20AND%20issuetype%20%3D%20Bug%20AND%20status%20%3D%20Open%20AND%20labels%20in%20(javac%2C%20javac-types%2C%20javac-wildcards%2C%20javac-check-spec)

final class Types {

  private static final DefaultDeclaredType CLONEABLE_TYPE = DefaultDeclaredType.JAVA_LANG_CLONEABLE;

  private static final DefaultDeclaredType SERIALIZABLE_TYPE = DefaultDeclaredType.JAVA_IO_SERIALIZABLE;

  // @GuardedBy("itself")
  private static final WeakHashMap<TypeMirror, Element> syntheticElements = new WeakHashMap<>();

  private Types() {
    super();
  }

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

  public static final ArrayType arrayType(final TypeMirror componentType) {
    return arrayType(componentType, List.of());
  }

  public static final ArrayType arrayType(final TypeMirror componentType, final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultArrayType.of(componentType, annotationMirrors);
  }

  public static final Element asElement(final TypeMirror t) {
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
      // related in the say that, say, DeclaredType and TypeElement
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
      throw new UnsupportedOperationException();

    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // Not visitor-based in javac
  private static final TypeMirror asOuterSuper(TypeMirror t, final Element e) {
    switch (t.getKind()) {
    case ARRAY:
      final TypeMirror et = e.asType();
      return subtype(t, et, true) ? et : null;
    case DECLARED:
    case INTERSECTION:
      return asOuterSuper0(t, e);
    case TYPEVAR:
      return asSuper(t, e);
    case ERROR:
      return t;
    default:
      return null;
    }
  }

  private static final TypeMirror asOuterSuper0(final TypeMirror t, final Element e) {
    TypeMirror x = t;
    WHILE_LOOP:
    while (x != null) {
      final TypeMirror s = asSuper(x, e);
      if (s != null) {
        return s;
      }
      switch (x.getKind()) {
      case DECLARED:
        x = ((DeclaredType)x).getEnclosingType();
        continue WHILE_LOOP;
      default:
        return null;
      }
    }
    return null;
  }

  // SimpleVisitor-based
  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2131-L2215
  private static final TypeMirror asSuper(final TypeMirror t, final Element e) {
    return asSuper(t, e, null);
  }

  private static final TypeMirror asSuper(final TypeMirror t, final Element e, final Set<AnnotatedConstruct> seen) {
    // TODO: optimize for when e.asType() is java.lang.Object
    switch (t.getKind()) {
    case ARRAY:
      return asSuper((ArrayType)t, e);
    case DECLARED:
    case TYPEVAR:
      return asSuper0(t, e, seen == null ? new HashSet<>() : seen);
    case ERROR:
      return t;
    case INTERSECTION:
      // TODO: I'm pretty sure this is a correct port, but...not super
      // sure.  IntersectionClassTypes have symbols, bizarrely enough,
      // but it is exactly one symbol.  This may matter in the "seen"
      // set.
      return null;
    default:
      return null; // Yes, really
    }
  }

  private static final TypeMirror asSuper(final ArrayType t, final Element e) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror elementType = e.asType();
    return subtype(t, elementType, true) ? elementType : null;
  }

  private static final TypeMirror asSuper0(final TypeMirror t, final Element e, final Set<AnnotatedConstruct> seen) {
    final Element te = asElement(t, true);
    if (Identity.identical(te, e, true)) {
      return t;
    }
    if (t.getKind() == TypeKind.TYPEVAR) {
      // SimpleVisitor-based so also handles captured type variables
      return asSuper(((TypeVariable)t).getUpperBound(), e, seen); // RECURSIVE
    }
    final DefaultElement teKey = DefaultElement.of(te);
    if (!seen.add(teKey)) {
      return t;
    }
    try {
      TypeMirror x = asSuper(supertype(t), e, seen); // RECURSIVE
      if (x != null) {
        return x;
      }
      if (isInterface(e)) {
        for (final TypeMirror iface : interfaces(t)) {
          if (iface.getKind() != TypeKind.ERROR) {
            x = asSuper(iface, e, seen); // RECURSIVE
            if (x != null) {
              return x;
            }
          }
        }
      }
      return null; // Yes, really.
    } finally {
      seen.remove(teKey);
    }
  }

  private static final boolean bottomType(final TypeMirror t) {
    return t.getKind() == TypeKind.NULL; // TODO: I think
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
    case ERROR:
    case INTERSECTION:
    default:
      return t;
    }
  }

  private static final boolean canonical(final TypeMirror t) {
    final Element e = asElement(t);
    return e == null || Identity.identical(t, e.asType(), true);
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

  private static final List<TypeMirror> closure(final TypeMirror t) {
    final List<TypeMirror> cl;
    final TypeMirror st = supertype(t);
    switch (t.getKind()) {
    case INTERSECTION:
      cl = closure(st);
      break;
    case DECLARED:
    case TYPEVAR:
      switch (st.getKind()) {
      case DECLARED:
        cl = closureInsert(closure(st), t);
        break;
      case TYPEVAR:
        cl = new ArrayList<>();
        cl.add(t);
        cl.addAll(closure(st));
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    // TODO: resume
    throw new UnsupportedOperationException();
  }

  private static final List<TypeMirror> closureInsert(final List<TypeMirror> closure, final TypeMirror t) {
    if (closure.isEmpty()) {
      closure.add(t);
      return closure;
    }
    final Element e = asElement(t);
    if (e == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    final Element headE = asElement(closure.get(0));
    if (headE == null) {
      throw new IllegalArgumentException("closure: " + closure);
    }
    if (!Identity.identical(e, headE, true)) {
      if (precedes(e, headE)) {
        closure.add(0, t);
      } else {
        closureInsert(closure.subList(1, closure.size()), t); // RECURSIVE
      }
    }
    return closure;
  }

  private static final List<TypeMirror> closureUnion(final List<TypeMirror> c1, final List<TypeMirror> c2) {
    if (c1.isEmpty()) {
      return c2;
    } else if (c2.isEmpty()) {
      return c1;
    }
    final TypeMirror head1 = c1.get(0);
    final Element head1E = asElement(head1);
    if (head1E == null) {
      throw new IllegalArgumentException("c1: " + c1);
    }
    final TypeMirror head2 = c2.get(0);
    final Element head2E = asElement(head2);
    if (head2E == null) {
      throw new IllegalArgumentException("c2: " + c2);
    }
    if (Identity.identical(head1E, head2E, true)) {
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

  /**
   * Returns {@code true} if {@code t1} contains {@code t2}.
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
   * @return {@code true} if {@code t1} contains {@code t2}
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if a {@link WildcardType} is
   * encountered that has both an upper and a lower bound
   */
  // Not visitor-based.
  public static final boolean contains(final TypeMirror t1, final TypeMirror t2) {
    switch (t1.getKind()) {
    case ARRAY:
      return contains((ArrayType)t1, t2);
    case DECLARED:
      return contains((DeclaredType)t1, t2);
    case EXECUTABLE:
      return contains((ExecutableType)t1, t2);
    case INTERSECTION:
      return contains((IntersectionType)t1, t2);
    case UNION:
      return contains((UnionType)t1, t2);
    case WILDCARD:
      return contains((WildcardType)t1, t2);
    default:
      return Identity.identical(t1, t2, false);
    }
  }

  private static final boolean contains(final ArrayType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.ARRAY;
    return
      Identity.identical(t1, t2, false) ||
      contains(t1.getComponentType(), t2);
  }

  private static final boolean contains(final DeclaredType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.DECLARED;
    return
      Identity.identical(t1, t2, false) ||
      (hasTypeArguments(t1) && (contains(t1.getEnclosingType(), t2) || anyIs(t1.getTypeArguments(), t2)));
  }

  private static final boolean contains(final ExecutableType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.EXECUTABLE;
    return
      Identity.identical(t1, t2, false) ||
      anyIs(t1.getParameterTypes(), t2) ||
      contains(t1.getReturnType(), t2) ||
      anyIs(t1.getThrownTypes(), t2);
  }

  // Interestingly, the compiler does *not* test for equality/identity
  // in these three cases.  It makes a certain amount of sense when
  // you consider that all three of these are effectively just
  // collections of types with no real identity of their own.

  private static final boolean contains(final IntersectionType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.INTERSECTION;
    return anyIs(t1.getBounds(), t2);
  }

  private static final boolean contains(final UnionType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.UNION;
    return anyIs(t1.getAlternatives(), t2);
  }

  private static final boolean contains(final WildcardType t1, final TypeMirror t2) {
    assert t1.getKind() == TypeKind.WILDCARD;
    final TypeMirror upperBound = t1.getExtendsBound();
    final TypeMirror lowerBound = t1.getSuperBound();
    if (upperBound == null) {
      return lowerBound != null && contains(lowerBound, t2);
    } else if (lowerBound == null) {
      return contains(upperBound, t2);
    } else {
      throw new IllegalArgumentException("t1: " + t1);
    }
  }

  // Is at least one t in ts interchangeable with s?
  private static final boolean anyIs(final Collection<? extends TypeMirror> ts, final TypeMirror s) {
    if (!ts.isEmpty()) {
      for (final TypeMirror t : ts) {
        if (Identity.identical(t, s, false)) {
          return true;
        }
      }
    }
    return false;
  }

  // Does t contain at least one s in ss?
  private static final boolean containsAny(final TypeMirror t, final Collection<? extends TypeMirror> ss) {
    if (!ss.isEmpty()) {
      for (final TypeMirror s : ss) {
        if (contains(t, s)) {
          return true;
        }
      }
    }
    return false;
  }

  // Does at least one t in ts contain at least one s in ss?
  private static final boolean anyContainsAny(final Collection<? extends TypeMirror> ts, final Collection<? extends TypeMirror> ss) {
    if (!ts.isEmpty() && !ss.isEmpty()) {
      for (final TypeMirror t : ts) {
        if (containsAny(t, ss)) {
          return true;
        }
      }
    }
    return false;
  }

  public static final DeclaredType declaredType() {
    return DefaultDeclaredType.JAVA_LANG_OBJECT;
  }

  public static final DeclaredType declaredType(final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType(noneType(), List.of(), annotationMirrors);
  }

  public static final DeclaredType declaredType(final List<? extends TypeMirror> typeArguments,
                                                final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType(noneType(), typeArguments, annotationMirrors);
  }

  public static final DeclaredType declaredType(final DeclaredType enclosingType,
                                                final List<? extends TypeMirror> typeArguments,
                                                final List<? extends AnnotationMirror> annotationMirrors) {
    return declaredType((TypeMirror)enclosingType, typeArguments, annotationMirrors);
  }

  public static final DeclaredType declaredType(final TypeMirror enclosingType,
                                                final List<? extends TypeMirror> typeArguments,
                                                final List<? extends AnnotationMirror> annotationMirrors) {
    if ((enclosingType == null || enclosingType.getKind() == TypeKind.NONE) &&
        (annotationMirrors == null || annotationMirrors.isEmpty()) &&
        (typeArguments == null || typeArguments.isEmpty())) {
      return DefaultDeclaredType.JAVA_LANG_OBJECT;
    }
    switch (enclosingType.getKind()) {
    case DECLARED:
    case NONE:
      break;
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
    }
  }

  // StructuralTypeMapping-based
  public static final TypeMirror erase(final TypeMirror t) {
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
    }
  }

  // The original is StructuralTypeMapping-based, which means identity
  // is important in the return value.
  public static final List<? extends TypeMirror> erase(final List<? extends TypeMirror> ts) {
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
    final List<?> typeArguments = t.getTypeArguments();
    final TypeMirror enclosingType = t.getEnclosingType();
    switch (enclosingType.getKind()) {
    case DECLARED:
      return declaredType(erase((DeclaredType)enclosingType), List.of(), t.getAnnotationMirrors());
    case NONE:
      return typeArguments.isEmpty() ? t : declaredType(enclosingType, List.of(), t.getAnnotationMirrors());
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

  // "greatest lower bound"
  // Not visitor-based
  private static final TypeMirror glb(final TypeMirror t, final TypeMirror s) {
    if (s == null) {
      return t;
    }
    // TODO: resume
    throw new UnsupportedOperationException();
  }

  public static final boolean hasTypeArguments(final TypeMirror t) {
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
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    default:
      return List.of();
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
      final List<? extends TypeMirror> actuals = allTypeArguments(t);
      final List<? extends TypeMirror> formals = allTypeArguments(canonicalType(t));
      if (raw(t)) {
        return erase(interfaces);
      } else if (!formals.isEmpty()) {
        return subst(interfaces, formals, actuals);
      } else {
        return interfaces;
      }
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
      if (isInterface(bounds.get(0))) {
        return bounds;
      }
      return List.of();
    default:
      if (isInterface(bounds.get(0))) {
        return bounds;
      }
      return bounds.subList(1, size);
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
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    default:
      return List.of();
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
    }
  }

  private static final boolean isStatic(final Element e) {
    return e.getModifiers().contains(Modifier.STATIC);
  }

  private static final WildcardType lowerBoundedWildcardType(final TypeMirror lowerBound, final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.lowerBoundedWildcardType(lowerBound, annotationMirrors);
  }

  // SimpleVisitor-based
  private static final TypeMirror memberType(final TypeMirror t, final Element e) {
    switch (t.getKind()) {
    case DECLARED:
      return memberType((DeclaredType)t, e);
    case ERROR:
      return memberType((ErrorType)t, e);
    case INTERSECTION:
      return memberType((IntersectionType)t, e);
    case TYPEVAR:
      return memberType((TypeVariable)t, e);
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    case WILDCARD:
      return memberType((WildcardType)t, e);
    default:
      return e.asType();
    }
  }

  private static final TypeMirror memberType(final DeclaredType t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED;
    return memberType(t, e, Types::asOuterSuper);
  }

  private static final TypeMirror memberType(final ErrorType t, final Element e) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
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

  public static final NoType noneType() {
    return DefaultNoType.NONE;
  }

  public static final NullType nullType() {
    return DefaultNullType.INSTANCE;
  }

  private static final DeclaredType objectType() {
    return DefaultDeclaredType.JAVA_LANG_OBJECT;
  }

  public static final boolean parameterized(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return parameterized(((ArrayType)t).getComponentType());
    case DECLARED:
      return !allTypeArguments(t).isEmpty();
    default:
      return false;
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
        if (Identity.identical(e, f, true)) {
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
      default:
        throw new IllegalArgumentException("f: " + f);
      }
    case TYPEVAR:
      switch (s.getKind()) {
      case TYPEVAR:
        if (Identity.identical(e, f, true)) {
          return false;
        }
        return subtype(t, s, true);
      default:
        return true;
      }
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
    case ERROR:
    case NONE:
      return 0;
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
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    case TYPEVAR:
      return subst((TypeVariable)t, from, to);
    case WILDCARD:
      return subst((WildcardType)t, from, to);
    default:
      return t;
    }
  }

  private static final List<? extends TypeMirror> subst(final List<? extends TypeMirror> types, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
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

  private static final ArrayType subst(final ArrayType t, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
    assert t.getKind() == TypeKind.ARRAY;
    assert from.size() == to.size();
    final TypeMirror componentType = t.getComponentType();
    final TypeMirror x = subst(componentType, from, to); // RECURSIVE
    if (componentType == x) {
      return t;
    }
    return arrayType(componentType, t.getAnnotationMirrors());
  }

  private static final DeclaredType subst(final DeclaredType t, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
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

  private static final ExecutableType subst(ExecutableType t, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
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
    if (anyContainsAny(to, typeVariables)) {
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

  private static final IntersectionType subst(final IntersectionType t, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
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
  private static final TypeMirror subst(final TypeVariable tv, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
    assert tv.getKind() == TypeKind.TYPEVAR;
    if (capturedTypeVariable(tv)) {
      return tv;
    }
    final int size = from.size();
    assert size == to.size();
    for (int i = 0; i < size; i++) {
      final TypeMirror f = from.get(i);
      if (Identity.identical(f, tv, false)) {
        final TypeMirror t = to.get(i);
        return
          t.getKind() == TypeKind.WILDCARD ? wildcardTypeWithTypeVariableBound((WildcardType)t, tv) : t;
      }
    }
    return tv;
  }

  private static final WildcardType subst(final WildcardType wt, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
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
  public static final boolean subtype(final TypeMirror t, final TypeMirror s, final boolean capture) {
    if (Identity.identical(t, s, false)) {
      return true;
    }
    switch (s.getKind()) {
    case INTERSECTION:
      return subtype(t, (IntersectionType)s, capture);
    case UNION:
      throw new IllegalArgumentException("s: " + s);
    default:
      switch (t.getKind()) {
      case INTERSECTION:
        break;
      case UNION:
        throw new IllegalArgumentException("t: " + t);
      default:
        final TypeMirror lowerBound = capturedTypeVariableLowerBound(wildcardLowerBound(s));
        if (s != lowerBound && !bottomType(lowerBound)) {
          return subtype(capture ? capture(t) : t, lowerBound, false); // RECURSIVE
        }
        break;
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
    switch (t.getKind()) {
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
      return subtype0((PrimitiveType)t, s);
    case DECLARED:
      return subtype0((DeclaredType)t, s);
    case ERROR:
      return true;
    case INTERSECTION:
      return subtype0((IntersectionType)t, s);
    case NULL:
      return subtype0((NullType)t, s);
    case TYPEVAR:
      return subtype0((TypeVariable)t, s);
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    case VOID:
      // Wow; this is odd
      return s.getKind() == TypeKind.VOID;
    case NONE:
    case WILDCARD:
      return false;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean subtype0(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    // TODO: resume
    throw new UnsupportedOperationException();
  }


  private static final boolean subtype0(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror sup = asSuper(t, asElement(s));
    // javac code says:
    //
    //  If t is an intersection, sup might not be a class type
    //
    // This implies:
    // * intersection types can have supertypes (which is not called out in the JLS)
    // * the supertype of an intersection type might be something other than:
    //   * a class/interface/enum/record,
    //   * an intersection type
    //   * (a union type)
    //   * (an error type)
    //   * a type variable
    //
    // That doesn't leave very much.
    //
    //   1. a


    // TODO: resume
    throw new UnsupportedOperationException();
  }

  private static final boolean subtype0(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // Note that there is explicit handling in Types.java of
    // intersection types here:
    // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1189-L1192
    //
    // TODO: resume
    throw new UnsupportedOperationException();
  }

  // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1125-L1128
  private static final boolean subtype0(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    switch (s.getKind()) {
    case ARRAY:
    case DECLARED:
    case INTERSECTION:
    case ERROR:
    case NULL:
    case TYPEVAR:
      return true;
    default:
      return false;
    }
  }

  private static final boolean subtype0(final PrimitiveType t, final TypeMirror s) {
    switch (t.getKind()) {
    case BOOLEAN:
      return s.getKind() == TypeKind.BOOLEAN;
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      break;
    default:
      throw new AssertionError();
    }
    // TODO: resume
    throw new UnsupportedOperationException();
  }

  private static final boolean subtype0(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return subtype(t.getUpperBound(), s, false); // RECURSIVE, in a way
  }


  // UnaryVisitor-based
  public static final TypeMirror supertype(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return supertype((ArrayType)t);
    case DECLARED:
      return supertype((DeclaredType)t);
    case INTERSECTION:
      return supertype((IntersectionType)t);
    case TYPEVAR:
      return supertype((TypeVariable)t);
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    default:
      return noneType();
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
    final List<? extends TypeMirror> actuals = allTypeArguments(boundingClass(t));
    final List<? extends TypeMirror> formals = allTypeArguments(canonicalType(t));
    if (raw(t)) {
      return erase(supertype);
    } else if (!formals.isEmpty()) {
      return subst(supertype, formals, actuals);
    } else {
      return supertype;
    }
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
    }
  }

  private static final DeclaredType syntheticDeclaredType(final DeclaredType canonicalType,
                                                          final List<? extends TypeMirror> typeArguments) {
    final DefaultDeclaredType t = new DefaultDeclaredType(canonicalType.getEnclosingType(), typeArguments, List.of());
    t.element(canonicalType.asElement());
    return t;
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

  private static final UnionType unionType(final List<? extends TypeMirror> alternatives) {
    return DefaultUnionType.of(alternatives);
  }

  private static final WildcardType upperBoundedWildcardType(final TypeMirror upperBound, final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.upperBoundedWildcardType(upperBound, annotationMirrors);
  }

  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L157-L167
  private static final TypeMirror wildcardLowerBound(final TypeMirror t) {
    switch (t.getKind()) {
    case WILDCARD:
      return wildcardLowerBound((WildcardType)t);
    default:
      return t;
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
      if (upperBound == null || !Identity.identical(tv, upperBound, false)) {
        // Replace the upper bound with the TypeVariable.
        return upperBoundedWildcardType(tv, w.getAnnotationMirrors());
      }
      return w;
    } else if (upperBound == null) {
      if (Identity.identical(tv, lowerBound, false)) {
        return w;
      }
      // Replace the lower bound with the TypeVariable.
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
    }
  }

  private static final TypeMirror wildcardUpperBound(final WildcardType w) {
    final TypeMirror lowerBound = w.getSuperBound();
    if (lowerBound == null) {
      final TypeMirror upperBound = w.getExtendsBound();
      if (upperBound == null) {
        return declaredType(); // e.g. java.lang.Object type
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
      return declaredType(); // e.g. java.lang.Object type
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
      }
    }
    return Collections.unmodifiableList(list);
  }

  private static final class SyntheticElement extends AbstractElement {

    private final Reference<TypeMirror> type;

    private SyntheticElement(final TypeMirror type) {
      this(generateName(type), type);
    }

    private SyntheticElement(final Name name, final TypeMirror type) {
      super(name, ElementKind.OTHER, noneType(), Set.of(), null, List.of());
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

    private static final Name generateName(final TypeMirror t) {
      return DefaultName.EMPTY; // TODO if it turns out to be important
    }

  }

}
