package org.hibernate.spatial.integration;

import java.util.Map;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.spatial.HibernateSpatialConfiguration;


/**
 * The {@code ServiceInitiator} for Hibernate Spatial.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/13
 */
public class SpatialInitiator implements StandardServiceInitiator {

	@Override
	public Class getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public Service initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final HibernateSpatialConfiguration configuration = configure( registry );
		return new SpatialDialectFactory( configuration );
	}

	private HibernateSpatialConfiguration configure(ServiceRegistry serviceRegistry) {
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		return new HibernateSpatialConfiguration(
				readOgcStrict( configService ),
				readConnectionFinder( configService, classLoaderService )
		);
	}

	/**
	 * Reads the configured property (if present), otherwise returns null
	 */
	private Boolean readOgcStrict(ConfigurationService configService) {
		final String ogcStrictKey = HibernateSpatialConfiguration.AvailableSettings.OGC_STRICT;
		return configService.getSetting(
				ogcStrictKey,
				new ConfigurationService.Converter<Boolean>() {
					@Override
					public Boolean convert(Object value) {
						return Boolean.parseBoolean( value.toString() );
					}
				}, null
		);
	}

	/**
	 * Reads the configured property (if present), otherwise returns null
	 */
	private ConnectionFinder readConnectionFinder(ConfigurationService configService, ClassLoaderService classLoaderService) {
		final String cfKey = HibernateSpatialConfiguration.AvailableSettings.CONNECTION_FINDER;
		final String className = configService.getSetting(
				cfKey,
				new ConfigurationService.Converter<String>() {
					@Override
					public String convert(Object value) {
						if ( value instanceof String ) {
							return (String) value;
						}
						return value.toString();
					}
				}, null
		);

		if ( className == null ) {
			return null;
		}

		try {
			return (ConnectionFinder) classLoaderService.classForName( className ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( " Could not instantiate ConnectionFinder: " + className, e );
		}
	}
}
