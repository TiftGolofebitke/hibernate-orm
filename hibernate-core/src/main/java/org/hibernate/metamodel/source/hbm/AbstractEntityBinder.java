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
package org.hibernate.metamodel.source.hbm;

import java.sql.ResultSet;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.Versioning;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.PluralAttributeNature;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.hbm.state.domain.HbmPluralAttributeDomainState;
import org.hibernate.metamodel.source.hbm.state.domain.HbmSimpleAttributeDomainState;
import org.hibernate.metamodel.source.hbm.state.relational.HbmSimpleValueRelationalStateContainer;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLAny;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLBag;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLComponent;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLDynamicComponent;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLFilter;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLVersion;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLIdbag;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoin;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLList;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOne;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLMap;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLOneToOne;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLProperties;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLProperty;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLQuery;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLResultset;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSet;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlQuery;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTuplizer;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclass;

/**
* TODO : javadoc
*
* @author Steve Ebersole
*/
abstract class AbstractEntityBinder {
	private final HibernateMappingBinder hibernateMappingBinder;
	private final Schema.Name schemaName;

	AbstractEntityBinder(HibernateMappingBinder hibernateMappingBinder,
						 XMLClass entityClazz) {
		this.hibernateMappingBinder = hibernateMappingBinder;
		this.schemaName = new Schema.Name(
				( entityClazz.getSchema() == null ?
						hibernateMappingBinder.getDefaultSchemaName() :
						entityClazz.getSchema() ),
				( entityClazz.getCatalog() == null ?
						hibernateMappingBinder.getDefaultCatalogName() :
						entityClazz.getCatalog() )
		);
	}

	protected HibernateMappingBinder getHibernateMappingBinder() {
		return hibernateMappingBinder;
	}

