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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;

import org.junit.Test;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Tests for {@code o.h.a.Cache} and {@code j.p.Cacheable}.
 *
 * @author Hardy Ferentschik
 */
public class CacheBindingTests extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = HibernateCacheEntity.class, cacheMode = SharedCacheMode.ALL)
	public void testHibernateCaching() {
		EntityBinding binding = getEntityBinding( HibernateCacheEntity.class );
		assertNotNull( "There should be a cache binding", binding.getCaching() );
		Caching caching = binding.getCaching();
		assertEquals( "Wrong region", "foo", caching.getRegion() );
		assertEquals( "Wrong strategy", AccessType.READ_WRITE, caching.getAccessType() );
		assertEquals( "Wrong lazy properties configuration", false, caching.isCacheLazyProperties() );
	}

	@Test
	@Resources(annotatedClasses = JpaCacheEntity.class, cacheMode = SharedCacheMode.ALL)
	public void testJpaCaching() {
		EntityBinding binding = getEntityBinding( JpaCacheEntity.class );
		assertNotNull( "There should be a cache binding", binding.getCaching() );
		Caching caching = binding.getCaching();
		assertEquals(
				"Wrong region",
				this.getClass().getName() + "$" + JpaCacheEntity.class.getSimpleName(),
				caching.getRegion()
		);
		assertEquals( "Wrong lazy properties configuration", true, caching.isCacheLazyProperties() );
	}

	@Test
	@Resources(annotatedClasses = NoCacheEntity.class, cacheMode = SharedCacheMode.NONE)
	public void testNoCaching() {
		EntityBinding binding = getEntityBinding( NoCacheEntity.class );
		assertNull( "There should be no cache binding", binding.getCaching() );
	}

	@Entity
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "foo", include = "non-lazy")
	class HibernateCacheEntity {
		@Id
		private int id;
	}

	@Entity
	@Cacheable
	class JpaCacheEntity {
		@Id
		private int id;
	}

	@Entity
	@Cacheable
	class NoCacheEntity {
		@Id
		private int id;
	}
}


