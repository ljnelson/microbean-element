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

import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.SimpleTypeVisitor14;

// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4590-L4690
final class AdaptingVisitor extends SimpleTypeVisitor14<Void, TypeMirror> {

  AdaptingVisitor() {
    super();
  }
  
}
