package br.com.pabelis.quickclock.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsInitialTables() {
        List<String> tables = jdbcTemplate.queryForList("""
            SELECT LOWER(table_name)
            FROM information_schema.tables
            WHERE LOWER(table_schema) = 'public'
            """, String.class);

        assertThat(tables)
            .contains(
                "company",
                "company_settings",
                "work_day",
                "additional_service",
                "estimate",
                "estimate_history",
                "financial_income",
                "expense"
            );
    }

    @Test
    void storesMoneyValuesInCentsColumns() {
        List<String> moneyColumns = jdbcTemplate.queryForList("""
            SELECT LOWER(column_name)
            FROM information_schema.columns
            WHERE LOWER(table_schema) = 'public'
              AND LOWER(column_name) LIKE '%_cents'
            """, String.class);

        assertThat(moneyColumns)
            .contains(
                "half_day_value_cents",
                "amount_cents"
            );
    }

    @Test
    void createsCompanyScopedIndexesForMonthlyQueries() {
        List<String> indexes = jdbcTemplate.queryForList("""
            SELECT LOWER(index_name)
            FROM information_schema.indexes
            WHERE LOWER(table_schema) = 'public'
            """, String.class);

        assertThat(indexes)
            .contains(
                "idx_work_day_company_month",
                "idx_additional_service_company_month",
                "idx_estimate_company_approved_month",
                "idx_financial_income_company_month"
            );
    }
}
