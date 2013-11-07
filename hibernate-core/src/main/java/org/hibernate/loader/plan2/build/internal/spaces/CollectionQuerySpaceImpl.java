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
package org.hibernate.loader.plan2.build.internal.spaces;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan2.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class CollectionQuerySpaceImpl extends AbstractQuerySpace implements ExpandingCollectionQuerySpace {
	private final CollectionPersister persister;
	private final CollectionPropertyMapping propertyMapping;

	public CollectionQuerySpaceImpl(
			CollectionPersister persister,
			String uid,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.COLLECTION, querySpaces, canJoinsBeRequired, sessionFactory );
		this.persister = persister;
		this.propertyMapping = new CollectionPropertyMapping( (QueryableCollection) persister );
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return propertyMapping;
	}

	@Override
	public ExpandingEntityQuerySpace addIndexEntityQuerySpace(
			final EntityPersister indexPersister) {
		final boolean required = canJoinsBeRequired();
		final String entityQuerySpaceUid = getExpandingQuerySpaces().generateImplicitUid();
		final ExpandingEntityQuerySpace entityQuerySpace = getExpandingQuerySpaces().makeEntityQuerySpace(
				entityQuerySpaceUid,
				indexPersister,
				required
		);

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createEntityJoin(
				this,
				// collection persister maps its index (through its PropertyMapping contract) as non-prefixed
				CollectionPropertyNames.COLLECTION_INDICES,
				entityQuerySpace,
				required,
				(EntityType) persister.getIndexType(),
				sessionFactory()
		);
		internalGetJoins().add( join );

		return entityQuerySpace;
	}

	@Override
	public ExpandingCompositeQuerySpace addIndexCompositeQuerySpace(
			CompositeType compositeType) {
		final String compositeQuerySpaceUid = getExpandingQuerySpaces().generateImplicitUid();
		final ExpandingCompositeQuerySpace compositeQuerySpace = getExpandingQuerySpaces().makeCompositeQuerySpace(
				compositeQuerySpaceUid,
				new CompositePropertyMapping(
						compositeType,
						(PropertyMapping) getCollectionPersister(),
						"index"
				),
				canJoinsBeRequired()
		);

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				CollectionPropertyNames.COLLECTION_INDICES,
				compositeQuerySpace,
				canJoinsBeRequired(),
				compositeType
		);
		internalGetJoins().add( join );

		return compositeQuerySpace;
	}

	@Override
	public ExpandingEntityQuerySpace addElementEntityQuerySpace(
			final EntityPersister elementPersister) {
		final String entityQuerySpaceUid = getExpandingQuerySpaces().generateImplicitUid();
		final ExpandingEntityQuerySpace entityQuerySpace = getExpandingQuerySpaces().makeEntityQuerySpace(
				entityQuerySpaceUid,
				elementPersister,
				canJoinsBeRequired()
		);

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createEntityJoin(
				this,
				// collection persister maps its elements (through its PropertyMapping contract) as non-prefixed
				CollectionPropertyNames.COLLECTION_ELEMENTS,
				entityQuerySpace,
				canJoinsBeRequired(),
				(EntityType) persister.getElementType(),
				sessionFactory()
		);
		internalGetJoins().add( join );

		return entityQuerySpace;
	}

	@Override
	public ExpandingCompositeQuerySpace addElementCompositeQuerySpace(
			CompositeType compositeType) {
		final String compositeQuerySpaceUid = getExpandingQuerySpaces().generateImplicitUid();

		final ExpandingCompositeQuerySpace compositeQuerySpace = getExpandingQuerySpaces().makeCompositeQuerySpace(
				compositeQuerySpaceUid,
				new CompositePropertyMapping(
						compositeType,
						(PropertyMapping) getCollectionPersister(),
						""
				),
				canJoinsBeRequired()
		);
		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				// collection persister maps its elements (through its PropertyMapping contract) as non-prefixed
				CollectionPropertyNames.COLLECTION_ELEMENTS,
				compositeQuerySpace,
				canJoinsBeRequired(),
				compositeType
		);
		internalGetJoins().add( join );

		return compositeQuerySpace;
	}
}
