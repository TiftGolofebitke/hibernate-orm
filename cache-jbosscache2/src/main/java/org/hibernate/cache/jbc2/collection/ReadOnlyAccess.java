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
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.CacheException;

/**
 * This defines the strategy for transactional access to collection data in a
 * pessimistic-locking JBossCache using its 2.x APIs. <p/> The read-only access
 * to a JBossCache really is still transactional, just with the extra semantic
 * or guarantee that we will not update data.
 * 
 * @author Steve Ebersole
 */
public class ReadOnlyAccess extends TransactionalAccess {
    private static final Logger log = LoggerFactory.getLogger(ReadOnlyAccess.class);

    public ReadOnlyAccess(CollectionRegionImpl region) {
        super(region);
    }

    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

    @Override
    public SoftLock lockRegion() throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only region");
    }

    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only item");
    }

    @Override
    public void unlockRegion(SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only region");
    }
}
