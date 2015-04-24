/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.integration.geolatte;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.geolatte.geom.C3DM;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecodeException;
import org.geolatte.geom.codec.WktDecoder;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import org.hibernate.spatial.testing.TestDataElement;

/**
 * Test class used in unit testing.
 *
 * Not that this is Entity class uses raw Geometries, because in test classes a wide variety of SRIDs and
 * coordinate spaces are mixed. (This creates notable problems for Oracle, which is very, very strict in what it accepts)
 *
 */
@Entity
@Table(name = "geomtest")
public class GeomEntity {

	@Id
	private Integer id;

	private String type;

	private Geometry geom;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

	public static GeomEntity createFrom(TestDataElement element) throws WktDecodeException {
		WktDecoder decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		Geometry geom = decoder.decode( element.wkt );
		GeomEntity result = new GeomEntity();
		result.setId( element.id );
		result.setGeom( geom );
		result.setType( element.type );
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		GeomEntity geomEntity = (GeomEntity) o;

		if ( ! id.equals(geomEntity.id) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}
}
