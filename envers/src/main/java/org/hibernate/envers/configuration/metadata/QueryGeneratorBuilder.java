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
package org.hibernate.envers.configuration.metadata;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.entities.mapper.relation.query.OneEntityQueryGenerator;
import org.hibernate.envers.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.entities.mapper.relation.query.ThreeEntityQueryGenerator;
import org.hibernate.envers.entities.mapper.relation.query.TwoEntityQueryGenerator;

/**
 * Builds query generators, for reading collection middle tables, along with any related entities.
 * The related entities information can be added gradually, and when complete, the query generator can be built.
 * @author Adam Warski (adam at warski dot org)
 */
public final class QueryGeneratorBuilder {
    private final GlobalConfiguration globalCfg;
    private final AuditEntitiesConfiguration verEntCfg;
    private final MiddleIdData referencingIdData;
    private final String versionsMiddleEntityName;
    private final List<MiddleIdData> idDatas;

    QueryGeneratorBuilder(GlobalConfiguration globalCfg, AuditEntitiesConfiguration verEntCfg,
                          MiddleIdData referencingIdData, String versionsMiddleEntityName) {
        this.globalCfg = globalCfg;
        this.verEntCfg = verEntCfg;
        this.referencingIdData = referencingIdData;
        this.versionsMiddleEntityName = versionsMiddleEntityName;

        idDatas = new ArrayList<MiddleIdData>();
    }

    void addRelation(MiddleIdData idData) {
        idDatas.add(idData);
    }

    RelationQueryGenerator build(MiddleComponentData... componentDatas) {
        if (idDatas.size() == 0) {
            return new OneEntityQueryGenerator(verEntCfg, versionsMiddleEntityName, referencingIdData,
                    componentDatas);
        } else if (idDatas.size() == 1) {
            return new TwoEntityQueryGenerator(globalCfg, verEntCfg, versionsMiddleEntityName, referencingIdData,
                    idDatas.get(0), componentDatas);
        } else if (idDatas.size() == 2) {
            return new ThreeEntityQueryGenerator(globalCfg, verEntCfg, versionsMiddleEntityName, referencingIdData, 
                    idDatas.get(0), idDatas.get(1), componentDatas);
        } else {
            throw new IllegalStateException("Illegal number of related entities.");
        }
    }

    /**
     * @return Current index of data in the array, which will be the element of a list, returned when executing a query
     * generated by the built query generator.
     */
    int getCurrentIndex() {
        return idDatas.size();
    }
}
