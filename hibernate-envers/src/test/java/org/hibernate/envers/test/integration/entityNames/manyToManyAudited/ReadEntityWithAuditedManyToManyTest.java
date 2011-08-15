package org.hibernate.envers.test.integration.entityNames.manyToManyAudited;

import org.hibernate.MappingException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractOneSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Hern&aacute;n Chanfreau
 * 
 */
public class ReadEntityWithAuditedManyToManyTest extends AbstractOneSessionTest{

	private long id_car1;
	private long id_car2;
	
	private long id_pers1;
	private long id_pers2;

	private Person person1;
	private Car car1;
	
	private Person person1_1;
	private Car car1_2;
	
	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/entityNames/manyToManyAudited/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
	}
	
	
    @Test
    @Priority(10)
    public void initData() {
    	
    	initializeSession();

        Person pers1 = new Person("Hernan", 28);
        Person pers2 = new Person("Leandro", 29);
        Person pers3 = new Person("Barba", 32);
        Person pers4 = new Person("Camomo", 15);

        //REV 1 
        getSession().getTransaction().begin();
        List<Person > owners = new ArrayList<Person>();
        owners.add(pers1);
        owners.add(pers2);
        owners.add(pers3);
        Car car1 = new Car(5, owners);

        getSession().persist(car1);
        getSession().getTransaction().commit();
        id_pers1 = pers1.getId();
        id_car1 = car1.getId();
		id_pers2 = pers2.getId();

        owners = new ArrayList<Person>();
        owners.add(pers2);
        owners.add(pers3);
        owners.add(pers4);
        Car car2 = new Car(27, owners);
        //REV 2
        getSession().getTransaction().begin();
        Person person1 = (Person)getSession().get("Personaje", id_pers1);
        person1.setName("Hernan David");
        person1.setAge(40);
        getSession().persist(car1);
        getSession().persist(car2);
        getSession().getTransaction().commit();
        id_car2 = car2.getId();
    }
    
    private void loadDataOnSessionAndAuditReader() {
    	
    	car1_2 = getAuditReader().find(Car.class, id_car1, 2);
    	Car car2_2 = getAuditReader().find(Car.class, id_car2, 2);

    	// navigate through relations to load objects
    	for (Person owner : car1_2.getOwners()) {
    		for (Car ownedCar : owner.getCars()) {
    			ownedCar.getNumber();
			}
		}
    	for (Person owner : car2_2.getOwners()) {
    		for (Car ownedCar : owner.getCars()) {
				ownedCar.getNumber();
			}
		}
    	
    	car1 = (Car)getSession().get(Car.class, id_car1);
    	person1 = (Person)getSession().get("Personaje", id_pers1);
    	person1_1 = getAuditReader().find(Person.class, "Personaje", id_pers1, 1);
    }
    


	private void checkEntityNames() {
		String currPerson1EN = getSession().getEntityName(person1);
    	String currCar1EN = getSession().getEntityName(car1);
    	
    	String person1_1EN = getAuditReader().getEntityName(id_pers1, 1, person1_1);
    	assert(currPerson1EN.equals(person1_1EN));
    	
    	String car1_2EN = getAuditReader().getEntityName(id_car1, 2, car1_2);
    	assert(currCar1EN.equals(car1_2EN));
	}
    
    @Test
    public void testGetEntityNameManyYoManyWithEntityName() {
    	
    	loadDataOnSessionAndAuditReader();
    	
    	checkEntityNames();
    }

    
    @Test
    public void testGetEntityNameManyYoManyWithEntityNameInNewSession() {
    	//force new session and AR
    	forceNewSession();
    	loadDataOnSessionAndAuditReader();
    	
    	checkEntityNames();

    }

	@Test
	public void testHasChangedPerson1() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity(Person.class, "Personaje", false, false)
				.add(AuditEntity.id().eq(id_pers1))
				.add(AuditEntity.property("cars").hasChanged())
				.getResultList();
		assertEquals(1, list.size());
		assertEquals(TestTools.makeList(1), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(Person.class, "Personaje", false, false)
				.add(AuditEntity.id().eq(id_pers1))
				.add(AuditEntity.property("cars").hasNotChanged())
				.getResultList();
		assertEquals(1, list.size());
		assertEquals(TestTools.makeList(2), extractRevisionNumbers(list));
	}

	@Test
	public void testHasChangedPerson2() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity(Person.class, "Personaje", false, false)
				.add(AuditEntity.id().eq(id_pers2))
				.add(AuditEntity.property("cars").hasChanged())
				.getResultList();
		assertEquals(2, list.size());
		assertEquals(TestTools.makeList(1, 2), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(Person.class, "Personaje", false, false)
				.add(AuditEntity.id().eq(id_pers2))
				.add(AuditEntity.property("cars").hasNotChanged())
				.getResultList();
		assertEquals(0, list.size());
	}

	@Test
	public void testHasChangedCar1() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity(Car.class, false, false)
				.add(AuditEntity.id().eq(id_car1))
				.add(AuditEntity.property("owners").hasChanged())
				.getResultList();
		assertEquals(1, list.size());
		assertEquals(TestTools.makeList(1), extractRevisionNumbers(list));

		list = getAuditReader().createQuery().forRevisionsOfEntity(Car.class, false, false)
				.add(AuditEntity.id().eq(id_car1))
				.add(AuditEntity.property("owners").hasNotChanged())
				.getResultList();
		assertEquals(0, list.size());
	}
}
