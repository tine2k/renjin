/**
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.sexp;

import org.renjin.eval.Context;

/**
 * Base interface for all R data types.
 */
public interface SEXP {

  /**
   * Returns the number of elements in this expression. All R objects are considered
   * to have length,
   *
   * @return the number of elements in this expression.
   */
  public int length();

  boolean hasAttributes();

  AttributeMap getAttributes();

  String getTypeName();

  void accept(SexpVisitor visitor);


  /**
   * 
   * @return true if this is a double vector, or an integer vector that is not a factor.
   */
  boolean isNumeric();


  /**
   * Coerces this {@code SEXP} to a single logical value
   * @return
   */
  Logical asLogical();

  /**
   * Coerces this {@code SEXP} to a single double value.
   */
  double asReal();


  String asString();

  /**
   *
   * @return TRUE if the object x has the R internal OBJECT bit set, and FALSE otherwise.
   * The OBJECT bit is set when a "class" attribute is added and removed when that attribute
   *  is removed, so this is a very efficient way to check if an object has a class attribute.
   */
  boolean isObject();

  /**
   * 
   * R possesses a simple generic function mechanism which can be used
   * for an object-oriented style of programming.  Method dispatch
   * takes place based on the class(es) of the first argument to the
   * generic function or of the object supplied as an argument to
   *  ‘UseMethod’ or ‘NextMethod’.
   *
   * 
   *
   * @return the R language class of this expression
   */
  StringVector getS3Class();

  String getImplicitClass();
  
  boolean inherits(String sClassName);

  /**
   *
   * @return the {@link StringVector} containing the element's names, or {@code NULL} if
   * this expression has no explicit {@code names} attribute.
   * 
   * <p>Note that in the case of a one-dimensional array, this method will return the 
   * value of {@code dimnames(this)[[1]]}</p>
   */
  AtomicVector getNames();

  /**
   * 
   * @return true if this SEXP has a names, including dimnames for a one-dimensional attribute.
   */
  boolean hasNames();
  

  /**
   * @param index zero-based index
   * @return the name of the element at index {@code index}, or the empty string
   * if the element has no name
   */
  String getName(int index);
  
  /**
   * Searches the list of this expression's {@link org.renjin.primitives.Attributes#NAMES} attribute for the
   * provided {@code name}.
   *
   * @param name the name for which to search
   * @return  the index of the matching name, or -1 if
   * no match is found.
   */
  int getIndexByName(String name);

  /**
   * Returns the value of the attribute named {@code name} for this
   * SEXP. Attributes are like metadata and can be associated with
   * most types of {@code SEXP}s.
   *
   * @param name the name of the attribute
   * @return  the value of the attribute, or {@code Null.INSTANCE} if the
   * attribute is not set.
   */
  SEXP getAttribute(Symbol name);

  /**
   * Copies this {@code SEXP}, sets the value of the attribute indicated
   * by {@code name}, and returns the copied object.
   *
   * @param attributeName
   * @param value  the new value, or {@code Null.INSTANCE} if the attribute should be removed (if present)
   * @return a copy of this {@code SEXP}
   */
  SEXP setAttribute(String attributeName, SEXP value);

  /**
   * Copies this {@code SEXP}, sets the value of the attribute indicated
   * by {@code name}, and returns the copied object.
   *
   * @param attributeName
   * @param value  the new value, or {@code Null.INSTANCE} if the attribute should be removed (if present)
   * @return a copy of this {@code SEXP}
   */  
  SEXP setAttribute(Symbol attributeName, SEXP value);
  
  /**
   * Replaces all of this {@code SEXP}'s attributes with the attributes
   * specified by {@code attributes}
   *
   * @param attributes a new collection of attributes
   * @return a copy of this {@code SEXP} with the new attributes
   */
  SEXP setAttributes(AttributeMap attributes);

  SEXP setAttributes(AttributeMap.Builder attributes);


  /**
   * @param index zero-based index of the element
   * @return the element at {@code index} as a {@link org.renjin.sexp.SEXP},
   * wrapping the element if necessary in a new {@link org.renjin.sexp.AtomicVector} if necessary
   */
  <S extends SEXP> S getElementAsSEXP(int index);

  
  SEXP force(Context context);

}