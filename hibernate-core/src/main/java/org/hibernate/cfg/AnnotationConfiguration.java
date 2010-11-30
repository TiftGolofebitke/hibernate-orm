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
package org.hibernate.cfg;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AnnotationException;
import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.util.CollectionHelper;

/**
 * Similar to the {@link Configuration} object but handles EJB3 and Hibernate
 * specific annotations as a metadata facility.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 *
 * @deprecated All functionality has been moved to {@link Configuration}
 */
@Deprecated
public class AnnotationConfiguration extends Configuration {
	private Logger log = LoggerFactory.getLogger( AnnotationConfiguration.class );

	public AnnotationConfiguration() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AnnotationConfiguration addAnnotatedClass(Class annotatedClass) throws MappingException {
		return (AnnotationConfiguration) super.addAnnotatedClass( annotatedClass );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AnnotationConfiguration addPackage(String packageName) throws MappingException {
		return (AnnotationConfiguration) super.addPackage( packageName );
	}

	public ExtendedMappings createExtendedMappings() {
		return new ExtendedMappingsImpl();
	}

	@Override
	public AnnotationConfiguration addFile(String xmlFile) throws MappingException {
		super.addFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addFile(File xmlFile) throws MappingException {
		super.addFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addCacheableFile(File xmlFile) throws MappingException {
		super.addCacheableFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addCacheableFile(String xmlFile) throws MappingException {
		super.addCacheableFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addXML(String xml) throws MappingException {
		super.addXML( xml );
		return this;
	}

	@Override
	public AnnotationConfiguration addURL(URL url) throws MappingException {
		super.addURL( url );
		return this;
	}

	@Override
	public AnnotationConfiguration addResource(String resourceName, ClassLoader classLoader) throws MappingException {
		super.addResource( resourceName, classLoader );
		return this;
	}

	@Override
	public AnnotationConfiguration addDocument(org.w3c.dom.Document doc) throws MappingException {
		super.addDocument( doc );
		return this;
	}

	@Override
	public AnnotationConfiguration addResource(String resourceName) throws MappingException {
		super.addResource( resourceName );
		return this;
	}

	@Override
	public AnnotationConfiguration addClass(Class persistentClass) throws MappingException {
		super.addClass( persistentClass );
		return this;
	}

	@Override
	public AnnotationConfiguration addJar(File jar) throws MappingException {
		super.addJar( jar );
		return this;
	}

	@Override
	public AnnotationConfiguration addDirectory(File dir) throws MappingException {
		super.addDirectory( dir );
		return this;
	}

	@Override
	public AnnotationConfiguration setInterceptor(Interceptor interceptor) {
		super.setInterceptor( interceptor );
		return this;
	}

	@Override
	public AnnotationConfiguration setProperties(Properties properties) {
		super.setProperties( properties );
		return this;
	}

	@Override
	public AnnotationConfiguration addProperties(Properties extraProperties) {
		super.addProperties( extraProperties );
		return this;
	}

	@Override
	public AnnotationConfiguration mergeProperties(Properties properties) {
		super.mergeProperties( properties );
		return this;
	}

	@Override
	public AnnotationConfiguration setProperty(String propertyName, String value) {
		super.setProperty( propertyName, value );
		return this;
	}

	@Override
	public AnnotationConfiguration configure() throws HibernateException {
		super.configure();
		return this;
	}

	@Override
	public AnnotationConfiguration configure(String resource) throws HibernateException {
		super.configure( resource );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(URL url) throws HibernateException {
		super.configure( url );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(File configFile) throws HibernateException {
		super.configure( configFile );
		return this;
	}

	@Override
	protected AnnotationConfiguration doConfigure(InputStream stream, String resourceName) throws HibernateException {
		super.doConfigure( stream, resourceName );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(org.w3c.dom.Document document) throws HibernateException {
		super.configure( document );
		return this;
	}

	@Override
	protected AnnotationConfiguration doConfigure(Document doc) throws HibernateException {
		super.doConfigure( doc );
		return this;
	}

	@Override
	public AnnotationConfiguration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy) {
		super.setCacheConcurrencyStrategy( clazz, concurrencyStrategy );
		return this;
	}

	@Override
	public AnnotationConfiguration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy, String region) {
		super.setCacheConcurrencyStrategy( clazz, concurrencyStrategy, region );
		return this;
	}

	@Override
	public AnnotationConfiguration setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy)
			throws MappingException {
		super.setCollectionCacheConcurrencyStrategy( collectionRole, concurrencyStrategy );
		return this;
	}

	@Override
	public AnnotationConfiguration setNamingStrategy(NamingStrategy namingStrategy) {
		super.setNamingStrategy( namingStrategy );
		return this;
	}

	@Deprecated
	protected class ExtendedMappingsImpl extends MappingsImpl implements ExtendedMappings {
	}
}
