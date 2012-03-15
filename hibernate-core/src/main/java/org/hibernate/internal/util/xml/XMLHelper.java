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
package org.hibernate.internal.util.xml;

import java.util.List;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jboss.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.hibernate.internal.CoreMessageLogger;


/**
 * Small helper class that lazy loads DOM and SAX reader and keep them for fast use afterwards.
 */
public final class XMLHelper {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, XMLHelper.class.getName());

	public static final EntityResolver DEFAULT_DTD_RESOLVER = new DTDEntityResolver();

	private DOMReader domReader;
	private SAXReader saxReader;

	/**
	 * @param file the file name of the xml file to parse
	 * @param errorsList a list to which to add all occurring errors
	 * @param entityResolver an xml entity resolver
	 *
	 * @return Create and return a dom4j {@code SAXReader} which will append all validation errors
	 * to the passed error list
	 */
	public SAXReader createSAXReader(String file, List<SAXParseException> errorsList, EntityResolver entityResolver) {
		SAXReader saxReader = resolveSAXReader();
		saxReader.setEntityResolver(entityResolver);
		saxReader.setErrorHandler( new ErrorLogger(file, errorsList) );
		return saxReader;
	}

	private SAXReader resolveSAXReader() {
		if ( saxReader == null ) {
			saxReader = new SAXReader();
			saxReader.setMergeAdjacentText(true);
			saxReader.setValidation(true);
		}
		return saxReader;
	}

	/**
	 * @return create and return a dom4j DOMReader
	 */
	public DOMReader createDOMReader() {
		if (domReader==null) domReader = new DOMReader();
		return domReader;
	}

	public static class ErrorLogger implements ErrorHandler {
		private String file;
		private List<SAXParseException> errors;

		private ErrorLogger(String file, List<SAXParseException> errors) {
			this.file=file;
			this.errors = errors;
		}
		public void error(SAXParseException error) {
            LOG.parsingXmlErrorForFile(file, error.getLineNumber(), error.getMessage());
			errors.add(error);
		}
		public void fatalError(SAXParseException error) {
			error(error);
		}
		public void warning(SAXParseException warn) {
            LOG.parsingXmlWarningForFile(file, warn.getLineNumber(), warn.getMessage());
		}
	}

	public static Element generateDom4jElement(String elementName) {
		return getDocumentFactory().createElement( elementName );
	}

    public static DocumentFactory getDocumentFactory() {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        DocumentFactory factory;
        try {
            Thread.currentThread().setContextClassLoader( XMLHelper.class.getClassLoader() );
            factory = DocumentFactory.getInstance();
        }
        finally {
            Thread.currentThread().setContextClassLoader( cl );
        }
        return factory;
    }

	public static void dump(Element element) {
		try {
			// try to "pretty print" it
			OutputFormat outformat = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter( System.out, outformat );
			writer.write( element );
			writer.flush();
			System.out.println( "" );
		}
		catch( Throwable t ) {
			// otherwise, just dump it
			System.out.println( element.asXML() );
		}

	}
}
