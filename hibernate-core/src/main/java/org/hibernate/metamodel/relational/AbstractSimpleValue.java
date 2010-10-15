/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
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
package org.hibernate.metamodel.relational;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic support for {@link SimpleValue} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimpleValue implements SimpleValue {
	private static final Logger log = LoggerFactory.getLogger( AbstractSimpleValue.class );

	private final ValueContainer container;
	private Datatype datatype;

	protected AbstractSimpleValue(ValueContainer container) {
		this.container = container;
	}

	/**
	 * {@inheritDoc}
	 */
	public ValueContainer getValueContainer() {
		return container;
	}

	/**
	 * {@inheritDoc}
	 */
	public Datatype getDatatype() {
		return datatype;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDatatype(Datatype datatype) {
		log.debug( "setting datatype for column {} : {}", toLoggableString(), datatype );
		if ( this.datatype != null && ! this.datatype.equals( datatype ) ) {
			log.debug( "overriding previous datatype : {}", this.datatype );
		}
		this.datatype = datatype;
	}
}
