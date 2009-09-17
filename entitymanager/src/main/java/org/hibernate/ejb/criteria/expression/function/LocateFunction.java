/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Models the ANSI SQL <tt>LOCATE</tt> function.
 *
 * @author Steve Ebersole
 */
public class LocateFunction extends BasicFunctionExpression<Integer> {
	public static final String NAME = "locate";

	private final Expression<String> pattern;
	private final Expression<String> string;
	private final Expression<Integer> start;

	public LocateFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> pattern,
			Expression<String> string,
			Expression<Integer> start) {
		super( queryBuilder, Integer.class, NAME );
		this.pattern = pattern;
		this.string = string;
		this.start = start;
	}

	public LocateFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> pattern,
			Expression<String> string) {
		this( queryBuilder, pattern, string, null );
	}

	public LocateFunction(QueryBuilderImpl queryBuilder, String pattern, Expression<String> string) {
		this(
				queryBuilder,
				new LiteralExpression<String>( queryBuilder, pattern ),
				string,
				null
		);
	}

	public LocateFunction(QueryBuilderImpl queryBuilder, String pattern, Expression<String> string, int start) {
		this(
				queryBuilder,
				new LiteralExpression<String>( queryBuilder, pattern ),
				string,
				new LiteralExpression<Integer>( queryBuilder, start )
		);
	}

	public Expression<String> getPattern() {
		return pattern;
	}

	public Expression<Integer> getStart() {
		return start;
	}

	public Expression<String> getString() {
		return string;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getPattern(), registry );
		Helper.possibleParameter( getStart(), registry );
		Helper.possibleParameter( getString(), registry );
	}
}
