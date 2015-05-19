/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu
 */
class ReadWriteEntityRegionAccessStrategy extends AbstractReadWriteAccessStrategy
		implements EntityRegionAccessStrategy {
	private final EntityRegionImpl region;

	ReadWriteEntityRegionAccessStrategy(EntityRegionImpl region) {
		this.region = region;
	}

	@Override
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {

		try {
			writeLock.lock();
			Lockable item = (Lockable) region.get( key );
			if ( item == null ) {
				region.put( key, new Item( value, version, region.nextTimestamp() ) );
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}


	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		try {
			writeLock.lock();
			Lockable item = (Lockable) region.get( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				Lock lockItem = (Lock) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( key, lockItem );
					return false;
				}
				else {
					region.put( key, new Item( value, currentVersion, region.nextTimestamp() ) );
					return true;
				}
			}
			else {
				handleLockExpiry( key, item );
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}


	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}

	@Override
	Comparator getVersionComparator() {
		return region.getCacheDataDescription().getVersionComparator();
	}

	@Override
	public EntityRegion getRegion() {
		return region;
	}
}
