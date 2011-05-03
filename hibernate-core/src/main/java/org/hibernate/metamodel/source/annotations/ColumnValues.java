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
package org.hibernate.metamodel.source.annotations;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.AssertionFailure;

/**
 * Container for the properties defined by {@code @Column}.
 *
 * @author Hardy Ferentschik
 */
public final class ColumnValues {
	private String name = "";
	private boolean unique = false;
	private boolean nullable = true;
	private boolean insertable = true;
	private boolean updatable = true;
	private String columnDefinition = "";
	private String table = "";
	private int length = 255;
	private int precision = 0;
	private int scale = 0;

	public ColumnValues(AnnotationInstance columnAnnotation, boolean isId) {
		if ( columnAnnotation != null && !JPADotNames.COLUMN.equals( columnAnnotation.name() ) ) {
			throw new AssertionFailure( "A @Column annotation needs to be passed to the constructor" );
		}
		applyColumnValues( columnAnnotation, isId );
	}

	private void applyColumnValues(AnnotationInstance columnAnnotation, boolean isId) {
		if ( columnAnnotation == null ) {
			return;
		}

		AnnotationValue nameValue = columnAnnotation.value( "name" );
		if ( nameValue != null ) {
			this.name = nameValue.asString();
		}

		// id attribute must be unique
		if ( isId ) {
			this.unique = true;
		}
		else {
			AnnotationValue uniqueValue = columnAnnotation.value( "unique" );
			if ( uniqueValue != null ) {
				this.unique = nameValue.asBoolean();
			}
		}

		// id attribute cannot be nullable
		if ( isId ) {
			this.nullable = false;
		}
		else {
			AnnotationValue nullableValue = columnAnnotation.value( "nullable" );
			if ( nullableValue != null ) {
				this.nullable = nullableValue.asBoolean();
			}
		}

		AnnotationValue insertableValue = columnAnnotation.value( "insertable" );
		if ( insertableValue != null ) {
			this.insertable = insertableValue.asBoolean();
		}

		AnnotationValue updatableValue = columnAnnotation.value( "updatable" );
		if ( updatableValue != null ) {
			this.updatable = updatableValue.asBoolean();
		}

		AnnotationValue columnDefinition = columnAnnotation.value( "columnDefinition" );
		if ( columnDefinition != null ) {
			this.columnDefinition = columnDefinition.asString();
		}

		AnnotationValue tableValue = columnAnnotation.value( "table" );
		if ( tableValue != null ) {
			this.table = tableValue.asString();
		}

		AnnotationValue lengthValue = columnAnnotation.value( "length" );
		if ( lengthValue != null ) {
			this.length = lengthValue.asInt();
		}

		AnnotationValue precisionValue = columnAnnotation.value( "precision" );
		if ( precisionValue != null ) {
			this.precision = precisionValue.asInt();
		}

		AnnotationValue scaleValue = columnAnnotation.value( "scale" );
		if ( scaleValue != null ) {
			this.scale = scaleValue.asInt();
		}
	}

	public final String getName() {
		return name;
	}

	public final boolean isUnique() {
		return unique;
	}

	public final boolean isNullable() {
		return nullable;
	}

	public final boolean isInsertable() {
		return insertable;
	}

	public final boolean isUpdatable() {
		return updatable;
	}

	public final String getColumnDefinition() {
		return columnDefinition;
	}

	public final String getTable() {
		return table;
	}

	public final int getLength() {
		return length;
	}

	public final int getPrecision() {
		return precision;
	}

	public final int getScale() {
		return scale;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ColumnValues" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", unique=" ).append( unique );
		sb.append( ", nullable=" ).append( nullable );
		sb.append( ", insertable=" ).append( insertable );
		sb.append( ", updatable=" ).append( updatable );
		sb.append( ", columnDefinition='" ).append( columnDefinition ).append( '\'' );
		sb.append( ", table='" ).append( table ).append( '\'' );
		sb.append( ", length=" ).append( length );
		sb.append( ", precision=" ).append( precision );
		sb.append( ", scale=" ).append( scale );
		sb.append( '}' );
		return sb.toString();
	}
}


