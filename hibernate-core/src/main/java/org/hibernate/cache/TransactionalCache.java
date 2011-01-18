/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.cache;

import java.util.Comparator;
import org.hibernate.Logger;
import org.hibernate.cache.access.SoftLock;

/**
 * Support for fully transactional cache implementations like
 * JBoss TreeCache. Note that this might be a less scalable
 * concurrency strategy than <tt>ReadWriteCache</tt>. This is
 * a "synchronous" concurrency strategy.
 *
 * @author Gavin King
 */
public class TransactionalCache implements CacheConcurrencyStrategy {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Logger.class.getPackage().getName());

	private Cache cache;

	public String getRegionName() {
		return cache.getRegionName();
	}

	public Object get(Object key, long txTimestamp) throws CacheException {
        LOG.debug("Cache lookup: " + key);
		Object result = cache.read( key );
        if (result == null) LOG.debug("Cache miss: " + key);
        else LOG.debug("Cache hit: " + key);
		return result;
	}

	public boolean put(
			Object key,
	        Object value,
	        long txTimestamp,
	        Object version,
	        Comparator versionComparator,
	        boolean minimalPut) throws CacheException {
		if ( minimalPut && cache.read( key ) != null ) {
            LOG.debug("Item already cached: " + key);
			return false;
		}
        LOG.debug("Caching: " + key);
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeLoad( key, value, version );
		}
		else {
			cache.put( key, value );
		}
		return true;
	}

	/**
	 * Do nothing, returning null.
	 */
	public SoftLock lock(Object key, Object version) throws CacheException {
		//noop
		return null;
	}

	/**
	 * Do nothing.
	 */
	public void release(Object key, SoftLock clientLock) throws CacheException {
		//noop
	}

	public boolean update(
			Object key,
	        Object value,
	        Object currentVersion,
	        Object previousVersion) throws CacheException {
        LOG.debug("Updating: " + key);
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeUpdate( key, value, currentVersion, previousVersion );
		}
		else {
			cache.update( key, value );
		}
		return true;
	}

	public boolean insert(
			Object key,
	        Object value,
	        Object currentVersion) throws CacheException {
        LOG.debug("Inserting: " + key);
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeInsert( key, value, currentVersion );
		}
		else {
			cache.update( key, value );
		}
		return true;
	}

	public void evict(Object key) throws CacheException {
		cache.remove( key );
	}

	public void remove(Object key) throws CacheException {
        LOG.debug("Removing: " + key);
		cache.remove( key );
	}

	public void clear() throws CacheException {
        LOG.debug("Clearing");
		cache.clear();
	}

	public void destroy() {
		try {
			cache.destroy();
		}
		catch ( Exception e ) {
            LOG.unableToDestroyCache(e.getMessage());
		}
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	/**
	 * Do nothing.
	 */
	public boolean afterInsert(
			Object key,
	        Object value,
	        Object version) throws CacheException {
		return false;
	}

	/**
	 * Do nothing.
	 */
	public boolean afterUpdate(
			Object key,
	        Object value,
	        Object version,
	        SoftLock clientLock) throws CacheException {
		return false;
	}

	@Override
    public String toString() {
		return cache + "(transactional)";
	}

}