	protected HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateMappingBinder.getHibernateXmlBinder();
	}

	protected MetadataImpl getMetadata() {
		return hibernateMappingBinder.getHibernateXmlBinder().getMetadata();
	}

	protected Schema.Name getSchemaName() {
		return schemaName;
	}
	protected NamingStrategy getNamingStrategy() {
		return getMetadata().getNamingStrategy();
	}

	protected void basicEntityBinding(XMLClass entityClazz,
									  EntityBinding entityBinding,
									  Hierarchical superType) {
		entityBinding.fromHbmXml(
				hibernateMappingBinder,
				entityClazz,
				new Entity( hibernateMappingBinder.extractEntityName( entityClazz ), superType )
		);
		// TODO: move this stuff out
		// transfer an explicitly defined lazy attribute
		bindPojoRepresentation( entityClazz, entityBinding );
		bindDom4jRepresentation( entityClazz, entityBinding );
		bindMapRepresentation( entityClazz, entityBinding );

		final String entityName = entityBinding.getEntity().getName();

		if ( entityClazz.getFetchProfile() != null ) {
			hibernateMappingBinder.parseFetchProfiles( entityClazz.getFetchProfile(), entityName );
		}

		getMetadata().addImport( entityName, entityName );
		if ( hibernateMappingBinder.isAutoImport() ) {
			if ( entityName.indexOf( '.' ) > 0 ) {
				getMetadata().addImport( StringHelper.unqualify( entityName ), entityName );
			}
		}
	}

	protected String getDefaultAccess() {
		return hibernateMappingBinder.getDefaultAccess();
	}

	private void bindPojoRepresentation(XMLClass entityClazz,
										EntityBinding entityBinding) {
		String className = hibernateMappingBinder.getClassName( entityClazz.getName() );
		String proxyName = hibernateMappingBinder.getClassName( entityClazz.getProxy() );

		entityBinding.getEntity().getPojoEntitySpecifics().setClassName( className );

		if ( proxyName != null ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( proxyName );
			entityBinding.setLazy( true );
		}
		else if ( entityBinding.isLazy() ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( className );
		}

		XMLTuplizer tuplizer = locateTuplizerDefinition( entityClazz, EntityMode.POJO );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setTuplizerClassName( tuplizer.getClazz() );
		}
	}

	private void bindDom4jRepresentation(XMLClass entityClazz,
										 EntityBinding entityBinding) {
		String nodeName = entityClazz.getNode();
		if ( nodeName == null ) {
			nodeName = StringHelper.unqualify( entityBinding.getEntity().getName() );
		}
		entityBinding.getEntity().getDom4jEntitySpecifics().setNodeName(nodeName);

		XMLTuplizer tuplizer = locateTuplizerDefinition( entityClazz, EntityMode.DOM4J );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getDom4jEntitySpecifics().setTuplizerClassName( tuplizer.getClazz() );
		}
	}

	private void bindMapRepresentation(XMLClass entityClazz,
									   EntityBinding entityBinding) {
		XMLTuplizer tuplizer = locateTuplizerDefinition( entityClazz, EntityMode.MAP );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getMapEntitySpecifics().setTuplizerClassName( tuplizer.getClazz() );
		}
	}

	/**
	 * Locate any explicit tuplizer definition in the metadata, for the given entity-mode.
	 *
	 * @param container The containing element (representing the entity/component)
	 * @param entityMode The entity-mode for which to locate the tuplizer element
	 *
	 * @return The tuplizer element, or null.
	 */
	private static XMLTuplizer locateTuplizerDefinition(XMLClass container,
													EntityMode entityMode) {
		for ( XMLTuplizer tuplizer : container.getTuplizer() ) {
			if ( entityMode.toString().equals( tuplizer.getEntityMode() ) ) {
				return tuplizer;
			}
		}
		return null;
	}

	int getOptimisticLockMode(Attribute olAtt) throws MappingException {
		if ( olAtt == null ) {
			return Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		String olMode = olAtt.getValue();
		if ( olMode == null || "version".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		else if ( "dirty".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_DIRTY;
		}
		else if ( "all".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_ALL;
		}
		else if ( "none".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_NONE;
		}
		else {
			throw new MappingException( "Unsupported optimistic-lock style: " + olMode );
		}
	}

	protected String getClassTableName(
			XMLClass entityClazz,
			EntityBinding entityBinding,
			Table denormalizedSuperTable) {
		final String entityName = entityBinding.getEntity().getName();
		String logicalTableName;
		String physicalTableName;
		if ( entityClazz.getTable() == null ) {
			logicalTableName = StringHelper.unqualify( entityName );
			physicalTableName = getHibernateXmlBinder().getMetadata().getNamingStrategy().classToTableName( entityName );
		}
		else {
			logicalTableName = entityClazz.getTable();
			physicalTableName = getHibernateXmlBinder().getMetadata().getNamingStrategy().tableName( logicalTableName );
		}
// todo : find out the purpose of these logical bindings
//			mappings.addTableBinding( schema, catalog, logicalTableName, physicalTableName, denormalizedSuperTable );
		return physicalTableName;
	}

	protected void buildAttributeBindings(XMLClass entityClazz,
										  EntityBinding entityBinding) {
		// null = UniqueKey (we are not binding a natural-id mapping)
		// true = mutable, by default properties are mutable
		// true = nullable, by default properties are nullable.
		buildAttributeBindings( entityClazz, entityBinding, null, true, true );
	}

	/**
	 * This form is essentially used to create natural-id mappings.  But the processing is the same, aside from these
	 * extra parameterized values, so we encapsulate it here.
	 *
	 * @param entityClazz
	 * @param entityBinding
	 * @param uniqueKey
	 * @param mutable
	 * @param nullable
	 */
	protected void buildAttributeBindings(
			XMLClass entityClazz,
			EntityBinding entityBinding,
			UniqueKey uniqueKey,
			boolean mutable,
			boolean nullable) {
		final boolean naturalId = uniqueKey != null;

		final String entiytName = entityBinding.getEntity().getName();
		final TableSpecification tabe = entityBinding.getBaseTable();

		AttributeBinding attributeBinding = null;
		for ( Object attribute : entityClazz.getPropertyOrManyToOneOrOneToOne() ) {
			if ( XMLBag.class.isInstance( attribute ) ) {
				XMLBag collection = XMLBag.class.cast( attribute );
				BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				bindBag( collection, collectionBinding, entityBinding );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( collectionBinding );
				attributeBinding = collectionBinding;
			}
			else if ( XMLIdbag.class.isInstance( attribute ) ) {
				XMLIdbag collection = XMLIdbag.class.cast( attribute );
				//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindIdbag( collection, bagBinding, entityBinding, PluralAttributeNature.BAG, collection.getName() );
				// todo: handle identifier
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLSet.class.isInstance( attribute ) ) {
				XMLSet collection = XMLSet.class.cast( attribute );
				//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindSet( collection, collectionBinding, entityBinding, PluralAttributeNature.SET, collection.getName() );
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLList.class.isInstance( attribute ) ) {
				XMLList collection = XMLList.class.cast( attribute );
				//ListBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindList( collection, bagBinding, entityBinding, PluralAttributeNature.LIST, collection.getName() );
				// todo : handle list index
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLMap.class.isInstance( attribute ) ) {
				XMLMap collection = XMLMap.class.cast( attribute );
				//BagBinding bagBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindMap( collection, bagBinding, entityBinding, PluralAttributeNature.MAP, collection.getName() );
				// todo : handle map key
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLManyToOne.class.isInstance( attribute ) ) {
// todo : implement
//				value = new ManyToOne( mappings, table );
//				bindManyToOne( subElement, (ManyToOne) value, propertyName, nullable, mappings );
			}
			else if ( XMLAny.class.isInstance( attribute ) ) {
// todo : implement
//				value = new Any( mappings, table );
//				bindAny( subElement, (Any) value, nullable, mappings );
			}
			else if ( XMLOneToOne.class.isInstance( attribute ) ) {
// todo : implement
//				value = new OneToOne( mappings, table, persistentClass );
//				bindOneToOne( subElement, (OneToOne) value, propertyName, true, mappings );
			}
			else if ( XMLProperty.class.isInstance( attribute ) ) {
				XMLProperty property = XMLProperty.class.cast( attribute );
				SimpleAttributeBinding binding = entityBinding.makeSimpleAttributeBinding( property.getName() );
				bindSimpleAttribute( property, binding, entityBinding );
				attributeBinding = binding;
			}
			else if ( XMLComponent.class.isInstance( attribute )
					|| XMLDynamicComponent.class.isInstance( attribute )
					|| XMLProperties.class.isInstance( attribute ) ) {
// todo : implement
//				String subpath = StringHelper.qualify( entityName, propertyName );
//				value = new Component( mappings, persistentClass );
//
//				bindComponent(
//						subElement,
//						(Component) value,
//						persistentClass.getClassName(),
//						propertyName,
//						subpath,
//						true,
//						"properties".equals( subElementName ),
//						mappings,
//						inheritedMetas,
//						false
//					);
			}
		}

		/*
Array
PrimitiveArray
*/
		for ( XMLJoin join : entityClazz.getJoin() ) {
// todo : implement
//			Join join = new Join();
//			join.setPersistentClass( persistentClass );
//			bindJoin( subElement, join, mappings, inheritedMetas );
//			persistentClass.addJoin( join );
		}
		for ( XMLSubclass subclass : entityClazz.getSubclass() ) {
// todo : implement
//			handleSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLJoinedSubclass subclass : entityClazz.getJoinedSubclass() ) {
// todo : implement
//			handleJoinedSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLUnionSubclass subclass : entityClazz.getUnionSubclass() ) {
// todo : implement
//			handleUnionSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLFilter filter : entityClazz.getFilter() ) {
// todo : implement
//				parseFilter( subElement, entityBinding );
		}
		if ( entityClazz.getNaturalId() != null ) {
// todo : implement
//				UniqueKey uk = new UniqueKey();
//				uk.setName("_UniqueKey");
//				uk.setTable(table);
//				//by default, natural-ids are "immutable" (constant)
//				boolean mutableId = "true".equals( subElement.attributeValue("mutable") );
//				createClassProperties(
//						subElement,
//						persistentClass,
//						mappings,
//						inheritedMetas,
//						uk,
//						mutableId,
//						false,
//						true
//					);
//				table.addUniqueKey(uk);
		}
		if ( entityClazz.getQueryOrSqlQuery() != null ) {
			for ( Object queryOrSqlQuery : entityClazz.getQueryOrSqlQuery() ) {
				if ( XMLQuery.class.isInstance( queryOrSqlQuery ) ) {
// todo : implement
//				bindNamedQuery(subElement, persistentClass.getEntityName(), mappings);
				}
				else if ( XMLSqlQuery.class.isInstance( queryOrSqlQuery ) ) {
// todo : implement
//			bindNamedSQLQuery(subElement, persistentClass.getEntityName(), mappings);
				}
			}
		}
		if ( entityClazz.getResultset() != null ) {
			for ( XMLResultset resultSet : entityClazz.getResultset() ) {
// todo : implement
//				bindResultSetMappingDefinition( subElement, persistentClass.getEntityName(), mappings );
			}
		}
//			if ( value != null ) {
//				Property property = createProperty( value, propertyName, persistentClass
//					.getClassName(), subElement, mappings, inheritedMetas );
//				if ( !mutable ) property.setUpdateable(false);
//				if ( naturalId ) property.setNaturalIdentifier(true);
//				persistentClass.addProperty( property );
//				if ( uniqueKey!=null ) uniqueKey.addColumns( property.getColumnIterator() );
//			}

	}

	protected void bindSimpleAttribute(XMLId id,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding,
									   String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							entityBinding.getEntity().getOrCreateSingularAttribute( attributeName ),
							entityBinding.getMetaAttributes(),
							id
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							true,
							id
					)
			);
		}
	}

	protected void bindSimpleAttribute(XMLDiscriminator discriminator,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding,
									   String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							entityBinding.getEntity().getOrCreateSingularAttribute( attributeName ),
							entityBinding.getMetaAttributes(),
							discriminator
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							true,
							discriminator
					)
			);
		}
	}

	protected void bindSimpleAttribute(XMLVersion version,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding,
									   String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							entityBinding.getEntity().getOrCreateSingularAttribute( attributeName ),
							entityBinding.getMetaAttributes(),
							version
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							true,
							version
					)
			);
		}
	}

	protected void bindSimpleAttribute(XMLTimestamp timestamp,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding,
									   String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							entityBinding.getEntity().getOrCreateSingularAttribute( attributeName ),
							entityBinding.getMetaAttributes(),
							timestamp
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							true,
							timestamp
					)
			);
		}
	}

	protected void bindSimpleAttribute(XMLProperty property,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							entityBinding.getEntity().getOrCreateSingularAttribute( property.getName() ),
							entityBinding.getMetaAttributes(),
							property
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							true,
							property
					)
			);
		}
	}

	protected void bindBag(
			XMLBag collection,
			PluralAttributeBinding collectionBinding,
			EntityBinding entityBinding) {
		if ( collectionBinding.getAttribute() == null ) {
			// domain model has not been bound yet
			collectionBinding.initialize(
					new HbmPluralAttributeDomainState(
							hibernateMappingBinder,
							collection,
							entityBinding.getMetaAttributes(),
							entityBinding.getEntity().getOrCreatePluralAttribute(
									collection.getName(),
									PluralAttributeNature.BAG
							)
					)
			);
		}

		if ( collectionBinding.getValue() == null ) {
			// todo : relational model binding
		}
	}

