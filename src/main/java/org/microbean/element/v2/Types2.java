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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public class Types2 {

  // @see #asElement(TypeMirror, boolean)
  // @GuardedBy("itself")
  private static final WeakHashMap<TypeMirror, Element> syntheticElements = new WeakHashMap<>();

  public Types2() {
    super();
  }

  // Not visitor-based in javac
  public static final List<? extends TypeMirror> allTypeArguments(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return allTypeArguments(((ArrayType)t).getComponentType()); // RECURSIVE
    case DECLARED:
      return allTypeArguments((DeclaredType)t);
    case INTERSECTION:
      // Verified; see
      // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1265. A
      // compiler IntersectionClassType is a ClassType that never has
      // any type parameters or annotations.
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  public static final List<? extends TypeMirror> allTypeArguments(final DeclaredType t) {
    if (t.getKind() != TypeKind.DECLARED) {
      throw new IllegalArgumentException("t: " + t);
    }
    final List<? extends TypeMirror> enclosingTypeTypeArguments = allTypeArguments(t.getEnclosingType()); // RECURSIVE
    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    if (enclosingTypeTypeArguments.isEmpty()) {
      return typeArguments.isEmpty() ? List.of() : typeArguments;
    } else if (typeArguments.isEmpty()) {
      return enclosingTypeTypeArguments;
    }
    final List<TypeMirror> list = new ArrayList<>(enclosingTypeTypeArguments.size() + typeArguments.size());
    list.addAll(enclosingTypeTypeArguments);
    list.addAll(typeArguments);
    return Collections.unmodifiableList(list);
  }

  static final Element asElement(final TypeMirror t, final boolean generateSyntheticElements) {
    // TypeMirror#asElement() says:
    //
    //   "Returns the element corresponding to a type. The type may be
    //   a DeclaredType or TypeVariable. Returns null if the type is
    //   not one with a corresponding element."
    //
    // This does not correspond at *all* to the innards of javac,
    // where nearly every type has an associated element, even where
    // it makes no sense.  For example, error types, intersection
    // types, executable types (!), primitive types and wildcard types
    // (which aren't even types!) all have elements somehow, but these
    // are not in the lang model.
    //
    // Much of javac's algorithmic behavior is based on most types
    // having elements, even where the elements make no sense.  In
    // fact, the only types in javac that have no elements at all are:
    //
    // * no type
    // * void type
    // * null type
    // * unknown type
    //
    // Symbols in javac do not override their equals()/hashCode()
    // methods, so no two symbols are ever the same.
    //
    // We blend all these facts together and set up synthetic elements
    // for types that, in the lang model, don't have them, but do have
    // them behind the scenes in javac.  We use a WeakHashMap to
    // associate TypeMirror instances with their synthetic elements.
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
          // The compiler uses exactly one synthetic element for all
          // array types.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticArrayElement.INSTANCE);
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
          return syntheticElements.computeIfAbsent(t, s -> SyntheticExecutableElement.INSTANCE);
        }
      }
      */
      return null;

    case WILDCARD:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for all
          // wildcard types.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticWildcardElement.INSTANCE);
        }
      }
      return null;

    case BOOLEAN:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.BOOLEAN);
        }
      }
      return null;

    case BYTE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.BYTE);
        }
      }
      return null;

    case CHAR:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.CHAR);
        }
      }
      return null;

    case DOUBLE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.DOUBLE);
        }
      }
      return null;

    case FLOAT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.FLOAT);
        }
      }
      return null;

    case INT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.INT);
        }
      }
      return null;

    case LONG:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.LONG);
        }
      }
      return null;

    case SHORT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, s -> SyntheticPrimitiveElement.SHORT);
        }
      }
      return null;

    case INTERSECTION:
    case MODULE:
    case PACKAGE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler uses one instance of a bogus element for
          // each instance of one of these types.
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

  static final boolean canonical(final TypeMirror t) {
    final Element e = asElement(t, false);
    return e == null || t == e.asType();
  }

  @SuppressWarnings("unchecked")
  static final <T extends TypeMirror> T canonicalType(final T t) {
    final Element e = asElement(t, false);
    return e == null ? t : (T)e.asType();
  }

  private static final boolean capturedTypeVariable(final TypeMirror t) {
    switch (t.getKind()) {
    case TYPEVAR:
      return t instanceof SyntheticCapturedType;
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  static final boolean erased(final TypeMirror t) {
    return t instanceof DefaultDeclaredType ddt && ddt.erased();
  }
  
  static final TypeMirror extendsBound(final TypeMirror t) {
    // See
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143.
    switch (t.getKind()) {
    case WILDCARD:
      return extendsBound((WildcardType)t);
    default:
      return t;
    }
  }

  static final TypeMirror extendsBound(final WildcardType w) {
    // See
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143
    switch (w.getKind()) {
    case WILDCARD:
      TypeMirror superBound = w.getSuperBound();
      if (superBound == null) {
        // Unbounded or upper-bounded.
        final TypeMirror extendsBound = w.getExtendsBound();
        if (extendsBound == null) {
          // Unbounded, so upper bound is Object.
          return ObjectConstruct.JAVA_LANG_OBJECT_TYPE;
        } else {
          // Upper-bounded.
          assert
            extendsBound.getKind() == TypeKind.ARRAY ||
            extendsBound.getKind() == TypeKind.DECLARED ||
            extendsBound.getKind() == TypeKind.TYPEVAR;
          // No need to recurse, because we know extendsBound cannot be
          // a wildcard, so we just return it.
          return extendsBound;
        }
      } else if (superBound.getKind() == TypeKind.TYPEVAR) {
        // Lower-bounded with a type variable.  For some reason the
        // compiler special-cases this.
        //
        // See
        // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L138,
        // particularly "w.bound.getUpperBound()".  This seems very
        // wrong.  See also
        // https://github.com/openjdk/jdk/commit/849ed753e2052eade5193e30faa8a13a9ec507c9.
        superBound = ((TypeVariable)superBound).getUpperBound();
        return superBound == null ? ObjectConstruct.JAVA_LANG_OBJECT_TYPE : superBound;
      } else {
        return ObjectConstruct.JAVA_LANG_OBJECT_TYPE;
      }      
    default:
      throw new IllegalArgumentException("w: " + w);
    }
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

  private static final boolean isInterface(final Element e) {    
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case INTERFACE:
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
    final TypeKind kind = t.getKind();
    switch (kind) {
    default:
      return kind.isPrimitive();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final boolean isStatic(final Element e) {
    return e.getModifiers().contains(Modifier.STATIC);
  }

  // See
  // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1154-L1164
  final boolean raw(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return raw(((ArrayType)t).getComponentType());
    case DECLARED:
      final TypeMirror canonicalType = canonicalType(t);
      return
        t != canonicalType && // t is synthetic and
        hasTypeArguments(canonicalType) && // the canonical type has type arguments and
        !hasTypeArguments(t); // t does not have type arguments
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  static final TypeMirror superBound(final TypeMirror t) {
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L157-L167
    switch (t.getKind()) {
    case WILDCARD:
      final TypeMirror superBound = ((WildcardType)t).getSuperBound();
      return superBound == null ? DefaultNullType.INSTANCE : superBound;
    default:
      return t;
    }
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
      super(name, ElementKind.OTHER, null, Set.of(), null, null);
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
      return AnnotatedName.of(DefaultName.of()); // TODO if it turns out to be important
    }

  }

}
