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
 */

package org.hibernate.envers.test.integration.inheritance.joined;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildAuditing extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildEntity.class);
        cfg.addAnnotatedClass(ParentEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        id1 = 1;

        // Rev 1
        em.getTransaction().begin();
        ChildEntity ce = new ChildEntity(id1, "x", 1l);
        em.persist(ce);
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        ce = em.find(ChildEntity.class, id1);
        ce.setData("y");
        ce.setNumber(2l);
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(ChildEntity.class, id1));
    }

    @Test
    public void testHistoryOfChildId1() {
        ChildEntity ver1 = new ChildEntity(id1, "x", 1l);
        ChildEntity ver2 = new ChildEntity(id1, "y", 2l);

        assert getAuditReader().find(ChildEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(ChildEntity.class, id1, 2).equals(ver2);

        assert getAuditReader().find(ParentEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(ParentEntity.class, id1, 2).equals(ver2);
    }

    @Test
    public void testPolymorphicQuery() {
        ChildEntity childVer1 = new ChildEntity(id1, "x", 1l);

        assert getAuditReader().createQuery().forEntitiesAtRevision(ChildEntity.class, 1).getSingleResult()
                .equals(childVer1);

        assert getAuditReader().createQuery().forEntitiesAtRevision(ParentEntity.class, 1).getSingleResult()
                .equals(childVer1);
    }

	@Test
	public void testChildHasChanged() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity(ChildEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("data").hasChanged())
				.getResultList();
		assertEquals(2, list.size());
		assertEquals(TestTools.makeList(1, 2), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(ChildEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("number").hasChanged())
				.getResultList();
		assertEquals(2, list.size());
		assertEquals(TestTools.makeList(1, 2), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(ChildEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("data").hasNotChanged())
				.getResultList();
		assertEquals(0, list.size());

		list = getAuditReader().createQuery().forRevisionsOfEntity(ChildEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("number").hasNotChanged())
				.getResultList();
		assertEquals(0, list.size());
	}

	@Test
	public void testParentHasChanged() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity(ParentEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("data").hasChanged())
				.getResultList();
		assertEquals(2, list.size());
		assertEquals(TestTools.makeList(1, 2), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(ParentEntity.class, false, false)
				.add(AuditEntity.id().eq(id1))
				.add(AuditEntity.property("data").hasNotChanged())
				.getResultList();
		assertEquals(0, list.size());
	}
}