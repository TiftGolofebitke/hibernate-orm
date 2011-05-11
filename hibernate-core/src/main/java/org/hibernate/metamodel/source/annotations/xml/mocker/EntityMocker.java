package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLDiscriminatorColumn;
import org.hibernate.metamodel.source.annotation.xml.XMLEntity;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.xml.XMLIdClass;
import org.hibernate.metamodel.source.annotation.xml.XMLInheritance;
import org.hibernate.metamodel.source.annotation.xml.XMLPostLoad;
import org.hibernate.metamodel.source.annotation.xml.XMLPostPersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPostRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPostUpdate;
import org.hibernate.metamodel.source.annotation.xml.XMLPrePersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPreRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPreUpdate;
import org.hibernate.metamodel.source.annotation.xml.XMLSecondaryTable;
import org.hibernate.metamodel.source.annotation.xml.XMLTable;


/**
 * Mock <entity> to {@link javax.persistence.Entity @Entity}
 *
 * @author Strong Liu
 */
class EntityMocker extends AbstractEntityObjectMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityMocker.class.getName()
	);
	private XMLEntity entity;

	EntityMocker(IndexBuilder indexBuilder, XMLEntity entity, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.entity = entity;
	}

	@Override
	protected String getClassName() {
		return entity.getClazz();
	}

	@Override
	protected void processExtra() {
		//@Entity
		create( ENTITY, MockHelper.stringValueArray( "name", entity.getName() ) );


		if ( entity.isCacheable() != null ) {
			//@Cacheable
			create(
					CACHEABLE,
					MockHelper.booleanValueArray( "value", entity.isCacheable() )

			);
		}
		if ( StringHelper.isNotEmpty( entity.getDiscriminatorValue() ) ) {
			//@DiscriminatorValue
			create(
					DISCRIMINATOR_VALUE,
					MockHelper.stringValueArray( "value", entity.getDiscriminatorValue() )

			);
		}
		//@Table
		parserTable( entity.getTable() );
		parserInheritance( entity.getInheritance() );
		parserDiscriminatorColumn( entity.getDiscriminatorColumn() );
		parserAttributeOverrides( entity.getAttributeOverride(), getTarget() );
		parserAssociationOverrides( entity.getAssociationOverride(), getTarget() );
		parserPrimaryKeyJoinColumnList( entity.getPrimaryKeyJoinColumn(), getTarget() );
		parserSecondaryTableList( entity.getSecondaryTable(), getTarget() );

	}

	//@Table  (entity only)
	private AnnotationInstance parserTable(XMLTable table) {
		if ( table == null ) {
			return null;
		}
		MockHelper.updateSchema( new SchemaAware.TableSchemaAware( table ), getDefaults() );
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", table.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", table.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", table.getSchema(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", table.getUniqueConstraint(), annotationValueList );
		return create( TABLE, annotationValueList );
	}

	@Override
	protected void applyDefaults() {
		if ( getDefaults() == null ) {
			return;
		}
		if ( MockHelper.hasSchemaOrCatalogDefined( getDefaults() ) ) {
			XMLTable table = entity.getTable();
			if ( table == null ) {
				table = new XMLTable();
				entity.setTable( table );
			}
			MockHelper.updateSchema( new SchemaAware.TableSchemaAware( table ), getDefaults() );
		}
		String className = MockHelper.buildSafeClassName( entity.getClazz(), getDefaults().getPackageName() );
		entity.setClazz( className );
		if ( entity.isMetadataComplete() == null ) {
			entity.setMetadataComplete( getDefaults().getMetadataComplete() );
		}
		if ( entity.getAccess() != null ) {
			entity.setAccess( getDefaults().getAccess() );
		}
		LOG.debugf( "Adding XML overriding information for %s", className );

	}

	@Override
	protected XMLPrePersist getPrePersist() {
		return entity.getPrePersist();
	}

	@Override
	protected XMLPreRemove getPreRemove() {
		return entity.getPreRemove();
	}

	@Override
	protected XMLPreUpdate getPreUpdate() {
		return entity.getPreUpdate();
	}

	@Override
	protected XMLPostPersist getPostPersist() {
		return entity.getPostPersist();
	}

	@Override
	protected XMLPostUpdate getPostUpdate() {
		return entity.getPostUpdate();
	}

	@Override
	protected XMLPostRemove getPostRemove() {
		return entity.getPostRemove();
	}

	@Override
	protected XMLPostLoad getPostLoad() {
		return entity.getPostLoad();
	}

	@Override
	protected XMLAttributes getAttributes() {
		return entity.getAttributes();
	}

	@Override
	protected boolean isMetadataComplete() {
		return entity.isMetadataComplete() != null && entity.isMetadataComplete();
	}

	@Override
	protected boolean isExcludeDefaultListeners() {
		return entity.getExcludeDefaultListeners() != null;
	}

	@Override
	protected boolean isExcludeSuperclassListeners() {
		return entity.getExcludeSuperclassListeners() != null;
	}

	@Override
	protected XMLIdClass getIdClass() {
		return entity.getIdClass();
	}

	@Override
	protected XMLEntityListeners getEntityListeners() {
		return entity.getEntityListeners();
	}

	@Override
	protected XMLAccessType getAccessType() {
		return entity.getAccess();
	}

	//@Inheritance
	protected AnnotationInstance parserInheritance(XMLInheritance inheritance) {
		if ( inheritance == null ) {
			return null;
		}
		return
				create(
						INHERITANCE,
						MockHelper.enumValueArray( "strategy", INHERITANCE_TYPE, inheritance.getStrategy() )

				);
	}

	//@DiscriminatorColumn
	protected AnnotationInstance parserDiscriminatorColumn(XMLDiscriminatorColumn discriminatorColumn) {
		if ( discriminatorColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", discriminatorColumn.getName(), annotationValueList );
		MockHelper.stringValue(
				"columnDefinition", discriminatorColumn.getColumnDefinition(), annotationValueList
		);
		MockHelper.integerValue( "length", discriminatorColumn.getLength(), annotationValueList );
		MockHelper.enumValue(
				"discriminatorType", DISCRIMINATOR_TYPE, discriminatorColumn.getDiscriminatorType(), annotationValueList
		);
		return
				create(
						DISCRIMINATOR_COLUMN, annotationValueList

				);

	}

	//@SecondaryTable
	protected AnnotationInstance parserSecondaryTable(XMLSecondaryTable secondaryTable, AnnotationTarget target) {
		if ( secondaryTable == null ) {
			return null;
		}
		MockHelper.updateSchema( new SchemaAware.SecondaryTableSchemaAware( secondaryTable ), getDefaults() );
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", secondaryTable.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", secondaryTable.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", secondaryTable.getSchema(), annotationValueList );
		nestedPrimaryKeyJoinColumnList(
				"pkJoinColumns", secondaryTable.getPrimaryKeyJoinColumn(), annotationValueList
		);
		nestedUniqueConstraintList(
				"uniqueConstraints", secondaryTable.getUniqueConstraint(), annotationValueList
		);
		return
				create(
						SECONDARY_TABLE, target, annotationValueList
				);
	}


	protected AnnotationInstance parserSecondaryTableList(List<XMLSecondaryTable> primaryKeyJoinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( primaryKeyJoinColumnList ) ) {
			if ( primaryKeyJoinColumnList.size() == 1 ) {
				return parserSecondaryTable( primaryKeyJoinColumnList.get( 0 ), target );
			}
			else {
				return create(
						SECONDARY_TABLES,
						target,
						nestedSecondaryTableList( "value", primaryKeyJoinColumnList, null )
				);
			}
		}
		return null;

	}

	protected AnnotationValue[] nestedSecondaryTableList(String name, List<XMLSecondaryTable> secondaryTableList, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( secondaryTableList ) ) {
			AnnotationValue[] values = new AnnotationValue[secondaryTableList.size()];
			for ( int i = 0; i < secondaryTableList.size(); i++ ) {
				AnnotationInstance annotationInstance = parserSecondaryTable( secondaryTableList.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
			return values;
		}
		return MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY;

	}


}
