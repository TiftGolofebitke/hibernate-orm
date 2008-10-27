package org.jboss.envers.test.integration.onetomany;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalMapKey extends AbstractEntityTest {
    private Integer ed_id;

    private Integer ing1_id;
    private Integer ing2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(RefIngMapKeyEntity.class);
        cfg.addAnnotatedClass(RefEdMapKeyEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Revision 1 (intialy 1 relation: ing1 -> ed)
        em.getTransaction().begin();

        RefEdMapKeyEntity ed = new RefEdMapKeyEntity();

        em.persist(ed);

        RefIngMapKeyEntity ing1 = new RefIngMapKeyEntity();
        ing1.setData("a");
        ing1.setReference(ed);

        RefIngMapKeyEntity ing2 = new RefIngMapKeyEntity();
        ing2.setData("b");

        em.persist(ing1);
        em.persist(ing2);

        em.getTransaction().commit();

        // Revision 2 (adding second relation: ing2 -> ed)
        em.getTransaction().begin();

        ed = em.find(RefEdMapKeyEntity.class, ed.getId());
        ing2 = em.find(RefIngMapKeyEntity.class, ing2.getId());

        ing2.setReference(ed);

        em.getTransaction().commit();

        //

        ed_id = ed.getId();

        ing1_id = ing1.getId();
        ing2_id = ing2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(RefEdMapKeyEntity.class, ed_id));

        assert Arrays.asList(1).equals(getVersionsReader().getRevisions(RefIngMapKeyEntity.class, ing1_id));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(RefIngMapKeyEntity.class, ing2_id));
    }

    @Test
    public void testHistoryOfEd() {
        RefIngMapKeyEntity ing1 = getEntityManager().find(RefIngMapKeyEntity.class, ing1_id);
        RefIngMapKeyEntity ing2 = getEntityManager().find(RefIngMapKeyEntity.class, ing2_id);

        RefEdMapKeyEntity rev1 = getVersionsReader().find(RefEdMapKeyEntity.class, ed_id, 1);
        RefEdMapKeyEntity rev2 = getVersionsReader().find(RefEdMapKeyEntity.class, ed_id, 2);

        assert rev1.getIdmap().equals(TestTools.makeMap("a", ing1));
        assert rev2.getIdmap().equals(TestTools.makeMap("a", ing1, "b", ing2));
    }
}