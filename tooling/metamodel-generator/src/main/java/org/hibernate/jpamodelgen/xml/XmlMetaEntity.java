// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import org.hibernate.jpamodelgen.IMetaAttribute;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.IMetaEntity;
import org.hibernate.jpamodelgen.ImportContext;
import org.hibernate.jpamodelgen.xml.jaxb.Attributes;
import org.hibernate.jpamodelgen.xml.jaxb.Basic;
import org.hibernate.jpamodelgen.xml.jaxb.ElementCollection;
import org.hibernate.jpamodelgen.xml.jaxb.Embeddable;
import org.hibernate.jpamodelgen.xml.jaxb.EmbeddableAttributes;
import org.hibernate.jpamodelgen.xml.jaxb.Entity;
import org.hibernate.jpamodelgen.xml.jaxb.Id;
import org.hibernate.jpamodelgen.xml.jaxb.ManyToOne;
import org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass;
import org.hibernate.jpamodelgen.xml.jaxb.OneToMany;
import org.hibernate.jpamodelgen.xml.jaxb.OneToOne;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaEntity implements IMetaEntity {

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	final private String clazzName;

	final private String packageName;

	final private ImportContext importContext;

	final private List<IMetaAttribute> members = new ArrayList<IMetaAttribute>();

	private TypeElement element;

	public XmlMetaEntity(Entity ormEntity, String packageName, TypeElement element) {
		this.clazzName = ormEntity.getClazz();
		this.packageName = packageName;
		importContext = new ImportContextImpl( getPackageName() );
		this.element = element;
		Attributes attributes = ormEntity.getAttributes();

		parseAttributes( attributes );
	}

	public XmlMetaEntity(MappedSuperclass mappedSuperclass, String packageName, TypeElement element) {
		this.clazzName = mappedSuperclass.getClazz();
		this.packageName = packageName;
		importContext = new ImportContextImpl( getPackageName() );
		this.element = element;
		Attributes attributes = mappedSuperclass.getAttributes();

		parseAttributes( attributes );
	}

	public XmlMetaEntity(Embeddable embeddable, String packageName, TypeElement element) {
		this.clazzName = embeddable.getClazz();
		this.packageName = packageName;
		importContext = new ImportContextImpl( getPackageName() );
		this.element = element;
		EmbeddableAttributes attributes = embeddable.getAttributes();

		XmlMetaSingleAttribute attribute;
		for ( Basic basic : attributes.getBasic() ) {
			attribute = new XmlMetaSingleAttribute( this, basic.getName(), getType( basic.getName() ) );
			members.add( attribute );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			attribute = new XmlMetaSingleAttribute( this, manyToOne.getName(), getType( manyToOne.getName() ) );
			members.add( attribute );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			attribute = new XmlMetaSingleAttribute( this, oneToOne.getName(), getType( oneToOne.getName() ) );
			members.add( attribute );
		}

		XmlMetaCollection metaCollection;
		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			String[] types = getCollectionType( oneToMany.getName() );
			metaCollection = new XmlMetaCollection( this, oneToMany.getName(), types[0], types[1] );
			members.add( metaCollection );
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			String[] types = getCollectionType( collection.getName() );
			metaCollection = new XmlMetaCollection( this, collection.getName(), types[0], types[1] );
			members.add( metaCollection );
		}
	}

	public String getSimpleName() {
		return clazzName;
	}

	public String getQualifiedName() {
		return packageName + "." + getSimpleName();
	}

	public String getPackageName() {
		return packageName;
	}

	public List<IMetaAttribute> getMembers() {
		return members;
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	private String[] getCollectionType(String propertyName) {
		String types[] = new String[2];
		for ( Element elem : element.getEnclosedElements() ) {
			if ( elem.getSimpleName().toString().equals( propertyName ) ) {
				DeclaredType type = ( ( DeclaredType ) elem.asType() );
				types[0] = type.getTypeArguments().get( 0 ).toString();
				types[1] = COLLECTIONS.get( type.asElement().toString() );
			}
		}
		return types;
	}

	private String getType(String propertyName) {
		String typeName = null;
		for ( Element elem : element.getEnclosedElements() ) {
			if ( elem.getSimpleName().toString().equals( propertyName ) ) {
				switch ( elem.asType().getKind() ) {
					case INT: {
						typeName = "java.lang.Integer";
						break;
					}
					case LONG: {
						typeName = "java.lang.Long";
						break;
					}
					case BOOLEAN: {
						typeName = "java.lang.Boolean";
						break;
					}
					case DECLARED: {
						typeName = elem.asType().toString();
						break;
					}
					case TYPEVAR: {
						typeName = elem.asType().toString();
						break;
					}
				}
				break;
			}
		}
		return typeName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "XmlMetaEntity" );
		sb.append( "{type=" ).append( element );
		sb.append( '}' );
		return sb.toString();
	}

	private void parseAttributes(Attributes attributes) {
		XmlMetaSingleAttribute attribute;

		if ( !attributes.getId().isEmpty() ) {
			// TODO what do we do if there are more than one id nodes?
			Id id = attributes.getId().get( 0 );
			attribute = new XmlMetaSingleAttribute(
					this, id.getName(), getType( id.getName() )
			);
			members.add( attribute );
		}

		for ( Basic basic : attributes.getBasic() ) {
			attribute = new XmlMetaSingleAttribute( this, basic.getName(), getType( basic.getName() ) );
			members.add( attribute );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			attribute = new XmlMetaSingleAttribute( this, manyToOne.getName(), getType( manyToOne.getName() ) );
			members.add( attribute );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			attribute = new XmlMetaSingleAttribute( this, oneToOne.getName(), getType( oneToOne.getName() ) );
			members.add( attribute );
		}

		XmlMetaCollection metaCollection;
		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			String[] types = getCollectionType( oneToMany.getName() );
			metaCollection = new XmlMetaCollection( this, oneToMany.getName(), types[0], types[1] );
			members.add( metaCollection );
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			String[] types = getCollectionType( collection.getName() );
			metaCollection = new XmlMetaCollection( this, collection.getName(), types[0], types[1] );
			members.add( metaCollection );
		}
	}
}
