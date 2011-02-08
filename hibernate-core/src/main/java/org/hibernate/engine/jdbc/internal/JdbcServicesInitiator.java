/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.internal;
import java.util.Map;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServicesRegistry;

/**
 * Standard initiator for the standard {@link JdbcServices} service
 *
 * @author Steve Ebersole
 */
public class JdbcServicesInitiator implements ServiceInitiator<JdbcServices> {
	public static final JdbcServicesInitiator INSTANCE = new JdbcServicesInitiator();

	/**
	 * {@inheritDoc}
	 */
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public JdbcServices initiateService(Map configValues, ServicesRegistry registry) {
		return new JdbcServicesImpl();
	}
}
