/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal.legacy;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.Schema;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * The main difference is that the legacy DatabaseMetadata object looked up table information
 * lazily.
 *
 * @author Steve Ebersole
 *
 * @deprecated Available for the moment as {@link org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl}
 * and {@link org.hibernate.tool.schema.extract.spi.DatabaseInformationBuilder} are still a work in progress.  This
 * class will be removed once that work has been finished.
 */
@Deprecated
public class DatabaseInformationImpl implements DatabaseInformation, ExtractionContext.DatabaseObjectAccess {
	private final InformationExtractor extractor;
	private final ExtractionContext extractionContext;

	private final JdbcEnvironment jdbcEnvironment;

	private final Map<QualifiedSequenceName,SequenceInformation> sequenceInformationMap = new HashMap<QualifiedSequenceName, SequenceInformation>();

	public DatabaseInformationImpl(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			JdbcConnectionAccess jdbcConnectionAccess,
			Identifier defaultCatalogName,
			Identifier defaultSchemaName) throws SQLException {
		this.jdbcEnvironment = jdbcEnvironment;

		this.extractionContext = new ExtractionContextImpl(
				serviceRegistry,
				jdbcEnvironment,
				jdbcConnectionAccess,
				this,
				defaultCatalogName,
				defaultSchemaName
		);

		// todo : make this pluggable
		this.extractor = new InformationExtractorJdbcDatabaseMetaDataImpl( extractionContext );

		// legacy code did initialize sequences...
		initializeSequences();
	}

	private void initializeSequences() throws SQLException {
		Iterable<SequenceInformation> itr = jdbcEnvironment.getDialect().getSequenceInformationExtractor().extractMetadata( extractionContext );
		for ( SequenceInformation sequenceInformation : itr ) {
			sequenceInformationMap.put(
					// for now, follow the legacy behavior of storing just the
					// unqualified sequence name.
					new QualifiedSequenceName(
							null,
							null,
							sequenceInformation.getSequenceName().getSequenceName()
					),
					sequenceInformation
			);
		}
	}

	@Override
	public boolean schemaExists(Schema.Name schema) {
		return extractor.schemaExists( schema.getCatalog(), schema.getSchema() );
	}

	@Override
	public TableInformation getTableInformation(
			Identifier catalogName,
			Identifier schemaName,
			Identifier tableName) {
		return getTableInformation( new QualifiedTableName( catalogName, schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(
			Schema.Name schemaName,
			Identifier tableName) {
		return getTableInformation( new QualifiedTableName( schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(QualifiedTableName qualifiedTableName) {
		if ( qualifiedTableName.getObjectName() == null ) {
			throw new IllegalArgumentException( "Passed table name cannot be null" );
		}

		return extractor.getTable(
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName(),
				qualifiedTableName.getTableName()
		);
	}

	@Override
	public void registerTable(TableInformation tableInformation) {
	}

	@Override
	public SequenceInformation getSequenceInformation(
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName) {
		return getSequenceInformation( new QualifiedSequenceName( catalogName, schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(
			Schema.Name schemaName,
			Identifier sequenceName) {
		return getSequenceInformation( new QualifiedSequenceName( schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(QualifiedSequenceName qualifiedSequenceName) {
		return locateSequenceInformation( qualifiedSequenceName );
	}

	@Override
	public TableInformation locateTableInformation(QualifiedTableName tableName) {
		return getTableInformation( tableName );
	}

	@Override
	public SequenceInformation locateSequenceInformation(QualifiedSequenceName sequenceName) {
		// again, follow legacy behavior
		if ( sequenceName.getCatalogName() != null || sequenceName.getSchemaName() != null ) {
			sequenceName = new QualifiedSequenceName( null, null, sequenceName.getSequenceName() );
		}

		return sequenceInformationMap.get( sequenceName );
	}
}
