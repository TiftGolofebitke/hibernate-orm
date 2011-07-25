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
package org.hibernate.metamodel.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.domain.AttributeContainer;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Provides the link between the domain and the relational model for an entity.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntityBinding extends AbstractAttributeBindingContainer {
	private final EntityBinding superEntityBinding;
	private final HierarchyDetails hierarchyDetails;

	private Entity entity;
	private TableSpecification baseTable;
	private Map<String, TableSpecification> secondaryTables = new HashMap<String, TableSpecification>();

	private Value<Class<?>> proxyInterfaceType;

	private String jpaEntityName;

	private Class<? extends EntityPersister> customEntityPersisterClass;
	private Class<? extends EntityTuplizer> customEntityTuplizerClass;

	private String discriminatorMatchValue;

	private Set<FilterDefinition> filterDefinitions = new HashSet<FilterDefinition>();
	private Set<SingularAssociationAttributeBinding> entityReferencingAttributeBindings = new HashSet<SingularAssociationAttributeBinding>();

	private MetaAttributeContext metaAttributeContext;

	private boolean lazy;
	private boolean mutable;
	private String whereFilter;
	private String rowId;

	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize;
	private boolean selectBeforeUpdate;
	private boolean hasSubselectLoadableCollections;

	private Boolean isAbstract;

	private String customLoaderName;
	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames = new HashSet<String>();

	/**
	 * Used to instantiate the EntityBinding for an entity that is the root of an inheritance hierarchy
	 *
	 * @param inheritanceType The inheritance type for the hierarchy
	 * @param entityMode The entity mode used in this hierarchy.
	 */
	public EntityBinding(InheritanceType inheritanceType, EntityMode entityMode) {
		this.superEntityBinding = null;
		this.hierarchyDetails = new HierarchyDetails( this, inheritanceType, entityMode );
	}

	/**
	 * Used to instantiate the EntityBinding for an entity that is a subclass (sub-entity) in an inheritance hierarchy
	 *
	 * @param superEntityBinding The entity binding of this binding's super
	 */
	public EntityBinding(EntityBinding superEntityBinding) {
		this.superEntityBinding = superEntityBinding;
		this.hierarchyDetails = superEntityBinding.getHierarchyDetails();
	}


	public HierarchyDetails getHierarchyDetails() {
		return hierarchyDetails;
	}

	public EntityBinding getSuperEntityBinding() {
		return superEntityBinding;
	}

	public boolean isRoot() {
		return superEntityBinding == null;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	@Override
	public TableSpecification getPrimaryTable() {
		return baseTable;
	}

	public void setBaseTable(TableSpecification baseTable) {
		this.baseTable = baseTable;
	}

	public TableSpecification locateTable(String tableName) {
		if ( tableName == null ) {
			return baseTable;
		}

		TableSpecification tableSpec = secondaryTables.get( tableName );
		if ( tableSpec == null ) {
		   throw new AssertionFailure( String.format("Unable to find table %s amongst tables %s", tableName, secondaryTables.keySet()) );
		}
		return tableSpec;
	}

	public void addSecondaryTable(String tableName, TableSpecification table) {
		secondaryTables.put( tableName, table );
	}

	public boolean isVersioned() {
		return getHierarchyDetails().getVersioningAttributeBinding() != null;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public void setDiscriminatorMatchValue(String discriminatorMatchValue) {
		this.discriminatorMatchValue = discriminatorMatchValue;
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition filterDefinition) {
		filterDefinitions.add( filterDefinition );
	}

	public Iterable<SingularAssociationAttributeBinding> getEntityReferencingAttributeBindings() {
		return entityReferencingAttributeBindings;
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return this;
	}

	@Override
	public String getPathBase() {
		return getEntity().getName();
	}

	@Override
	public Class<?> getClassReference() {
		return getEntity().getClassReference();
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return getEntity();
	}

	@Override
	protected void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		if ( SingularAssociationAttributeBinding.class.isInstance( attributeBinding ) ) {
			entityReferencingAttributeBindings.add( (SingularAssociationAttributeBinding) attributeBinding );
		}
		super.registerAttributeBinding( name, attributeBinding );
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public void setMetaAttributeContext(MetaAttributeContext metaAttributeContext) {
		this.metaAttributeContext = metaAttributeContext;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public Value<Class<?>> getProxyInterfaceType() {
		return proxyInterfaceType;
	}

	public void setProxyInterfaceType(Value<Class<?>> proxyInterfaceType) {
		this.proxyInterfaceType = proxyInterfaceType;
	}

	public String getWhereFilter() {
		return whereFilter;
	}

	public void setWhereFilter(String whereFilter) {
		this.whereFilter = whereFilter;
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	/* package-protected */
	void setSubselectLoadableCollections(boolean hasSubselectLoadableCollections) {
		this.hasSubselectLoadableCollections = hasSubselectLoadableCollections;
	}

	public Class<? extends EntityPersister> getCustomEntityPersisterClass() {
		return customEntityPersisterClass;
	}

	public void setCustomEntityPersisterClass(Class<? extends EntityPersister> customEntityPersisterClass) {
		this.customEntityPersisterClass = customEntityPersisterClass;
	}

	public Class<? extends EntityTuplizer> getCustomEntityTuplizerClass() {
		return customEntityTuplizerClass;
	}

	public void setCustomEntityTuplizerClass(Class<? extends EntityTuplizer> customEntityTuplizerClass) {
		this.customEntityTuplizerClass = customEntityTuplizerClass;
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public void addSynchronizedTableNames(java.util.Collection<String> synchronizedTableNames) {
		this.synchronizedTableNames.addAll( synchronizedTableNames );
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public void setJpaEntityName(String jpaEntityName) {
		this.jpaEntityName = jpaEntityName;
	}

	public String getCustomLoaderName() {
		return customLoaderName;
	}

	public void setCustomLoaderName(String customLoaderName) {
		this.customLoaderName = customLoaderName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public void setCustomInsert(CustomSQL customInsert) {
		this.customInsert = customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public void setCustomUpdate(CustomSQL customUpdate) {
		this.customUpdate = customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public void setCustomDelete(CustomSQL customDelete) {
		this.customDelete = customDelete;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityBinding" );
		sb.append( "{entity=" ).append( entity != null ? entity.getName() : "not set" );
		sb.append( '}' );
		return sb.toString();
	}
}