//	private static Property createProperty(
//			final Value value,
//	        final String propertyName,
//			final String className,
//	        final Element subnode,
//	        final Mappings mappings,
//			java.util.Map inheritedMetas) throws MappingException {
//
//		if ( StringHelper.isEmpty( propertyName ) ) {
//			throw new MappingException( subnode.getName() + " mapping must defined a name attribute [" + className + "]" );
//		}
//
//		value.setTypeUsingReflection( className, propertyName );
//
//		// this is done here 'cos we might only know the type here (ugly!)
//		// TODO: improve this a lot:
//		if ( value instanceof ToOne ) {
//			ToOne toOne = (ToOne) value;
//			String propertyRef = toOne.getReferencedPropertyName();
//			if ( propertyRef != null ) {
//				mappings.addUniquePropertyReference( toOne.getReferencedEntityName(), propertyRef );
//			}
//		}
//		else if ( value instanceof Collection ) {
//			Collection coll = (Collection) value;
//			String propertyRef = coll.getReferencedPropertyName();
//			// not necessarily a *unique* property reference
//			if ( propertyRef != null ) {
//				mappings.addPropertyReference( coll.getOwnerEntityName(), propertyRef );
//			}
//		}
//
//		value.createForeignKey();
//		Property prop = new Property();
//		prop.setValue( value );
//		bindProperty( subnode, prop, mappings, inheritedMetas );
//		return prop;
//	}


//	protected HbmRelationalState processValues(Element identifierElement, TableSpecification baseTable, String propertyPath, boolean isSimplePrimaryKey) {
		// first boolean (false here) indicates that by default columns are nullable
		// second boolean (true here) indicates that by default column names should be guessed
// todo : logical 1-1 handling
//			final Attribute uniqueAttribute = node.attribute( "unique" );
//			if ( uniqueAttribute != null
//					&& "true".equals( uniqueAttribute.getValue() )
//					&& ManyToOne.class.isInstance( simpleValue ) ) {
//				( (ManyToOne) simpleValue ).markAsLogicalOneToOne();
//			}
		//return processValues( identifierElement, baseTable, false, true, propertyPath, isSimplePrimaryKey );



}