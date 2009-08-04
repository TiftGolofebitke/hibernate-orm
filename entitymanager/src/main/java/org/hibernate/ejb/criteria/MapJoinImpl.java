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
package org.hibernate.ejb.criteria;

import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.Type.PersistenceType;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.Map}, whose elements
 * are associations.
 *
 * @author Steve Ebersole
 */
public class MapJoinImpl<O,K,V>
		extends JoinImpl<O,V>
		implements MapJoin<O,K,V> {

	public MapJoinImpl(
			QueryBuilderImpl queryBuilder,
			Class<V> javaType,
			PathImpl<O> lhs,
			MapAttribute<? super O, K, V> joinProperty,
			JoinType joinType) {
		super(queryBuilder, javaType, lhs, joinProperty, joinType);
	}

	@Override
	public MapAttribute<? super O, K, V> getAttribute() {
		return (MapAttribute<? super O, K, V>) super.getAttribute();
	}

	@Override
	public MapAttribute<? super O, K, V> getModel() {
		return getAttribute();
	}

	/**
	 * {@inheritDoc}
	 */
	public Join<Map<K, V>, K> joinKey() {
		return joinKey( DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public Join<Map<K, V>, K> joinKey(JoinType jt) {
		if ( PersistenceType.BASIC.equals( getAttribute().getKeyType().getPersistenceType() ) ) {
			throw new BasicPathUsageException( "Cannot join to map key of basic type", getAttribute() );
        }

		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final MapKeyHelpers.MapPath<K,V> mapKeySource = new MapKeyHelpers.MapPath<K,V>(
				queryBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute(),
				getParentPath().getModel()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( queryBuilder(), getAttribute() );
		final Join<Map<K, V>, K> join = new MapKeyHelpers.MapKeyJoin<K,V>(
				queryBuilder(),
				mapKeySource,
				mapKeyAttribute,
				jt
		);

		return join;
	}

	/**
	 * {@inheritDoc}
	 */
	public Path<K> key() {
		final MapKeyHelpers.MapPath<K,V> mapKeySource = new MapKeyHelpers.MapPath<K,V>(
				queryBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute(),
				getParentPath().getModel()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( queryBuilder(), getAttribute() );
		return new MapKeyHelpers.MapKeyPath( queryBuilder(), mapKeySource, mapKeyAttribute );
	}

	/**
	 * {@inheritDoc}
	 */
	public Path<V> value() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression<Entry<K, V>> entry() {
		// TODO : ???
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
