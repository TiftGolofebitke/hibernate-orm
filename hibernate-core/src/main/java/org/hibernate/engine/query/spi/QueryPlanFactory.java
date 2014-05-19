/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.query.spi;

import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Contract for factories of query plans.
 * <p>
 * The default implementation creates query plans for SQL queries, but
 * integrators can plug in another factory by registering a custom
 * {@link SessionFactoryServiceInitiator} for this service contract.
 *
 * @author Gunnar Morling
 *
 */
public interface QueryPlanFactory extends Service {

	/**
	 * Creates a new query plan for the specified native query.
	 *
	 * @param nativeQuerySpecification
	 *            Describes the query to create a plan for
	 * @param sessionFactory
	 *            The current session factory
	 * @return A query plan for the specified native query.
	 */
	NativeSQLQueryPlan createNativeQueryPlan(
			NativeSQLQuerySpecification nativeQuerySpecification,
			SessionFactoryImplementor sessionFactory);
}
