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

import java.util.Collection;
import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.CollectionExpression;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class MemberOfPredicate<E, C extends Collection<E>>
		extends AbstractSimplePredicate {

	private final Expression<E> elementExpression;
	private final CollectionExpression<C> collectionExpression;

	public MemberOfPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<E> elementExpression,
			CollectionExpression<C> collectionExpression) {
		super(queryBuilder);
		this.elementExpression = elementExpression;
		this.collectionExpression = collectionExpression;
	}

	public MemberOfPredicate(
			QueryBuilderImpl queryBuilder,
			E element,
			CollectionExpression<C> collectionExpression) {
		this(
				queryBuilder,
				new LiteralExpression<E>( queryBuilder, element ),
				collectionExpression
		);
	}

	public CollectionExpression<C> getCollectionExpression() {
		return collectionExpression;
	}

	public Expression<E> getElementExpression() {
		return elementExpression;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getCollectionExpression(), registry );
		Helper.possibleParameter( getElementExpression(), registry );
	}

}
