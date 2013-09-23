/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan2.spi.CollectionReference;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * @author Steve Ebersole
 */
public class CollectionFetchableIndexEntityGraph extends AbstractEntityReference implements CollectionFetchableIndex {
	private final CollectionReference collectionReference;
	private final EntityQuerySpace entityQuerySpace;

	public CollectionFetchableIndexEntityGraph(
			CollectionReference collectionReference,
			Join entityJoin) {
		super(
				(ExpandingEntityQuerySpace) entityJoin.getRightHandSide(),
				collectionReference.getPropertyPath().append( "<index>" )
		);

		this.collectionReference = collectionReference;
		this.entityQuerySpace = (EntityQuerySpace) entityJoin.getRightHandSide();
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
	}
}
