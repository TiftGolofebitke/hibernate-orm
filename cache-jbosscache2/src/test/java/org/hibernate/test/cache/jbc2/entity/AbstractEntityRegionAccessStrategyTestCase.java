/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc2.entity;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc2.entity.EntityRegionImpl;
import org.hibernate.cache.jbc2.entity.TransactionalAccess;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.hibernate.cache.jbc2.util.NonLockingDataVersion;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc2.AbstractJBossCacheTestCase;
import org.hibernate.test.util.CacheTestUtil;
import org.hibernate.util.ComparableComparator;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/** 
 * Base class for tests of EntityRegionAccessStrategy impls.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractEntityRegionAccessStrategyTestCase extends AbstractJBossCacheTestCase {

    public static final String REGION_NAME = "test/com.foo.test";
    public static final String KEY = "KEY";
    public static final String VALUE1 = "VALUE1";
    public static final String VALUE2 = "VALUE2";
    
    protected static Configuration localCfg;
    protected static JBossCacheRegionFactory localRegionFactory;
    protected static Cache localCache;
    protected static Configuration remoteCfg;
    protected static JBossCacheRegionFactory remoteRegionFactory;
    protected static Cache remoteCache;
    
    protected static boolean invalidation;
    protected static boolean optimistic;
    protected static boolean synchronous;
    
    protected EntityRegion localEntityRegion;
    protected EntityRegionAccessStrategy localAccessStrategy;

    protected EntityRegion remoteEntityRegion;
    protected EntityRegionAccessStrategy remoteAccessStrategy;
    
    protected Exception node1Exception;
    protected Exception node2Exception;
    
    protected AssertionFailedError node1Failure;
    protected AssertionFailedError node2Failure;
    
    
    public static Test getTestSetup(Class testClass, String configName) {
        TestSuite suite = new TestSuite(testClass);
        return new AccessStrategyTestSetup(suite, configName);
    }
    
    public static Test getTestSetup(Test test, String configName) {
        return new AccessStrategyTestSetup(test, configName);
    }
    
    
    /**
     * Create a new TransactionalAccessTestCase.
     * 
     * @param name
     */
    public AbstractEntityRegionAccessStrategyTestCase(String name) {
        super(name);
    }

    protected abstract AccessType getAccessType();

    protected void setUp() throws Exception {
        super.setUp();
        
        localEntityRegion = localRegionFactory.buildEntityRegion(REGION_NAME, localCfg.getProperties(), getCacheDataDescription());
        localAccessStrategy = localEntityRegion.buildAccessStrategy(getAccessType());
        
        remoteEntityRegion = remoteRegionFactory.buildEntityRegion(REGION_NAME, remoteCfg.getProperties(), getCacheDataDescription());
        remoteAccessStrategy = remoteEntityRegion.buildAccessStrategy(getAccessType());
                
        node1Exception = null;
        node2Exception = null;
        
        node1Failure = null;
        node2Failure  = null;
    }

    protected void tearDown() throws Exception {
        
        super.tearDown();
        
        if (localEntityRegion != null)
            localEntityRegion.destroy();
        if (remoteEntityRegion != null)
            remoteEntityRegion.destroy();
        
        try {
            localCache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
            localCache.removeNode(Fqn.ROOT);
        }
        catch (Exception e) {
            log.error("Problem purging local cache" ,e);
        }
        
        try {
            remoteCache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
            remoteCache.removeNode(Fqn.ROOT);
        }
        catch (Exception e) {
            log.error("Problem purging remote cache" ,e);
        }
        
        node1Exception = null;
        node2Exception = null;
        
        node1Failure = null;
        node2Failure  = null;
    }
    
    protected static Configuration createConfiguration(String configName) {
        Configuration cfg = CacheTestUtil.buildConfiguration(REGION_PREFIX, MultiplexedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, configName);
        return cfg;
    }
    
    protected CacheDataDescription getCacheDataDescription() {
        return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
    }
    
    protected boolean isUsingOptimisticLocking() {
        return optimistic;
    }
    
    protected boolean isUsingInvalidation() {
        return invalidation;
    }
    
    protected boolean isSynchronous() {
        return synchronous;
    }
    
    protected Fqn getRegionFqn(String regionName, String regionPrefix) {
        return BasicRegionAdapter.getTypeLastRegionFqn(regionName, regionPrefix, EntityRegionImpl.TYPE);
    }
    
    /**
     * This is just a setup test where we assert that the cache config is
     * as we expected.
     */
    public abstract void testCacheConfiguration();
    
    /**
     * Test method for {@link TransactionalAccess#getRegion()}.
     */
    public void testGetRegion() {
        assertEquals("Correct region", localEntityRegion, localAccessStrategy.getRegion());
    }

    /**
     * Test method for {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)}.
     */
    public void testPutFromLoad() throws Exception {
        putFromLoadTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)}.
     */
    public void testPutFromLoadMinimal() throws Exception {
        putFromLoadTest(true);
    }
    
    /**
     * Simulate 2 nodes, both start, tx do a get, experience a cache miss,
     * then 'read from db.' First does a putFromLoad, then an update.
     * Second tries to do a putFromLoad with stale data (i.e. it took
     * longer to read from the db).  Both commit their tx. Then
     * both start a new tx and get. First should see the updated data;
     * second should either see the updated data (isInvalidation()( == false)
     * or null (isInvalidation() == true).
     * 
     * @param useMinimalAPI
     * @throws Exception
     */
    private void putFromLoadTest(final boolean useMinimalAPI) throws Exception {
        
        final CountDownLatch writeLatch1 = new CountDownLatch(1);
        final CountDownLatch writeLatch2 = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        
        Thread node1 = new Thread() {          
            
            public void run() {
                
                try {       
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    assertNull("node1 starts clean", localAccessStrategy.get(KEY, txTimestamp));
                    
                    writeLatch1.await();
                    
                    if (useMinimalAPI) {
                        localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);                        
                    }
                    else {
                        localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
                    }
                    
                    localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    // Let node2 write
                    writeLatch2.countDown();
                    completionLatch.countDown();
                }
            }
        };
        
        Thread node2 = new Thread() {
          
            public void run() { 
                
                try {                  
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    assertNull("node1 starts clean", remoteAccessStrategy.get(KEY, txTimestamp));
                    
                    // Let node1 write
                    writeLatch1.countDown();
                    // Wait for node1 to finish
                    writeLatch2.await();
                    
                    if (useMinimalAPI) {
                        remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);                        
                    }
                    else {
                        remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
                    }
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node2 caught exception", e);
                    node2Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node2Failure = e;
                    rollback();
                }
                finally {
                    completionLatch.countDown();
                }
            }
        };
        
        node1.setDaemon(true);
        node2.setDaemon(true);
        
        node1.start();
        node2.start();        
        
        assertTrue("Threads completed", completionLatch.await(2, TimeUnit.SECONDS));
        
        if (node1Failure != null)
            throw node1Failure;
        if (node2Failure != null)
            throw node2Failure;
        
        assertEquals("node1 saw no exceptions", null, node1Exception);
        assertEquals("node2 saw no exceptions", null, node2Exception);
        
        long txTimestamp = System.currentTimeMillis();
        assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(KEY, txTimestamp));
        
        if (isUsingInvalidation()) {
            if (isUsingOptimisticLocking())
                // The node is "deleted" but it's ghost data version prevents the stale node2 PFER 
                assertEquals("Correct node2 value", null, remoteAccessStrategy.get(KEY, txTimestamp));
            else {
                // no data version to prevent the PFER; we count on db locks preventing this
                assertEquals("Expected node2 value", VALUE1, remoteAccessStrategy.get(KEY, txTimestamp));                
            }
        }
        else {
            // The node1 update is replicated, preventing the node2 PFER
            assertEquals("Correct node2 value", VALUE2, remoteAccessStrategy.get(KEY, txTimestamp));            
        }
    }

    /**
     * Test method for {@link TransactionalAccess#insert(java.lang.Object, java.lang.Object, java.lang.Object)}.
     */
    public void testInsert() throws Exception {
        
        final CountDownLatch readLatch = new CountDownLatch(1);
        final CountDownLatch commitLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        
        Thread inserter = new Thread() {          
            
            public void run() {
                
                try {       
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    assertNull("Correct initial value", localAccessStrategy.get(KEY, txTimestamp));
                    
                    localAccessStrategy.insert(KEY, VALUE1, new Integer(1));
                    
                    readLatch.countDown();
                    commitLatch.await();
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    completionLatch.countDown();
                }
            }
        };
        
        Thread reader = new Thread() {          
            
            public void run() {
                
                try {       
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    readLatch.await();
                    Object expected = isUsingOptimisticLocking() ? null : VALUE1;
                    
                    assertEquals("Correct initial value", expected, localAccessStrategy.get(KEY, txTimestamp));
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    commitLatch.countDown();
                    completionLatch.countDown();
                }
            }
        };
        
        inserter.setDaemon(true);
        reader.setDaemon(true);
        inserter.start();
        reader.start();
        
        if (isUsingOptimisticLocking())
            assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));
        else {
            // Reader should be blocking for lock
            assertFalse("Threads completed", completionLatch.await(250, TimeUnit.MILLISECONDS));
            commitLatch.countDown();
            assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));
        }
        
        if (node1Failure != null)
            throw node1Failure;
        if (node2Failure != null)
            throw node2Failure;
        
        assertEquals("node1 saw no exceptions", null, node1Exception);
        assertEquals("node2 saw no exceptions", null, node2Exception);
        
        long txTimestamp = System.currentTimeMillis();
        assertEquals("Correct node1 value", VALUE1, localAccessStrategy.get(KEY, txTimestamp));
        Object expected = isUsingInvalidation() ? null : VALUE1;
        assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(KEY, txTimestamp));
    }

    /**
     * Test method for {@link TransactionalAccess#update(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)}.
     */
    public void testUpdate() throws Exception {
        
        // Set up initial state
        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        
        // Let the async put propagate
        sleep(250);
        
        final CountDownLatch readLatch = new CountDownLatch(1);
        final CountDownLatch commitLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        
        Thread updater = new Thread() {          
            
            public void run() {
                
                try {       
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(KEY, txTimestamp));
                    
                    localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));
                    
                    readLatch.countDown();
                    commitLatch.await();
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    completionLatch.countDown();
                }
            }
        };
        
        Thread reader = new Thread() {          
            
            public void run() {                
                try {       
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();
                    
                    readLatch.await();
                    
                    // This will block w/ pessimistic locking and then
                    // read the new value; w/ optimistic it shouldn't
                    // block and will read the old value
                    Object expected = isUsingOptimisticLocking() ? VALUE1 : VALUE2;
                    assertEquals("Correct value", expected, localAccessStrategy.get(KEY, txTimestamp));
                    
                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    commitLatch.countDown();
                    completionLatch.countDown();
                }                
            }
        };
        
        updater.setDaemon(true);
        reader.setDaemon(true);
        updater.start();
        reader.start();
        
        if (isUsingOptimisticLocking())
            // Should complete promptly
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS));
        else {        
            // Reader thread should be blocking
            assertFalse(completionLatch.await(250, TimeUnit.MILLISECONDS));
            // Let the writer commit down
            commitLatch.countDown();
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS));
        }
        
        if (node1Failure != null)
            throw node1Failure;
        if (node2Failure != null)
            throw node2Failure;
        
        assertEquals("node1 saw no exceptions", null, node1Exception);
        assertEquals("node2 saw no exceptions", null, node2Exception);
        
        long txTimestamp = System.currentTimeMillis();
        assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(KEY, txTimestamp));
        Object expected = isUsingInvalidation() ? null : VALUE2;
        assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(KEY, txTimestamp));
    }

    /**
     * Test method for {@link TransactionalAccess#remove(java.lang.Object)}.
     */
    public void testRemove() {
        evictOrRemoveTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#removeAll()}.
     */
    public void testRemoveAll() {
        evictOrRemoveAllTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#evict(java.lang.Object)}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the EntityRegionAccessStrategy API.
     */
    public void testEvict() {
        evictOrRemoveTest(true);
    }

    /**
     * Test method for {@link TransactionalAccess#evictAll()}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the EntityRegionAccessStrategy API.
     */
    public void testEvictAll() {
        evictOrRemoveAllTest(true);
    }

    private void evictOrRemoveTest(boolean evict) {
        assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
        
        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
        
        // Wait for async propagation
        sleep(250);
        
        if (evict)
            localAccessStrategy.evict(KEY);
        else
            localAccessStrategy.remove(KEY);
        
        assertEquals(null, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        
//        sleep(1000);
        
        assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
    }

    private void evictOrRemoveAllTest(boolean evict) {
        
        Fqn regionFqn = getRegionFqn(REGION_NAME, REGION_PREFIX);
        
        Node regionRoot = localCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, regionRoot.getChildrenNames().size());
        assertTrue(regionRoot.isResident());
        
        if (isUsingOptimisticLocking()) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }

        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, regionRoot.getChildrenNames().size());
        assertTrue(regionRoot.isResident());
        
        if (isUsingOptimisticLocking()) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }
        
        assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
        
        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
        
        // Wait for async propagation
        sleep(250);
        

        
        if (isUsingOptimisticLocking()) {
            regionRoot = localCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
            regionRoot = remoteCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }
        
        if (evict)
            localAccessStrategy.evictAll();
        else
            localAccessStrategy.removeAll();
        
        regionRoot = localCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, regionRoot.getChildrenNames().size());
        assertTrue(regionRoot.isResident());

        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        if (isUsingInvalidation()) {
            // JBC seems broken: see http://www.jboss.com/index.html?module=bb&op=viewtopic&t=121408
            // FIXME   replace with the following when JBCACHE-1199 and JBCACHE-1200 are done:
            //assertFalse(regionRoot.isValid());
            checkNodeIsEmpty(regionRoot);
        }
        else {
            // Same assertion, just different assertion msg
            assertEquals(0, regionRoot.getChildrenNames().size());
        }        
        assertTrue(regionRoot.isResident());
        
        assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
    }
    
    private void checkNodeIsEmpty(Node node) {
        assertEquals("Known issue JBCACHE-1200. node " + node.getFqn() + " should not have keys", 0, node.getKeys().size());
        for (Iterator it = node.getChildren().iterator(); it.hasNext(); ) {
            checkNodeIsEmpty((Node) it.next());
        }
    }
    
    protected void rollback() {
        try {
            BatchModeTransactionManager.getInstance().rollback();
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
    }
    
    private static class AccessStrategyTestSetup extends TestSetup {
        
        private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";
        
        private String configName;
        private String preferIPv4Stack;
        
        public AccessStrategyTestSetup(Test test, String configName) {
            super(test);
            this.configName = configName;
        }

        @Override
        protected void setUp() throws Exception {
            try {
                super.tearDown();
            }
            finally {
                if (preferIPv4Stack == null)
                    System.clearProperty(PREFER_IPV4STACK);
                else 
                    System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);                
            }
            
            // Try to ensure we use IPv4; otherwise cluster formation is very slow 
            preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
            System.setProperty(PREFER_IPV4STACK, "true");
            
            localCfg = createConfiguration(configName);
            localRegionFactory = CacheTestUtil.startRegionFactory(localCfg);
            localCache = localRegionFactory.getCacheInstanceManager().getEntityCacheInstance();
            
            remoteCfg = createConfiguration(configName);
            remoteRegionFactory  = CacheTestUtil.startRegionFactory(remoteCfg);
            remoteCache = remoteRegionFactory.getCacheInstanceManager().getEntityCacheInstance();
            
            invalidation = CacheHelper.isClusteredInvalidation(localCache);
            synchronous = CacheHelper.isSynchronous(localCache);
            optimistic = localCache.getConfiguration().getNodeLockingScheme() == org.jboss.cache.config.Configuration.NodeLockingScheme.OPTIMISTIC;
        }

        @Override
        protected void tearDown() throws Exception {            
            super.tearDown();
            
            if (localRegionFactory != null)
                localRegionFactory.stop();
            
            if (remoteRegionFactory != null)
                remoteRegionFactory.stop();
        }
        
        
    }

}
