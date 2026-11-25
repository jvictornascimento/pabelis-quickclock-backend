package br.com.pabelis.quickclock.adapters.in.web.workday;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(WorkDayControllerTest.FixedTime.class)
class WorkDayControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 25);

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-02-25T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void requiresReversalBeforeChangingReceivedWorkDay() throws Exception {
        long id = createCompany("Received work");
        saveWorkDay(id, TODAY, true, false);
        Long workDayId = jdbc.queryForObject("SELECT id FROM work_day WHERE company_id=?", Long.class, id);
        jdbc.update("""
            INSERT INTO financial_income(company_id,source_type,source_id,income_date,description,amount_cents,paid_at,created_at)
            VALUES(?,'WORK_REPORT',?,?,'Received work',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            """, id, workDayId, TODAY);
        saveWorkDay(id, TODAY, true, false);
        mockMvc.perform(put("/api/companies/{id}/work-days/{date}", id, TODAY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workedBeforeLunch\":true,\"workedAfterLunch\":true}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/before-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/after-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
        jdbc.update("DELETE FROM financial_income WHERE source_type='WORK_REPORT' AND source_id=?", workDayId);
        saveWorkDay(id, TODAY, true, true);
    }

    @Test
    void savesCurrentWorkDay() throws Exception {
        long companyId = createCompany("Work Day Company");
        LocalDate today = TODAY;

        mockMvc.perform(put("/api/companies/{companyId}/work-days/{date}", companyId, today)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "workedBeforeLunch": true,
                      "workedAfterLunch": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyId", is((int) companyId)))
            .andExpect(jsonPath("$.date", is(today.toString())))
            .andExpect(jsonPath("$.workedBeforeLunch", is(true)))
            .andExpect(jsonPath("$.workedAfterLunch", is(false)));
    }

    @Test
    void togglesCurrentWorkDayPeriods() throws Exception {
        long companyId = createCompany("Toggle Company");
        LocalDate today = TODAY;

        mockMvc.perform(patch("/api/companies/{companyId}/work-days/{date}/before-lunch/toggle", companyId, today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workedBeforeLunch", is(true)))
            .andExpect(jsonPath("$.workedAfterLunch", is(false)));

        mockMvc.perform(patch("/api/companies/{companyId}/work-days/{date}/after-lunch/toggle", companyId, today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workedBeforeLunch", is(true)))
            .andExpect(jsonPath("$.workedAfterLunch", is(true)));
    }

    @Test
    void blocksOldAndFutureWorkDayEditing() throws Exception {
        long companyId = createCompany("Policy Company");
        LocalDate today = TODAY;

        mockMvc.perform(put("/api/companies/{companyId}/work-days/{date}", companyId, today.minusDays(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "workedBeforeLunch": true,
                      "workedAfterLunch": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Old work days are read only.")));

        mockMvc.perform(put("/api/companies/{companyId}/work-days/{date}", companyId, today.plusDays(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "workedBeforeLunch": true,
                      "workedAfterLunch": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Future work days cannot be edited.")));
    }

    @Test
    void searchesWorkDaysByDateAndMonth() throws Exception {
        long companyId = createCompany("Search Company");
        LocalDate today = TODAY;
        String month = YearMonth.from(today).toString();

        saveWorkDay(companyId, today, true, false);

        mockMvc.perform(get("/api/companies/{companyId}/work-days", companyId)
                .param("date", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].date", is(today.toString())));

        mockMvc.perform(get("/api/companies/{companyId}/work-days", companyId)
                .param("month", month))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].workedBeforeLunch", is(true)));
    }

    @Test
    void keepsSameDateSeparatedByCompany() throws Exception {
        long firstCompanyId = createCompany("First Company");
        long secondCompanyId = createCompany("Second Company");
        LocalDate today = TODAY;

        saveWorkDay(firstCompanyId, today, true, false);
        saveWorkDay(secondCompanyId, today, false, true);

        mockMvc.perform(get("/api/companies/{companyId}/work-days", firstCompanyId)
                .param("date", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workedBeforeLunch", is(true)))
            .andExpect(jsonPath("$[0].workedAfterLunch", is(false)));

        mockMvc.perform(get("/api/companies/{companyId}/work-days", secondCompanyId)
                .param("date", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workedBeforeLunch", is(false)))
            .andExpect(jsonPath("$[0].workedAfterLunch", is(true)));
    }

    @Test
    void rejectsAmbiguousSearchFilters() throws Exception {
        long id = createCompany("Ambiguous search");
        mockMvc.perform(get("/api/companies/{id}/work-days", id)
                .param("date", TODAY.toString()).param("month", YearMonth.from(TODAY).toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void defaultsSearchMonthToConfiguredClock() throws Exception {
        long id = createCompany("Clock search");
        saveWorkDay(id, TODAY, true, false);
        mockMvc.perform(get("/api/companies/{id}/work-days", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].date", is(TODAY.toString())));
    }

    @Test
    void blocksInactiveCompanyWritesButAllowsHistoricalReads() throws Exception {
        long id = createCompany("Inactive history");
        saveWorkDay(id, TODAY, true, false);
        mockMvc.perform(delete("/api/companies/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(put("/api/companies/{id}/work-days/{date}", id, TODAY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workedBeforeLunch\":false,\"workedAfterLunch\":false}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/before-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/after-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/companies/{id}/work-days/{date}", id, TODAY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workedBeforeLunch", is(true)))
            .andExpect(jsonPath("$.beforeLunchValueCents", is(0)));
    }

    @Test
    void snapshotsEachPeriodOnceAndKeepsPriceAfterUnmarking() throws Exception {
        long id = createCompany("Prices");
        saveWorkDay(id, TODAY, false, false);
        mockMvc.perform(get("/api/companies/{id}/work-days/{date}", id, TODAY))
            .andExpect(jsonPath("$.beforeLunchValueCents").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.afterLunchValueCents").value(org.hamcrest.Matchers.nullValue()));
        settings(id, 10000, true);
        saveWorkDay(id, TODAY, true, false);
        settings(id, 12000, true);
        saveWorkDay(id, TODAY, true, true);
        saveWorkDay(id, TODAY, false, true);
        saveWorkDay(id, TODAY, true, true);
        saveWorkDay(id, TODAY, true, true);
        mockMvc.perform(get("/api/companies/{id}/work-days/{date}", id, TODAY))
            .andExpect(jsonPath("$.beforeLunchValueCents", is(10000)))
            .andExpect(jsonPath("$.afterLunchValueCents", is(12000)));
        mockMvc.perform(get("/api/companies/{id}/work-days", id).param("date", TODAY.toString()))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsDisabledWeekdayForSaveAndBothToggles() throws Exception {
        long id = createCompany("No Wednesdays");
        settings(id, 10000, false);
        mockMvc.perform(put("/api/companies/{id}/work-days/{date}", id, TODAY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workedBeforeLunch\":true,\"workedAfterLunch\":false}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/before-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/companies/{id}/work-days/{date}/after-lunch/toggle", id, TODAY))
            .andExpect(status().isBadRequest());
    }

    private void settings(long id, long price, boolean wednesday) throws Exception {
        mockMvc.perform(put("/api/companies/{id}/settings", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"halfDayValueCents":%d,"weekdays":{"mondayActive":true,"tuesdayActive":true,
                    "wednesdayActive":%s,"thursdayActive":true,"fridayActive":true,
                    "saturdayActive":false,"sundayActive":false}}
                    """.formatted(price, wednesday)))
            .andExpect(status().isOk());
    }

    private long createCompany(String name) throws Exception {
        String response = mockMvc.perform(post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private void saveWorkDay(
        long companyId,
        LocalDate date,
        boolean workedBeforeLunch,
        boolean workedAfterLunch
    ) throws Exception {
        mockMvc.perform(put("/api/companies/{companyId}/work-days/{date}", companyId, date)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "workedBeforeLunch": %s,
                      "workedAfterLunch": %s
                    }
                    """.formatted(workedBeforeLunch, workedAfterLunch)))
            .andExpect(status().isOk());
    }
}
