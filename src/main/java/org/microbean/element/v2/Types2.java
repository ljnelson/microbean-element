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

import javax.lang.model.element.AnnotationMirror;

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
    // Although, see
    // https://github.com/openjdk/jdk/blob/jdk-20%2B12/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacTypes.java#L76,
    // which *is* in the lang model and will return an element for an
    // intersection type.  What a mess.
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
          return syntheticElements.computeIfAbsent(t, arrayType -> SyntheticArrayElement.INSTANCE);
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
          return syntheticElements.computeIfAbsent(t, wildcardType -> SyntheticWildcardElement.INSTANCE);
        }
      }
      return null;

    case BOOLEAN:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, booleanType -> SyntheticPrimitiveElement.BOOLEAN);
        }
      }
      return null;

    case BYTE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, byteType -> SyntheticPrimitiveElement.BYTE);
        }
      }
      return null;

    case CHAR:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, charType -> SyntheticPrimitiveElement.CHAR);
        }
      }
      return null;

    case DOUBLE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, doubleType -> SyntheticPrimitiveElement.DOUBLE);
        }
      }
      return null;

    case FLOAT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, floatType -> SyntheticPrimitiveElement.FLOAT);
        }
      }
      return null;

    case INT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, intType -> SyntheticPrimitiveElement.INT);
        }
      }
      return null;

    case LONG:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, longType -> SyntheticPrimitiveElement.LONG);
        }
      }
      return null;

    case SHORT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a
          // given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, shortType -> SyntheticPrimitiveElement.SHORT);
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

  // Does the supplied TypeMirror represent a type that is being
  // declared (rather than used)?
  static final boolean declaration(final TypeMirror t) {
    final Element e = asElement(t, false);
    return e == null || t == e.asType();
  }

  // Return the TypeMirror representing the declaration whose type may
  // currently be being used.  E.g. given a type representing
  // List<String>, return List<E> (from List<String>'s usage of
  // List<E>)
  //
  // I don't like this name.
  @SuppressWarnings("unchecked")
  static final <T extends TypeMirror> T declaredTypeMirror(final T t) {
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
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143
    switch (t.getKind()) {
    case WILDCARD:
      final WildcardType w = (WildcardType)t;
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
            extendsBound.getKind() == TypeKind.TYPEVAR :
          "extendsBound kind: " + extendsBound.getKind();
          return extendsBound;
        }
      } else {
        // See
        // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L138.
        // A (javac) WildcardType's bound field is NOT the same as its
        // type field.  The lang model only exposes the type field.
        //
        // Consider some context like this:
        //
        //   interface Foo<T extends Serializable> {}
        //
        // And then a wildcard like this:
        //
        //   Foo<? super String> f;
        //
        // The (javac) WildcardType's bound field will be initialized
        // to T extends Serializable.  Its type field will be
        // initialized to String.  wildUpperBound(thisWildcardType)
        // will return Serializable.class, not Object.class.
        //
        // The lang model makes this impossible, because bound is not
        // exposed, and getSuperBound() doesn't do anything fancy to
        // return it.
        //
        // Dan Smith writes:
        //
        // "It turns out 'bound' is used to represent the
        // *corresponding type parameter* of the wildcard, where
        // additional bounds can be found (for things like capture
        // conversion), not anything about the wildcard itself."
        //
        // And:
        //
        // "Honestly, it's a little sketchy that the compiler
        // internals are doing this at all. I'm not totally sure that,
        // for example, uses of 'wildUpperBound' aren't violating the
        // language spec somewhere. For lang.model, no, the 'bound'
        // field is not at all part of the specified API, so shouldn't
        // have any impact on API behavior.
        //
        // "(The right thing to do, per the language spec, to
        // incorporate the corresponding type parameter bounds, is to
        // perform capture on the wildcard's enclosing parameterized
        // type, and then work with the resulting capture type
        // variables.)"
        //
        // So bound gets set to T extends Serializable.
        // There is no way to extract T extends Serializable from a
        // javax.lang.model.type.WildcardType, and without that
        // ability we have no other information, so we must return
        // Object.class.
        return ObjectConstruct.JAVA_LANG_OBJECT_TYPE;
      }
    default:
      return t;
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

  static final boolean isInterface(final TypeMirror t) {
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
      final TypeMirror declaredTypeMirror = declaredTypeMirror(t);
      return
        t != declaredTypeMirror && // t is a parameterized type, i.e. a type usage, and
        hasTypeArguments(declaredTypeMirror) && // the type it parameterizes has type arguments (type variables declared by type parameters) and
        !hasTypeArguments(t); // t does not supply type arguments
    default:
      return false;
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  static final boolean parameterized(final TypeMirror t) {
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

  static final WildcardType unboundedWildcardType() {
    return DefaultWildcardType.UNBOUNDED;
  }

  static final WildcardType unboundedWildcardType(final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.unboundedWildcardType(annotationMirrors);
  }

  static final WildcardType upperBoundedWildcardType(final TypeMirror upperBound,
                                                     final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultWildcardType.upperBoundedWildcardType(upperBound, annotationMirrors);
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
