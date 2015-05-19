/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * <b>assigned</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 */

public class Assigned implements IdentifierGenerator, Configurable {
	
	private String entityName;

	public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {
		//TODO: cache the persister, this shows up in yourkit
		final Serializable id = session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
		if ( id == null ) {
			throw new IdentifierGenerationException(
					"ids for this class must be manually assigned before calling save(): " + entityName
			);
		}
		
		return id;
	}

	@Override
	public void configure(Type type, Properties params, JdbcEnvironment jdbcEnv) throws MappingException {
		entityName = params.getProperty( ENTITY_NAME );
		if ( entityName == null ) {
			throw new MappingException("no entity name");
		}
	}
}






