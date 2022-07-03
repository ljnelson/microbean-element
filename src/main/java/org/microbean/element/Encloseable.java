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

import javax.lang.model.element.Element;

/**
 * An interface indicating that its implementation may be {@linkplain
 * #setEnclosingElement(Element) enclosed} by an {@linkplain
 * #getEnclosingElement() enclosing <code>Element</code>}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #setEnclosingElement(Element)
 */
public interface Encloseable {

  /**
   * Returns the {@link Element} enclosing this {@link Encloseable},
   * or {@code null} if there is no such {@link Element}.
   */
  public Element getEnclosingElement();

  /**
   * Sets this {@link Encloseable}'s enclosing {@link Element},
   * provided that it has not already been set.
   *
   * @param enclosingElement the {@link Element}; may be {@code null}
   *
   * @exception IllegalArgumentException if {@code enclosingElement}
   * is unsuitable in some way
   *
   * @exception IllegalStateException if an enclosing element has
   * already been set, by any thread
   */
  public void setEnclosingElement(final Element enclosingElement);

}
