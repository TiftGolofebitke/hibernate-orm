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
package org.hibernate.ejb.criteria;

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

/**
 * Models a non-collection property join.
 *
 * @author Steve Ebersole
 */
public class JoinImpl<Z, X> extends FromImpl<Z, X> implements JoinImplementors.JoinImplementor<Z,X> {
	// TODO : keep track or whether any non-identifier properties get dereferenced
	// for join optimization like in HQL

	// TODO : do we need (or is it easier with) a separate "component join impl"?

	// TODO : cleanup these ctors, ugh...

	private final ManagedType<X> managedType;
	private final JoinType joinType;

	@SuppressWarnings({ "unchecked" })
	public JoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathImpl<Z> lhs,
			Attribute<? super Z, ?> joinProperty,
			JoinType joinType) {
		super(
				criteriaBuilder,
				javaType,
				lhs,
				joinProperty,
				criteriaBuilder.getEntityManagerFactory().getMetamodel().managedType( javaType )
		);
		this.managedType = getManagedType();
		this.joinType = joinType;
	}

	@SuppressWarnings({ "unchecked" })
	protected ManagedType<X> getManagedType() {
		return (ManagedType<X>) getModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public From<?, Z> getParent() {
		// AFAICT, only "froms" (specifically roots and joins) can be the parent of a join.
		return ( From<?, Z> ) getParentPath();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Attribute<? super Z, ?> getAttribute() {
		return (Attribute<? super Z, ?>) super.getAttribute();
	}

	/**
	 * {@inheritDoc}
	 */
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected Attribute<X, ?> getAttribute(String name) {
		return (Attribute<X, ?>) managedType.getAttribute( name );
	}

	@SuppressWarnings({ "unchecked" })
	public JoinImplementors.JoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery) {
		JoinImpl<Z,X> correlation = new JoinImpl<Z,X>(
				queryBuilder(),
				getJavaType(),
				(PathImpl<Z>)getParentPath(),
				getAttribute(),
				getJoinType()
		);
		correlation.defineJoinScope( subquery.getJoinScope() );
		correlation.correlationParent = this;
		return correlation;
	}

	private From<Z,X> correlationParent;

	/**
	 * {@inheritDoc}
	 */
	public boolean isCorrelated() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public From<Z,X> getCorrelationParent() {
		return correlationParent;
	}

	public String renderTableExpression(CriteriaQueryCompiler.RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		( (FromImpl) getParent() ).prepareAlias( renderingContext );
		return getParent().getAlias() + '.' + getAttribute().getName() + " as " + getAlias();
	}
}
