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
package org.hibernate.envers.test.integration.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.RevisionProperty;
import org.hibernate.envers.query.RevisionTypeProperty;
import org.hibernate.envers.query.AuditRestrictions;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class SimpleQuery extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;
    private Integer id3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site3 = new StrIntTestEntity("b", 5);

        em.persist(site1);
        em.persist(site2);
        em.persist(site3);

        id1 = site1.getId();
        id2 = site2.getId();
        id3 = site3.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setStr1("c");
        site2.setNumber(20);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        site3 = em.find(StrIntTestEntity.class, id3);

        site3.setStr1("a");

        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);

        em.remove(site1);

        em.getTransaction().commit();
    }

    @Test
    public void testEntitiesIdQuery() {
        StrIntTestEntity ver2 = (StrIntTestEntity) getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 2)
                .add(AuditRestrictions.idEq(id2))
                .getSingleResult();

        assert ver2.equals(new StrIntTestEntity("a", 20, id2));
    }

    @Test
    public void testEntitiesPropertyEqualsQuery() {
        List ver1 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 1)
                .add(AuditRestrictions.eq("str1", "a"))
                .getResultList();

        List ver2 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 2)
                .add(AuditRestrictions.eq("str1", "a"))
                .getResultList();

        List ver3 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 3)
                .add(AuditRestrictions.eq("str1", "a"))
                .getResultList();

        assert new HashSet(ver1).equals(TestTools.makeSet(new StrIntTestEntity("a", 10, id1),
                new StrIntTestEntity("a", 10, id2)));
        assert new HashSet(ver2).equals(TestTools.makeSet(new StrIntTestEntity("a", 20, id2)));
        assert new HashSet(ver3).equals(TestTools.makeSet(new StrIntTestEntity("a", 20, id2),
                new StrIntTestEntity("a", 5, id3)));
    }

    @Test
    public void testEntitiesPropertyLeQuery() {
        List ver1 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 1)
                .add(AuditRestrictions.le("number", 10))
                .getResultList();

        List ver2 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 2)
                .add(AuditRestrictions.le("number", 10))
                .getResultList();

        List ver3 = getVersionsReader().createQuery()
                .forEntitiesAtRevision(StrIntTestEntity.class, 3)
                .add(AuditRestrictions.le("number", 10))
                .getResultList();

        assert new HashSet(ver1).equals(TestTools.makeSet(new StrIntTestEntity("a", 10, id1),
                new StrIntTestEntity("a", 10, id2), new StrIntTestEntity("b", 5, id3)));
        assert new HashSet(ver2).equals(TestTools.makeSet(new StrIntTestEntity("c", 10, id1),
                new StrIntTestEntity("b", 5, id3)));
        assert new HashSet(ver3).equals(TestTools.makeSet(new StrIntTestEntity("c", 10, id1),
                new StrIntTestEntity("a", 5, id3)));
    }

    @Test
    public void testRevisionsPropertyEqQuery() {
        List revs_id1 = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(AuditRestrictions.le("str1", "a"))
                .add(AuditRestrictions.idEq(id1))
                .getResultList();

        List revs_id2 = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(AuditRestrictions.le("str1", "a"))
                .add(AuditRestrictions.idEq(id2))
                .getResultList();

        List revs_id3 = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(AuditRestrictions.le("str1", "a"))
                .add(AuditRestrictions.idEq(id3))
                .getResultList();

        assert Arrays.asList(1).equals(revs_id1);
        assert Arrays.asList(1, 2).equals(revs_id2);
        assert Arrays.asList(3).equals(revs_id3);
    }

    @Test
    public void testSelectEntitiesQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, true, false)
                .add(AuditRestrictions.idEq(id1))
                .getResultList();

        assert result.size() == 2;

        assert result.get(0).equals(new StrIntTestEntity("a", 10, id1));
        assert result.get(1).equals(new StrIntTestEntity("c", 10, id1));
    }

    @Test
    public void testSelectEntitiesAndRevisionsQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditRestrictions.idEq(id1))
                .getResultList();

        assert result.size() == 3;

        assert ((Object []) result.get(0))[0].equals(new StrIntTestEntity("a", 10, id1));
        assert ((Object []) result.get(1))[0].equals(new StrIntTestEntity("c", 10, id1));
        assert ((Object []) result.get(2))[0].equals(new StrIntTestEntity(null, null, id1));

        assert ((DefaultRevisionEntity) ((Object []) result.get(0))[1]).getId() == 1;
        assert ((DefaultRevisionEntity) ((Object []) result.get(1))[1]).getId() == 2;
        assert ((DefaultRevisionEntity) ((Object []) result.get(2))[1]).getId() == 4;

        assert ((Object []) result.get(0))[2].equals(RevisionType.ADD);
        assert ((Object []) result.get(1))[2].equals(RevisionType.MOD);
        assert ((Object []) result.get(2))[2].equals(RevisionType.DEL);
    }

    @Test
    public void testSelectRevisionTypeQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionTypeProperty.revisionType())
                .add(AuditRestrictions.idEq(id1))
                .getResultList();

        assert result.size() == 3;

        assert result.get(0).equals(RevisionType.ADD);
        assert result.get(1).equals(RevisionType.MOD);
        assert result.get(2).equals(RevisionType.DEL);
    }

    @Test
    public void testEmptyRevisionOfEntityQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .getResultList();

        assert result.size() == 7;
    }

    @Test
    public void testEmptyConjunctionRevisionOfEntityQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditRestrictions.conjunction())
                .getResultList();

        assert result.size() == 7;
    }

    @Test
    public void testEmptyDisjunctionRevisionOfEntityQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditRestrictions.disjunction())
                .getResultList();

        assert result.size() == 0;
    }
}
