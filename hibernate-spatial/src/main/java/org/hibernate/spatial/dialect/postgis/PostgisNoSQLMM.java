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

package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 *  A Dialect for Postgresql with support for the Postgis spatial types, functions and operators (release 1.x - 1.3)
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Dec 18, 2010
 */
public class PostgisNoSQLMM extends PostgisDialect {

	@Override
	protected void registerTypesAndFunctions() {

		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname, second the name as
		// it occurs in the spatial dialect
		registerFunction(
				"dimension", new StandardSQLFunction(
				"dimension",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"geometrytype", new StandardSQLFunction(
				"geometrytype", StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"srid", new StandardSQLFunction(
				"srid",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"envelope", new StandardSQLFunction(
				"envelope"
		)
		);
		registerFunction(
				"astext", new StandardSQLFunction(
				"astext",
				StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"asbinary", new StandardSQLFunction(
				"asbinary",
				StandardBasicTypes.BINARY
		)
		);
		registerFunction(
				"isempty", new StandardSQLFunction(
				"isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"issimple", new StandardSQLFunction(
				"issimple",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"boundary", new StandardSQLFunction(
				"boundary"
		)
		);

		// Register functions for spatial relation constructs
		registerFunction(
				"overlaps", new StandardSQLFunction(
				"overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"intersects", new StandardSQLFunction(
				"intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"equals", new StandardSQLFunction(
				"equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"contains", new StandardSQLFunction(
				"contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"crosses", new StandardSQLFunction(
				"crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"disjoint", new StandardSQLFunction(
				"disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"touches", new StandardSQLFunction(
				"touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"within", new StandardSQLFunction(
				"within",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"relate", new StandardSQLFunction(
				"relate",
				StandardBasicTypes.BOOLEAN
		)
		);

		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSQLFunction(
				"distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction(
				"buffer", new StandardSQLFunction(
				"buffer"
		)
		);
		registerFunction(
				"convexhull", new StandardSQLFunction(
				"convexhull"
		)
		);
		registerFunction(
				"difference", new StandardSQLFunction(
				"difference"
		)
		);
		registerFunction(
				"intersection", new StandardSQLFunction(
				"intersection"
		)
		);
		registerFunction(
				"symdifference",
				new StandardSQLFunction( "symdifference" )
		);
		registerFunction(
				"geomunion", new StandardSQLFunction(
				"geomunion"
		)
		);

		//register Spatial Aggregate function
		registerFunction(
				"extent", new StandardSQLFunction(
				"extent"
		)
		);

		//other common spatial functions
		registerFunction(
				"transform", new StandardSQLFunction(
				"transform"
		)
		);
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return "( dwithin(" + columnName + ",?,?) )";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return "( srid(" + columnName + ") = ?)";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = "( isempty(" + columnName + ")) ";
		return isEmpty ? emptyExpr : "not " + emptyExpr;
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return "(" + columnName + " && ?  AND within(" + columnName + ", ?))";
			case SpatialRelation.CONTAINS:
				return "(" + columnName + " && ? AND contains(" + columnName + ", ?))";
			case SpatialRelation.CROSSES:
				return "(" + columnName + " && ? AND crosses(" + columnName + ", ?))";
			case SpatialRelation.OVERLAPS:
				return "(" + columnName + " && ? AND overlaps(" + columnName + ", ?))";
			case SpatialRelation.DISJOINT:
				return "(" + columnName + " && ? AND disjoint(" + columnName + ", ?))";
			case SpatialRelation.INTERSECTS:
				return "(" + columnName + " && ? AND intersects(" + columnName + ", ?))";
			case SpatialRelation.TOUCHES:
				return "(" + columnName + " && ? AND touches(" + columnName + ", ?))";
			case SpatialRelation.EQUALS:
				return "(" + columnName + " && ? AND equals(" + columnName + ", ?))";
			default:
				throw new IllegalArgumentException( "Spatial relation is not known by this dialect" );
		}

	}

	@Override
	public boolean supports(SpatialFunction function) {
		return super.supports( function );
	}
}
