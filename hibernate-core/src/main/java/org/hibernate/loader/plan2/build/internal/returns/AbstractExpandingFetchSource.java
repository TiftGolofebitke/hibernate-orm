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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan2.build.spi.ExpandingSourceQuerySpace;
import org.hibernate.loader.plan2.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * @author Gail Badner
 */
public abstract class AbstractExpandingFetchSource implements ExpandingFetchSource {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final Fetch[] NO_FETCHES = new Fetch[0];

	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final BidirectionalEntityReference[] NO_BIDIRECTIONAL_ENTITY_REFERENCES =
			new BidirectionalEntityReference[0];

	private final ExpandingSourceQuerySpace querySpace;
	private final PropertyPath propertyPath;
	private List<Fetch> fetches;
	private List<BidirectionalEntityReference> bidirectionalEntityReferences;

	public AbstractExpandingFetchSource(ExpandingSourceQuerySpace querySpace, PropertyPath propertyPath) {
		this.querySpace = querySpace;
		this.propertyPath = propertyPath;
	}

	@Override
	public final String getQuerySpaceUid() {
		return querySpace.getUid();
	}

	protected final ExpandingSourceQuerySpace expandingQuerySpace() {
		return querySpace;
	}

	@Override
	public final PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}

	private void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}
		fetches.add( fetch );
	}

	@Override
	public BidirectionalEntityReference[] getBidirectionalEntityReferences() {
		return bidirectionalEntityReferences == null ?
				NO_BIDIRECTIONAL_ENTITY_REFERENCES :
				bidirectionalEntityReferences.toArray(
						new BidirectionalEntityReference[ bidirectionalEntityReferences.size() ]
				);
	}

	private void addBidirectionalEntityReference(BidirectionalEntityReference bidirectionalEntityReference) {
		if ( bidirectionalEntityReferences == null ) {
			bidirectionalEntityReferences = new ArrayList<BidirectionalEntityReference>();
		}
		bidirectionalEntityReferences.add( bidirectionalEntityReference );
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		final EntityType fetchedType = (EntityType) attributeDefinition.getType();
		final EntityPersister fetchedPersister = attributeDefinition.toEntityDefinition().getEntityPersister();

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate EntityPersister [%s] for fetch [%s]",
							fetchedType.getAssociatedEntityName(),
							attributeDefinition.getName()
					)
			);
		}

		final ExpandingEntityQuerySpace entityQuerySpace = querySpace.addEntityQuerySpace(
				attributeDefinition,
				fetchedPersister,
				getQuerySpaces().generateImplicitUid(),
				attributeDefinition.isNullable(),
				shouldIncludeJoin( fetchStrategy )
		);
		final EntityFetch fetch = new EntityFetchImpl( this, attributeDefinition, fetchStrategy, entityQuerySpace );
		addFetch( fetch );
		return fetch;
	}

	@Override
	public BidirectionalEntityReference buildBidirectionalEntityReference(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			EntityReference targetEntityReference) {
		final EntityType fetchedType = (EntityType) attributeDefinition.getType();
		final EntityPersister fetchedPersister = attributeDefinition.toEntityDefinition().getEntityPersister();

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate EntityPersister [%s] for bidirectional entity reference [%s]",
							fetchedType.getAssociatedEntityName(),
							attributeDefinition.getName()
					)
			);
		}

		final BidirectionalEntityReference bidirectionalEntityReference =
				new BidirectionalEntityReferenceImpl( this, attributeDefinition, targetEntityReference );
		addBidirectionalEntityReference( bidirectionalEntityReference );
		return bidirectionalEntityReference;
	}

	protected abstract CompositeFetch createCompositeFetch(
			CompositionDefinition compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace);

	protected ExpandingQuerySpaces getQuerySpaces() {
		return querySpace.getExpandingQuerySpaces();
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition) {
		final ExpandingSourceQuerySpace leftHandSide = expandingQuerySpace();
		final ExpandingCompositeQuerySpace compositeQuerySpace = leftHandSide.addCompositeQuerySpace(
				attributeDefinition,
				getQuerySpaces().generateImplicitUid(),
				shouldIncludeJoin( AbstractCompositeFetch.FETCH_STRATEGY )
		);
		final CompositeFetch fetch = createCompositeFetch( attributeDefinition, compositeQuerySpace );
		addFetch( fetch );
		return fetch;
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {

		// general question here wrt Joins and collection fetches...  do we create multiple Joins for many-to-many,
		// for example, or do we allow the Collection QuerySpace to handle that?

		final CollectionType fetchedType = (CollectionType) attributeDefinition.getType();
		final CollectionPersister fetchedPersister = attributeDefinition.toCollectionDefinition().getCollectionPersister();

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate CollectionPersister [%s] for fetch [%s]",
							fetchedType.getRole(),
							attributeDefinition.getName()
					)
			);
		}
		final ExpandingCollectionQuerySpace collectionQuerySpace = querySpace.addCollectionQuerySpace(
				attributeDefinition,
				fetchedPersister,
				getQuerySpaces().generateImplicitUid(),
				shouldIncludeJoin( fetchStrategy )
		);
		final CollectionFetch fetch = new CollectionFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy,
				collectionQuerySpace
		);
		addFetch( fetch );
		return fetch;
	}

	private boolean shouldIncludeJoin(FetchStrategy fetchStrategy) {
		return fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN;
	}
}
