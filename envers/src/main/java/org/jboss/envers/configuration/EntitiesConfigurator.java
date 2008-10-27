/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.configuration;

import org.jboss.envers.entities.EntitiesConfigurations;
import org.jboss.envers.configuration.metadata.VersionsMetadataGenerator;
import org.jboss.envers.configuration.metadata.PersistentClassVersioningData;
import org.jboss.envers.configuration.metadata.AnnotationsMetadataReader;
import org.jboss.envers.configuration.metadata.EntityXmlMappingData;
import org.jboss.envers.tools.graph.GraphTopologicalSort;
import org.jboss.envers.tools.reflection.YReflectionManager;
import org.jboss.envers.tools.StringTools;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurator {
    public EntitiesConfigurations configure(Configuration cfg, YReflectionManager reflectionManager,
                                            GlobalConfiguration globalCfg, VersionsEntitiesConfiguration verEntCfg,
                                            Document revisionInfoXmlMapping, Element revisionInfoRelationMapping) {
        VersionsMetadataGenerator versionsMetaGen = new VersionsMetadataGenerator(cfg, globalCfg, verEntCfg,
                revisionInfoRelationMapping);
        DOMWriter writer = new DOMWriter();

        // Sorting the persistent class topologically - superclass always before subclass
        Iterator<PersistentClass> classes = GraphTopologicalSort.sort(new PersistentClassGraphDefiner(cfg)).iterator();

        Map<PersistentClass, PersistentClassVersioningData> pcDatas =
                new HashMap<PersistentClass, PersistentClassVersioningData>();
        Map<PersistentClass, EntityXmlMappingData> xmlMappings = new HashMap<PersistentClass, EntityXmlMappingData>();

        // First pass
        while (classes.hasNext()) {
            PersistentClass pc = classes.next();
            // Collecting information from annotations on the persistent class pc
            AnnotationsMetadataReader annotationsMetadataReader =
                    new AnnotationsMetadataReader(globalCfg, reflectionManager, pc);
            PersistentClassVersioningData versioningData = annotationsMetadataReader.getVersioningData();

            if (versioningData.isVersioned()) {
                pcDatas.put(pc, versioningData);

                if (!StringTools.isEmpty(versioningData.versionsTable.value())) {
                    verEntCfg.addCustomVersionsTableName(pc.getEntityName(), versioningData.versionsTable.value());
                }

                EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
                versionsMetaGen.generateFirstPass(pc, versioningData, xmlMappingData);
                xmlMappings.put(pc, xmlMappingData);
            }
        }

        // Second pass
        for (Map.Entry<PersistentClass, PersistentClassVersioningData> pcDatasEntry : pcDatas.entrySet()) {
            EntityXmlMappingData xmlMappingData = xmlMappings.get(pcDatasEntry.getKey());

            versionsMetaGen.generateSecondPass(pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData);

            try {
                cfg.addDocument(writer.write(xmlMappingData.getMainXmlMapping()));
                // TODO
                //writeDocument(xmlMappingData.getMainXmlMapping());

                for (Document additionalMapping : xmlMappingData.getAdditionalXmlMappings()) {
                    cfg.addDocument(writer.write(additionalMapping));
                    // TODO
                    //writeDocument(additionalMapping);
                }
            } catch (DocumentException e) {
                throw new MappingException(e);
            }
        }

        // Only if there are any versioned classes
        if (pcDatas.size() > 0) {
            try {
                if (revisionInfoXmlMapping !=  null) {
                    // TODO
                    //writeDocument(revisionInfoXmlMapping);
                    cfg.addDocument(writer.write(revisionInfoXmlMapping));
                }
            } catch (DocumentException e) {
                throw new MappingException(e);
            }
        }

        return new EntitiesConfigurations(versionsMetaGen.getEntitiesConfigurations());
    }

    // todo
    private void writeDocument(Document e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer w = new PrintWriter(baos);

        try {
            XMLWriter xw = new XMLWriter(w, new OutputFormat(" ", true));
            xw.write(e);
            w.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        System.out.println("-----------");
        System.out.println(baos.toString());
        System.out.println("-----------");
    }
}
