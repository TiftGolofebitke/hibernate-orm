/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.state.domain;

import java.util.Comparator;

import org.hibernate.FetchMode;
import org.hibernate.metamodel.binding.AbstractAttributeBinding;
import org.hibernate.metamodel.binding.CollectionElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.PluralAttributeBinding;

/**
 * Created by IntelliJ IDEA.
 * User: gbadner
 * Date: 5/9/11
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PluralAttributeDomainState extends AttributeDomainState {
	FetchMode getFetchMode();
	boolean isExtraLazy();
	CollectionElementDomainState getCollectionElementDomainState();
	boolean isInverse();
	boolean isMutable();
	boolean isSubselectLoadable();
	String getCacheConcurrencyStrategy();
	String getCacheRegionName();
	String getOrderBy();
	String getWhere();
	String getReferencedPropertyName();
	boolean isSorted();
	Comparator getComparator();
	String getComparatorClassName();
	boolean isOrphanDelete();
	int getBatchSize();
	boolean isEmbedded();
	boolean isOptimisticLocked();
	Class getCollectionPersisterClass();
	java.util.Map getFilters();
	java.util.Set getSynchronizedTables();
	CustomSQL getCustomSQLInsert();
	CustomSQL getCustomSQLUpdate();
	CustomSQL getCustomSQLDelete();
	CustomSQL getCustomSQLDeleteAll();
	String getLoaderName();
}
