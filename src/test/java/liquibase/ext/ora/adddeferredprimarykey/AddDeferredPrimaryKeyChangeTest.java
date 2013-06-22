package liquibase.ext.ora.adddeferredprimarykey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.changelog.ChangeLogIterator;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.ext.ora.test.BaseTest;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

import org.junit.Before;
import org.junit.Test;

public class AddDeferredPrimaryKeyChangeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        changeLogFile = "liquibase/ext/ora/adddeferredprimarykey/changelog.test.xml";
        connectToDB();
        cleanDB();
    }

    @Test
    public void getChangeMetaData() {
        AddDeferredPrimaryKeyChange addPrimaryKeyChange = new AddDeferredPrimaryKeyChange();

        assertEquals("addDeferredPrimaryKey", addPrimaryKeyChange.getChangeMetaData().getName());
        assertEquals("Add Deferred Primary Key", addPrimaryKeyChange.getChangeMetaData().getDescription());
        assertEquals(ChangeMetaData.PRIORITY_DEFAULT, addPrimaryKeyChange.getChangeMetaData().getPriority());
    }

    @Test
    public void getConfirmationMessage() throws Exception {
        AddDeferredPrimaryKeyChange change = new AddDeferredPrimaryKeyChange();
        change.setTableName("TABLE_NAME");
        change.setColumnNames("COL_HERE");

        assertEquals("Deferred primary key added to TABLE_NAME (COL_HERE)", change.getConfirmationMessage());
    }

    @Test
    public void generateStatement() throws Exception {

        AddDeferredPrimaryKeyChange change = new AddDeferredPrimaryKeyChange();
        change.setSchemaName("SCHEMA_NAME");
        change.setTableName("TABLE_NAME");
        change.setColumnNames("COL_HERE");
        change.setConstraintName("PK_NAME");

        change.setDeferrable(true);
        change.setInitiallyDeferred(true);

        Database database = new OracleDatabase();
        SqlStatement[] sqlStatements = change.generateStatements(database);

        assertEquals(1, sqlStatements.length);
        assertTrue(sqlStatements[0] instanceof AddDeferredPrimaryKeyStatement);

        assertEquals("SCHEMA_NAME", ((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getSchemaName());
        assertEquals("TABLE_NAME", ((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getTableName());
        assertEquals("COL_HERE", ((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getColumnNames());
        assertEquals("PK_NAME", ((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getConstraintName());

        assertTrue(((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getDeferrable());
        assertTrue(((AddDeferredPrimaryKeyStatement) sqlStatements[0]).getInitiallyDeferred());
    }

    @Test
    public void parseAndGenerate() throws Exception {
        Database database = liquiBase.getDatabase();
        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();


        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance().getParser(changeLogFile, resourceAccessor).parse(changeLogFile,
                changeLogParameters, resourceAccessor);

        database.checkDatabaseChangeLogTable(false, changeLog, null);
        changeLog.validate(database);


        List<ChangeSet> changeSets = changeLog.getChangeSets();

        List<String> expectedQuery = new ArrayList<String>();

        expectedQuery.add("CREATE TABLE AddDeferredPrimaryKeyTest (id INT, name varchar2(50))");
        expectedQuery
                .add("ALTER TABLE AddDeferredPrimaryKeyTest ADD CONSTRAINT PK_AddDeferredPrimaryKeyTest PRIMARY KEY (id) DEFERRABLE INITIALLY DEFERRED");

        int i = 0;

        for (ChangeSet changeSet : changeSets) {
            for (Change change : changeSet.getChanges()) {
                Sql[] sql = SqlGeneratorFactory.getInstance().generateSql(change.generateStatements(database)[0], database);

                assertEquals(expectedQuery.get(i), sql[0].toSql());
            }
            i++;
        }
    }

    @Test
    public void test() throws Exception {

        liquiBase.update(null);

    }
}