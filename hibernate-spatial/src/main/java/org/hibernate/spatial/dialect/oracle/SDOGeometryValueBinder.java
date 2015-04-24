/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

package org.hibernate.spatial.dialect.oracle;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Encoders;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
class SDOGeometryValueBinder<J> implements ValueBinder<J> {

	private static final String SQL_TYPE_NAME = "MDSYS.SDO_GEOMETRY";

	private final OracleJDBCTypeFactory typeFactory;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public SDOGeometryValueBinder(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			OracleJDBCTypeFactory typeFactory) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.typeFactory = typeFactory;
	}

	@Override
	public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT, SQL_TYPE_NAME );
		}
		else {
			final Geometry geometry = javaTypeDescriptor.unwrap( value, Geometry.class, options );
			final Object dbGeom = toNative( geometry, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

	public Object store(SDOGeometry geom, Connection conn) throws SQLException {
		return typeFactory.createStruct( geom, conn );
	}

	private Object toNative(Geometry geom, Connection connection) {
		try {
			final SDOGeometry sdoGeom = Encoders.encode( geom );
			return store( sdoGeom, connection );
		}
		catch (SQLException e) {
			throw new HibernateException( "Problem during conversion from JTS to SDOGeometry", e );
		}
		catch (IllegalArgumentException e) {
			//we get here if the type of geometry is unsupported by geolatte encoders
			throw new HibernateException( e.getMessage() );
		}
		catch(Exception e) {
			throw new HibernateException( e );
		}
	}

}
