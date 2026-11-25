package br.com.pabelis.quickclock.adapters.out.persistence.workday;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "WORKDAY_TEST_DATABASE_URL", matches = ".+")
class WorkDayMigrationTest {
    @Test
    void migratesLegacyValuesAndEnforcesConstraintsOnPostgres() throws Exception {
        String url = System.getenv("WORKDAY_TEST_DATABASE_URL");
        String user = System.getenv("WORKDAY_TEST_DATABASE_USER");
        String password = System.getenv("WORKDAY_TEST_DATABASE_PASSWORD");
        String schema = "workday_" + UUID.randomUUID().toString().replace("-", "");
        var base = Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema);
        base.target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            connection.setSchema(schema);
            statement.executeUpdate("""
                INSERT INTO company(id,name,active,created_at,updated_at)
                VALUES(1,'Legacy',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                      (2,'Missing settings',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
            statement.executeUpdate("""
                INSERT INTO company_settings(company_id,half_day_value_cents,updated_at)
                VALUES(1,12345,CURRENT_TIMESTAMP)
                """);
            statement.executeUpdate("""
                INSERT INTO work_day(company_id,work_date,worked_before_lunch,worked_after_lunch,created_at,updated_at)
                VALUES(1,DATE '2026-11-23',true,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                      (1,DATE '2026-11-24',false,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                      (2,DATE '2026-11-24',true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
            Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                .target("2").load().migrate();
            try (var result = statement.executeQuery("SELECT before_lunch_value_cents,after_lunch_value_cents FROM work_day ORDER BY company_id,work_date")) {
                assertTrue(result.next());
                assertEquals(12345L, result.getObject(1));
                assertNull(result.getObject(2));
                assertTrue(result.next());
                assertNull(result.getObject(1));
                assertEquals(12345L, result.getObject(2));
                assertTrue(result.next());
                assertEquals(0L, result.getObject(1));
                assertEquals(0L, result.getObject(2));
            }
            assertEquals("23514", assertThrows(SQLException.class,
                () -> statement.executeUpdate("UPDATE work_day SET before_lunch_value_cents=-1")).getSQLState());
            assertEquals("23514", assertThrows(SQLException.class,
                () -> statement.executeUpdate("UPDATE work_day SET after_lunch_value_cents=-1")).getSQLState());
            assertEquals("23514", assertThrows(SQLException.class,
                () -> statement.executeUpdate("UPDATE work_day SET before_lunch_value_cents=NULL WHERE worked_before_lunch")).getSQLState());
            assertEquals("23514", assertThrows(SQLException.class,
                () -> statement.executeUpdate("UPDATE work_day SET after_lunch_value_cents=NULL WHERE worked_after_lunch")).getSQLState());
        } finally {
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
