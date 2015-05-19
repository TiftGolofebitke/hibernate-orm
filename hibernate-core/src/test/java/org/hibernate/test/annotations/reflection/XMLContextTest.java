/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.reflection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotSupportedException;

import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.internal.util.xml.ErrorLogger;
import org.hibernate.internal.util.xml.XMLHelper;

/**
 * @author Emmanuel Bernard
 */
public class XMLContextTest {
	@Test
	public void testAll() throws Exception {
		XMLHelper xmlHelper = new XMLHelper();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(
				"org/hibernate/test/annotations/reflection/orm.xml"
		);
		Assert.assertNotNull( "ORM.xml not found", is );
		XMLContext context = new XMLContext();
		ErrorLogger errorLogger = new ErrorLogger();
		SAXReader saxReader = xmlHelper.createSAXReader( errorLogger, EJB3DTDEntityResolver.INSTANCE );
		//saxReader.setValidation( false );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
		}
		catch ( SAXNotSupportedException e ) {
			saxReader.setValidation( false );
		}
		org.dom4j.Document doc;
		try {
			doc = saxReader
					.read( new InputSource( new BufferedInputStream( is ) ) );
		}
		finally {
			try {
				is.close();
			}
			catch ( IOException ioe ) {
				//log.warn( "Could not close input stream", ioe );
			}
		}
		Assert.assertFalse( errorLogger.hasErrors() );
		context.addDocument( doc );
	}
}
