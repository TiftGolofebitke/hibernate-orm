package org.hibernate.envers.test.integration.jta;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import java.util.Arrays;

/**
 * Same as {@link org.hibernate.envers.test.integration.basic.Simple}, but in a JTA environment.
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaTransaction extends AbstractEntityTest {
    private TransactionManager tm;
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);

        tm = addJTAConfig(cfg);
    }

    @Test
    public void initData() throws Exception {
        tm.begin();

        newEntityManager();
        EntityManager em = getEntityManager();
        IntTestEntity ite = new IntTestEntity(10);
        em.persist(ite);
        id1 = ite.getId();

        tm.commit();

        //

        tm.begin();

        newEntityManager();
        em = getEntityManager();
        ite = em.find(IntTestEntity.class, id1);
        ite.setNumber(20);

        tm.commit();
    }

    @Test
    public void testRevisionsCounts() throws Exception {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(IntTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        IntTestEntity ver1 = new IntTestEntity(10, id1);
        IntTestEntity ver2 = new IntTestEntity(20, id1);

        assert getAuditReader().find(IntTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(IntTestEntity.class, id1, 2).equals(ver2);
    }
}
