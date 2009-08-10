/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.criteria.predicate;

import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.UnaryOperatorExpression;

/**
 * Defines a {@link javax.persistence.criteria.Predicate} for checking the
 * nullness state of an expression, aka an <tt>IS (NOT?) NULL</tt> predicate.
 * <p/>
 * The <tt>NOT NULL</tt> form can be built by calling the constructor and then
 * calling {@link #negate}.
 *
 * @author Steve Ebersole
 */
public class NullnessPredicate extends AbstractSimplePredicate implements UnaryOperatorExpression<Boolean> {
	private final Expression<?> operand;

	/**
	 * Constructs the affirmitive form of nullness checking (<i>IS NULL</i>).  To
	 * construct the negative form (<i>IS NOT NULL</i>) call {@link #negate} on the
	 * constructed instance.
	 *
	 * @param queryBuilder The query builder from whcih this originates.
	 * @param expression The expression to check.
	 */
	public NullnessPredicate(QueryBuilderImpl queryBuilder, Expression<?> operand) {
		super( queryBuilder );
		this.operand = operand;
	}

	public Expression<?> getOperand() {
		return operand;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getOperand(), registry );
	}
}
