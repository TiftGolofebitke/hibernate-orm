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
package org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel;

import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 * 
 * @author Hern�n Chanfreau
 * 
 *         Same class from package
 *         org.hibernate.envers.test.integration.superclass changing the Audited
 *         annotation from property str to class level
 */
@MappedSuperclass
@Audited
public class SuperclassOfEntity2 {

	private String str;

	public SuperclassOfEntity2() {
	}

	public SuperclassOfEntity2(String str) {
		this.str = str;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SuperclassOfEntity2))
			return false;

		SuperclassOfEntity2 that = (SuperclassOfEntity2) o;

        //noinspection RedundantIfStatement
        if (str != null ? !str.equals(that.str) : that.str != null)
			return false;

		return true;
	}

	public int hashCode() {
		return (str != null ? str.hashCode() : 0);
	}
}
