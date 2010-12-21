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
package org.hibernate.dialect.resolver;

import static org.jboss.logging.Logger.Level.WARN;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.JDBCConnectionException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A templated resolver impl which delegates to the {@link #resolveDialectInternal} method
 * and handles any thrown {@link SQLException}s.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDialectResolver implements DialectResolver {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                CollectionSecondPass.class.getPackage().getName());

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here we template the resolution, delegating to {@link #resolveDialectInternal} and handling
	 * {@link java.sql.SQLException}s properly.
	 */
	public final Dialect resolveDialect(DatabaseMetaData metaData) {
		try {
			return resolveDialectInternal( metaData );
		}
		catch ( SQLException sqlException ) {
			JDBCException jdbcException = BasicSQLExceptionConverter.INSTANCE.convert( sqlException );
            if (jdbcException instanceof JDBCConnectionException) throw jdbcException;
            LOG.unableToQueryDatabaseMetadata(BasicSQLExceptionConverter.MSG, sqlException.getMessage());
            return null;
		}
		catch ( Throwable t ) {
            LOG.unableToExecuteResolver(this, t.getMessage());
			return null;
		}
	}

	/**
	 * Perform the actual resolution without caring about handling {@link SQLException}s.
	 *
	 * @param metaData The database metadata
	 * @return The resolved dialect, or null if we could not resolve.
	 * @throws SQLException Indicates problems accessing the metadata.
	 */
	protected abstract Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException;

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = WARN )
        @Message( value = "Error executing resolver [%s] : %s" )
        void unableToExecuteResolver( AbstractDialectResolver abstractDialectResolver,
                                      String message );

        @LogMessage( level = WARN )
        @Message( value = "%s : %s" )
        void unableToQueryDatabaseMetadata( String message,
                                            String errorMessage );
    }
}
