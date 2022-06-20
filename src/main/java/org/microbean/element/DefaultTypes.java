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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.lang.model.util.Types;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public final class DefaultTypes implements Types {

  private DefaultTypes() {
    super();
  }

  @Override // Types
  public final Element asElement(final TypeMirror t) {
    // Returns the element corresponding to a type. The type may be a
    // DeclaredType or TypeVariable. Returns null if the type is not
    // one with a corresponding element.
    switch (t.getKind()) {
    case DECLARED:
      return ((DeclaredType)t).asElement();
    case TYPEVAR:
      return ((TypeVariable)t).asElement();
    default:
      return null;
    }
  }

  @Override // Types
  public final TypeMirror asMemberOf(final DeclaredType containing, final Element element) {
    // Returns the type of an element when that element is viewed as a
    // member of, or otherwise directly contained by, a given
    // type. For example, when viewed as a member of the parameterized
    // type Set<String>, the Set.add method is an ExecutableType whose
    // parameter is of type String.
    //
    // https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacTypes.java#L295
    if (element.getModifiers().contains(Modifier.STATIC)) {
      return element.asType();
    }

    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final TypeElement boxedClass(final PrimitiveType p) {
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final TypeMirror capture(final TypeMirror t) {
    // https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4388
    if (t.getKind() == TypeKind.DECLARED) {
      return this.captureDeclared((DeclaredType)t);
    }
    return t;
  }

  private final TypeMirror captureDeclared(DeclaredType t) {
    // https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4392
    final TypeMirror enclosingType = t.getEnclosingType();
    if (enclosingType.getKind() != TypeKind.NONE) {
      assert enclosingType.getKind() == TypeKind.DECLARED;
      final TypeMirror capturedEnclosingType = this.capture(enclosingType);
      if (capturedEnclosingType != enclosingType) {
        // i.e. something happened

      }
    }

    // https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4400
    final List<? extends TypeParameterElement> typeParameters = ((TypeElement)t.asElement()).getTypeParameters();
    if (typeParameters.isEmpty()) {
      return t;
    }
    final int typeParametersSize = typeParameters.size();
    final List<? extends TypeMirror> T = t.getTypeArguments();
    final int typeArgumentsSize = T.size();
    if (typeArgumentsSize < typeParametersSize) {
      return t;
    } else if (typeArgumentsSize > typeParametersSize) {
      throw new IllegalArgumentException("t: " + t);
    }

    // https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4403
    final List<? extends TypeMirror> A = ((DeclaredType)t.asElement().asType()).getTypeArguments();
    final List<? extends TypeMirror> S = freshTypeVariables(T);
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
      final DefaultDeclaredType returnValue = new DefaultDeclaredType(t.getEnclosingType(), S, null);
      returnValue.element(t.asElement());
      return returnValue;
    }
    return t;
  }

  private final TypeMirror subst(final TypeMirror t, final List<? extends TypeMirror> from, final List<? extends TypeMirror> to) {
    throw new UnsupportedOperationException();
  }

  private final TypeMirror glb(final TypeMirror t, final TypeMirror s) {
    if (s == null) {
      return t;
    }
    throw new UnsupportedOperationException();
  }

  private static final List<? extends TypeMirror> freshTypeVariables(final List<? extends TypeMirror> typeArguments) {
    final List<TypeMirror> list = new ArrayList<>(typeArguments.size());
    for (final TypeMirror typeArgument : typeArguments) {
      if (typeArgument.getKind() == TypeKind.WILDCARD) {
        list.add(new DefaultCapturedType((WildcardType)typeArgument));
      } else {
        list.add(typeArgument);
      }
    }
    return Collections.unmodifiableList(list);
  }

  @Override // Types
  public final boolean contains(final TypeMirror typeArgument1, final TypeMirror typeArgument2) {
    // JLS 4.5.1, i.e. containing types section, i.e. wildcards
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final List<? extends TypeMirror> directSupertypes(final TypeMirror t) {
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final TypeMirror erasure(final TypeMirror t) {
    return this.erase(t);
  }

  private final TypeMirror erase(final TypeMirror t) {
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
    if (t != null) {
      switch (t.getKind()) {
      case DECLARED:
        return this.eraseDeclared((DeclaredType)t);
      case ARRAY:
        return this.eraseArray((ArrayType)t);
      case TYPEVAR:
        return this.eraseTypeVariable((TypeVariable)t);
      default:
        break;
      }
    }
    return t;
  }

  private final TypeMirror eraseDeclared(final DeclaredType t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of a parameterized type (§4.5) G<T1,…,Tn> is |G|.
    //
    // The erasure of a nested type T.C is |T|.C.
    final List<?> typeArguments = t.getTypeArguments();
    final TypeMirror enclosingType = t.getEnclosingType();
    if (typeArguments.isEmpty()) {
      if (enclosingType.getKind() == TypeKind.NONE) {
        // The erasure of t's enclosing type is simply its enclosing type.
        // t has no type arguments.
        // So t is already erased.
        return t;
      } else {
        return this.erase(this.getDeclaredType(enclosingType, t.asElement(), List.of()));
      }
    } else if (enclosingType.getKind() == TypeKind.NONE) {
      return this.erase(this.getDeclaredType(enclosingType, t.asElement(), t.getTypeArguments()));
    } else {
      return this.erase(this.getDeclaredType(this.erase(enclosingType), t.asElement(), t.getTypeArguments()));
    }
  }

  private final TypeMirror eraseArray(final ArrayType t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of an array type T[] is |T|[].
    return this.getArrayType(this.erase(t.getComponentType()));
  }

  private final TypeMirror eraseTypeVariable(final TypeVariable t) {
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6
    // 4.6. Type Erasure
    //
    // The erasure of a type variable (§4.4) is the erasure of its
    // leftmost bound.
    return this.erase(t.getUpperBound());
  }

  @Override // Types
  public final ArrayType getArrayType(final TypeMirror componentType) {
    // TODO: copy annotation mirrors of the componentType into the array type?
    return DefaultArrayType.of(componentType);
  }

  public final DeclaredType getDeclaredType(final TypeElement typeElement) {
    return this.getDeclaredType(this.getNoType(TypeKind.NONE), typeElement, List.of());
  }

  @Override // Types
  public final DeclaredType getDeclaredType(final TypeElement typeElement, final TypeMirror... typeArguments) {
    return
      this.getDeclaredType(this.getNoType(TypeKind.NONE),
                           typeElement,
                           typeArguments == null || typeArguments.length <= 0 ? List.of() : Arrays.asList(typeArguments));
  }

  @Override // Types
  public final DeclaredType getDeclaredType(final DeclaredType enclosingType,
                                            final TypeElement typeElement,
                                            final TypeMirror... typeArguments) {
    return
      this.getDeclaredType(enclosingType,
                           typeElement,
                           typeArguments == null || typeArguments.length <= 0 ? List.of() : Arrays.asList(typeArguments));
  }

  public final DeclaredType getDeclaredType(final DeclaredType enclosingType,
                                            final TypeElement typeElement,
                                            final List<? extends TypeMirror> typeArguments) {
    return this.getDeclaredType(enclosingType, typeElement, typeArguments);
  }

  public final DeclaredType getDeclaredType(final NoType enclosingType,
                                            final TypeElement typeElement,
                                            final List<? extends TypeMirror> typeArguments) {
    return this.getDeclaredType(enclosingType, typeElement, typeArguments);
  }

  private final DeclaredType getDeclaredType(final TypeMirror enclosingType,
                                             final Element typeElement,
                                             final List<? extends TypeMirror> typeArguments) {
    final int typeParameterCount;
    if (typeElement instanceof TypeElement te) {
      typeParameterCount = te.getTypeParameters().size();
    } else {
      typeParameterCount = 0;
    }
    if (typeParameterCount > 0 && typeParameterCount != typeArguments.size()) {
      throw new IllegalArgumentException("typeArguments: " + typeArguments + "; typeParameters: " + ((TypeElement)typeElement).getTypeParameters());
    }
    final DefaultDeclaredType returnValue =
      new DefaultDeclaredType(enclosingType == null ? this.getNoType(TypeKind.NONE) : enclosingType,
                              typeArguments,
                              null); // TODO: what about the annotations?
    returnValue.element(typeElement);
    return returnValue;
  }

  @Override // Types
  public final NoType getNoType(final TypeKind kind) {
    switch (kind) {
    case MODULE:
      return DefaultNoType.MODULE;
    case NONE:
      return DefaultNoType.NONE;
    case PACKAGE:
      return DefaultNoType.PACKAGE;
    case VOID:
      return DefaultNoType.VOID;
    default:
      throw new IllegalArgumentException("Unexpected kind: " + kind);
    }
  }

  @Override // Types
  public final NullType getNullType() {
    return DefaultNullType.INSTANCE;
  }

  @Override // Types
  public final PrimitiveType getPrimitiveType(final TypeKind kind) {
    switch (kind) {
    case BOOLEAN:
      return DefaultPrimitiveType.BOOLEAN;
    case BYTE:
      return DefaultPrimitiveType.BYTE;
    case CHAR:
      return DefaultPrimitiveType.CHAR;
    case DOUBLE:
      return DefaultPrimitiveType.DOUBLE;
    case FLOAT:
      return DefaultPrimitiveType.FLOAT;
    case INT:
      return DefaultPrimitiveType.INT;
    case LONG:
      return DefaultPrimitiveType.LONG;
    case SHORT:
      return DefaultPrimitiveType.SHORT;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // Types
  public final WildcardType getWildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
    if (superBound == null) {
      if (extendsBound == null) {
        return DefaultWildcardType.unboundedWildcardType();
      }
      return DefaultWildcardType.upperBoundedWildcardType(extendsBound, null);
    } else if (extendsBound == null) {
      return DefaultWildcardType.lowerBoundedWildcardType(superBound, null);
    } else {
      throw new IllegalArgumentException("extendsBound: " + extendsBound + "; superBound: " + superBound);
    }
  }

  @Override // Types
  public final boolean isAssignable(final TypeMirror t1, final TypeMirror t2) {
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final boolean isSameType(final TypeMirror t1, final TypeMirror t2) {
    // This is weird.  The javadoc says:
    //
    // "Caveat: if either of the arguments to this method represents a
    // wildcard, this method will return false. As a consequence, a
    // wildcard is not the same type as itself."
    //
    // But in the compiler, it is implemented here:
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1336-L1341
    //
    // That's just a simple visitor.  Its "visit wildcard" has no
    // special handling:
    //
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1380-L1389
    //
    // ...and if t and s in that method are identical, it will return
    // true it seems to me.
    //
    // See also: https://bugs.openjdk.java.net/browse/JDK-8161277
    // which is fixed but still.
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final boolean isSubsignature(final ExecutableType t1, final ExecutableType t2) {
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final boolean isSubtype(final TypeMirror t1, final TypeMirror t2) {
    throw new UnsupportedOperationException();
  }

  @Override // Types
  public final PrimitiveType unboxedType(final TypeMirror t) {
    // JLS 5.1.8
    throw new UnsupportedOperationException();
  }

  private static final boolean contains0(final List<? extends TypeMirror> list1, final List<? extends TypeMirror> list2) {
    throw new UnsupportedOperationException();
  }
  
  private static final boolean contains0(final TypeMirror t1, final TypeMirror t2) {
    if (t1 == null || t2 == null) {
      return false;
    }
    switch (t1.getKind()) {

    case ERROR:
      return true;

    case WILDCARD:
      final WildcardType wt1 = (WildcardType)t1;
      switch (t2.getKind()) {

      case TYPEVAR:
        return t2 instanceof CapturedType ct2 && sameWildcard(wt1, ct2.getWildcardType());

      case WILDCARD:
        final WildcardType wt2 = (WildcardType)t2;
        if (sameWildcard(wt1, wt2)) {
          return true;
        }
        final TypeMirror ub1 = wt1.getExtendsBound();
        final TypeMirror ub2 = wt2.getExtendsBound();
        final TypeMirror lb1 = wt1.getSuperBound();
        final TypeMirror lb2 = wt2.getSuperBound();
        if (lb1 == null) {
          if (lb2 == null) {
            if (ub1 == null) {
              if (ub2 == null) {
                // Both are unbounded (extends, super)
                return true;
              } else {
                // wt1 is unbounded (extends, super)
                // wt2 is upper-bounded (extends)
              }
            } else if (ub2 == null) {
              // wt1 is upper-bounded (extends)
              // wt2 is unbounded (extends, super)
            } else {
              // wt1 is upper-bounded (extends)
              // wt2 is upper-bounded (extends)
            }
          } else if (ub1 == null) {
            if (ub2 == null) {
              // wt1 is unbounded (extends, super)
              // wt2 is lower-bounded (super)
            } else {
              // wt2 is a bad wildcard (both upper and lower bounds)
              return false;
            }
          } else if (ub2 == null) {
            // wt1 is upper-bounded (extends)
            // wt2 is lower-bounded
          } else {

          }
        } else if (lb2 == null) {
          if (ub1 == null) {
            if (ub2 == null) {
              // wt1 is lower-bounded (super)
              // wt2 is unbounded (extends, super)
            } else {
              // wt1 is lower-bounded (super)
              // wt2 is upper-bounded (extends)
            }
          } else if (ub2 == null) {
            // wt1 is bad (lb1 != null && ub1 != null)
            // wt2 is unbounded (extends, super)
          } else {
            // wt1 is upper-bounded (extends)
            // wt2 is upper-bounded (extends)
          }
        } else if (ub1 == null) {
          if (ub2 == null) {
            // wt1 is lower-bounded (super)
            // wt2 is lower-bounded (super)
          } else {
            // wt1 is lower-bounded (super)
            // wt2 is bad (lb2 != null && ub2 != null)
          }
        } else if (ub2 == null) {
          // wt1 is bad (lb1 != null && ub1 != null)
          // wt2 is lower-bounded (super)
        } else {
          // wt1 is bad (lb1 != null && ub1 != null)
          // wt2 is bad (lb2 != null && ub2 != null)
        }
        throw new UnsupportedOperationException();

      default:
        return represents(t1, t2);
      }

    default:
      return false;
    }
  }

  private static final boolean represents(final TypeMirror t1, final TypeMirror t2) {
    if (t1 == null || t2 == null) {
      return false;
    }
    final TypeKind k1 = t1.getKind();
    if (k1.isPrimitive()) {
      return t1 == t2 || k1 == t2.getKind();
    }
    
    switch (k1) {

    case DECLARED:
      if (t1 == t2) {
        return true;
      }
      final DeclaredType dt1 = (DeclaredType)t1;
      switch (t2.getKind()) {

      case DECLARED:
        final DeclaredType dt2 = (DeclaredType)t2;
        return
          dt1.asElement() == dt2.asElement() &&
          represents(dt1.getEnclosingType(), dt2.getEnclosingType()) && // RECURSIVE
          contains0(dt1.getTypeArguments(), dt2.getTypeArguments());

      case WILDCARD:
        final WildcardType wt2 = (WildcardType)t2;
        if (wt2.getSuperBound() != null && wt2.getExtendsBound() == null) {
          return
            represents(dt1, wildcardUpperBound(wt2)) &&
            represents(dt1, wildcardLowerBound(wt2));
        }
        return false;
        
      default:
        return false;
      }
      
    case TYPEVAR:
      if (t1 == t2) {
        return true;
      }
      final TypeVariable tv1 = (TypeVariable)t1;
      switch (t2.getKind()) {

      case WILDCARD:
        final WildcardType wt2 = (WildcardType)t2;
        return
          wt2.getSuperBound() != null &&
          wt2.getExtendsBound() == null &&
          represents(tv1, wildcardUpperBound(wt2)); // RECURSIVE

      case TYPEVAR:
        throw new AssertionError(); // already covered

      default:
        return false;
      }
      
    case WILDCARD:
      if (t1 == t2) {
        // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacTypes.java#L88-L90
        // says this has to return false, but
        // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1380-L1389
        // doesn't enforce this.
        //
        // The issue is recursion: if we enter here via processing
        // List<? extends Map<? extends String>>, List<? extends Map<?
        // extends String>> I think those are supposed to match.
        //
        // Probably we only need to enforce
        // no-two-wildcards-are-ever-the-same at a higher level.
        // Here, we'll return true.
        return true;
      }
      final WildcardType wt1 = (WildcardType)t1;
      switch (t2.getKind()) {

      case WILDCARD:
        final WildcardType wt2 = (WildcardType)t2;
        final TypeMirror lb1 = wt1.getSuperBound();
        final TypeMirror lb2 = wt2.getSuperBound();
        final TypeMirror ub1 = wt1.getExtendsBound();
        final TypeMirror ub2 = wt2.getExtendsBound();
        if (lb1 == null) {
          if (lb2 == null) {
            if (ub1 == null) {
              // Return true if they're both unbounded
              return ub2 == null;
            } else if (ub2 == null) {
              return false;
            } else {
              // Both upper-bounded.
              return represents(ub1, ub2); // RECURSIVE
            }            
          } else {
            return false;
          }
        } else if (lb2 == null) {
          return false;
        } else if (ub1 == null) {
          if (ub2 == null) {
            // Both lower-bounded.
            return represents(lb1, lb2); // RECURSIVE
          } else {
            // Bad t2 wildcard; has lower bounds and upper bounds?
            return false;
          }
        } else if (ub2 == null) {
          // Bad t1 wildcard; has lower bounds and upper bounds?
          return false;
        } else {
          // Both wildcards are bad; both have upper and lower bounds
          return false;
        }
        
      default:
        return false;

      }
    default:
      return false;
    }    
  }

  private static final boolean sameWildcard(final WildcardType wt1, final WildcardType wt2) {
    if (wt1 == null || wt2 == null || wt1.getKind() != TypeKind.WILDCARD || wt2.getKind() != TypeKind.WILDCARD) {
      return false;
    }
    // This is kind of a guess. javac takes great care to compare
    // wildcard bound *identity*, but I think, given that a wildcard's
    // bound is going to be either a declared type or a type variable,
    // it reduces to represents(t1, t2).
    //
    // See
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1613-L1618
    // and
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1391-L1425
    // and
    // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1361-L1374.
    return represents(wt1, wt2);
  }
  
  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143
  private static final TypeMirror wildcardUpperBound(final TypeMirror t) {
    switch (t.getKind()) {
    case WILDCARD:
      final WildcardType w = (WildcardType)t;
      final TypeMirror lowerBound = w.getSuperBound();
      if (lowerBound == null) {
        final TypeMirror upperBound = w.getExtendsBound();
        return upperBound == null ? DefaultDeclaredType.JAVA_LANG_OBJECT : wildcardUpperBound(upperBound); // RECURSIVE
      } else if (lowerBound.getKind() == TypeKind.TYPEVAR) {
        return ((TypeVariable)lowerBound).getUpperBound();
      } else {
        return DefaultDeclaredType.JAVA_LANG_OBJECT;
      }
    default:
      return t;
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-18+37/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L157-L167
  private static final TypeMirror wildcardLowerBound(final TypeMirror t) {
    switch (t.getKind()) {
    case WILDCARD:
      final WildcardType w = (WildcardType)t;
      final TypeMirror lowerBound = w.getSuperBound();
      return w.getExtendsBound() == null && lowerBound != null ? wildcardLowerBound(lowerBound) : null; // RECURSIVE
    default:
      return t;
    }
  }
  
}
