/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.test.util;

import java.util.Properties;

import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.SharedJBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.test.tm.jbc2.BatchModeTransactionManagerLookup;

/**
 * Utilities for cache testing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CacheTestUtil {

    public static String LOCAL_OPTIMISIC_CACHE;
    public static String LOCAL_PESSIMISTIC_CACHE;
    
    static {
        String pkg = CacheTestUtil.class.getPackage().getName().replace('.', '/');
        LOCAL_OPTIMISIC_CACHE = pkg + "/optimistic-local-cache.xml";
        LOCAL_PESSIMISTIC_CACHE = pkg + "/pessimistic-local-cache.xml";
    }
    
    public static Configuration buildConfiguration(String regionPrefix, Class regionFactory, boolean use2ndLevel, boolean useQueries) {
        
        Configuration cfg = new Configuration();
        cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
        cfg.setProperty(Environment.USE_STRUCTURED_CACHE, "true");
//        cfg.setProperty(Environment.CONNECTION_PROVIDER, DummyConnectionProvider.class.getName());
        cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, BatchModeTransactionManagerLookup.class.getName());

        cfg.setProperty(Environment.CACHE_REGION_FACTORY, regionFactory.getName());
        cfg.setProperty(Environment.CACHE_REGION_PREFIX, regionPrefix);
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, String.valueOf(use2ndLevel));
        cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(useQueries));
        
        return cfg;
    }
    
    public static Configuration buildLocalOnlyConfiguration(String regionPrefix, boolean optimistic, boolean use2ndLevel, boolean useQueries) {
        Configuration cfg = buildConfiguration(regionPrefix, SharedJBossCacheRegionFactory.class, use2ndLevel, useQueries);
        
        String resource = CacheTestUtil.class.getPackage().getName().replace('.', '/') + "/";
        resource += optimistic ? "optimistic" : "pessimistic";
        resource += "-local-cache.xml";
        
        cfg.setProperty(SharedCacheInstanceManager.CACHE_RESOURCE_PROP, resource);
        
        return cfg;
    }
    
    public static JBossCacheRegionFactory startRegionFactory(Configuration cfg) 
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        
        Settings settings = cfg.buildSettings();
        Properties properties = cfg.getProperties();
        
        String factoryType = cfg.getProperty(Environment.CACHE_REGION_FACTORY);
        Class factoryClass = Thread.currentThread().getContextClassLoader().loadClass(factoryType);
        JBossCacheRegionFactory regionFactory = (JBossCacheRegionFactory) factoryClass.newInstance();
        
        regionFactory.start(settings, properties);
        
        return regionFactory;        
    }
    
    public static JBossCacheRegionFactory startRegionFactory(Configuration cfg, CacheTestSupport testSupport) 
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    
        JBossCacheRegionFactory factory = startRegionFactory(cfg);
        testSupport.registerFactory(factory);
        return factory;
    }
    
    public static void stopRegionFactory(JBossCacheRegionFactory factory, CacheTestSupport testSupport) {
    
        factory.stop();
        testSupport.unregisterFactory(factory);
    }
    
    /**
     * Prevent instantiation. 
     */
    private CacheTestUtil() {        
    }

}
