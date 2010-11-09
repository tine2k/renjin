/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package r.lang.primitive.types;

import r.lang.PairListExp;
import r.lang.SEXP;

import java.util.ArrayList;

/**
 * Coerces a list of arguments to a {@code StringExp}
 */
public class CoerceArgToString extends AbstractCoerceToString {

  public CoerceArgToString(SEXP exp) {
    super(new ArrayList<String>());
    exp.accept(this);
  }

  @Override
  public void visit(PairListExp listExp) {
    // as.character does to recurse infinitely
    // if there are nested lists or other items, we add them as deparsed strings
    CoerceNestedListToString subVisitor = new CoerceNestedListToString(values);
    for(SEXP exp : listExp) {
      exp.accept(subVisitor);
    }
  }
}