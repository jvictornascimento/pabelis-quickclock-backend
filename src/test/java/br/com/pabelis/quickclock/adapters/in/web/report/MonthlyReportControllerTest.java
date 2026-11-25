package br.com.pabelis.quickclock.adapters.in.web.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MonthlyReportControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aggregatesOnlyMarkedDaysServicesAndEstimatesApprovedWithinMonth() throws Exception {
        long company = company();
        long other = company();
        day(company, "2026-12-01", true, false);
        day(company, "2026-12-31", true, true);
        day(company, "2026-12-02", false, false);
        day(company, "2027-01-01", true, true);
        day(other, "2026-12-01", true, true);
        service(company, "2026-12-01", 101);
        service(company, "2026-12-31", 202);
        service(company, "2027-01-01", 400);
        service(other, "2026-12-01", 800);
        estimate(company, "APPROVED", "2026-12-01 00:00:00", 303);
        estimate(company, "APPROVED", "2026-12-31 23:59:59.999999", 404);
        estimate(company, "APPROVED", "2027-01-01 00:00:00", 900);
        estimate(company, "APPROVED", "2026-11-30 23:59:59", 900);
        estimate(company, "DRAFT", null, 900);
        estimate(company, "CANCELED", "2026-12-15 00:00:00", 900);
        estimate(other, "APPROVED", "2026-12-01 00:00:00", 900);
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2026-12"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(company))
            .andExpect(jsonPath("$.companyName").value("Report company"))
            .andExpect(jsonPath("$.month").value("2026-12"))
            .andExpect(jsonPath("$.workDays", hasSize(2)))
            .andExpect(jsonPath("$.workDays[0].id").isNumber())
            .andExpect(jsonPath("$.additionalServices", hasSize(2)))
            .andExpect(jsonPath("$.additionalServices[0].id").isNumber())
            .andExpect(jsonPath("$.approvedEstimates", hasSize(2)))
            .andExpect(jsonPath("$.approvedEstimates[0].id").isNumber())
            .andExpect(jsonPath("$.workedDays").value(2))
            .andExpect(jsonPath("$.workedPeriods").value(3))
            .andExpect(jsonPath("$.workDaysValueCents").value(0))
            .andExpect(jsonPath("$.additionalServicesValueCents").value(303))
            .andExpect(jsonPath("$.approvedEstimatesValueCents").value(707))
            .andExpect(jsonPath("$.totalValueCents").value(1010));
    }

    @Test
    void returnsEmptyReportAndValidatesMonthAndCompany() throws Exception {
        long company = company();
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2027-02"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalValueCents").value(0))
            .andExpect(jsonPath("$.receivedValueCents").value(0))
            .andExpect(jsonPath("$.pendingValueCents").value(0))
            .andExpect(jsonPath("$.workDays", hasSize(0)))
            .andExpect(jsonPath("$.additionalServices", hasSize(0)))
            .andExpect(jsonPath("$.approvedEstimates", hasSize(0)));
        mvc.perform(get("/api/companies/{id}/reports", company)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2026-13"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/companies/{id}/reports", Long.MAX_VALUE).param("month", "2026-12"))
            .andExpect(status().isNotFound());
    }

    @Test
    void usesHistoricalSnapshotsRatherThanCurrentCompanyPrice() throws Exception {
        long company = company();
        day(company, "2026-10-20", true, false);
        jdbc.update("UPDATE work_day SET before_lunch_value_cents=10000, after_lunch_value_cents=99000 WHERE company_id=?", company);
        jdbc.update("UPDATE company_settings SET half_day_value_cents=77700 WHERE company_id=?", company);
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2026-10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.workDaysValueCents").value(10000))
            .andExpect(jsonPath("$.workDays[0].amountCents").value(10000))
            .andExpect(jsonPath("$.workedPeriods").value(1));
    }

    @Test
    void rejectsOverflowInsteadOfWrappingMoney() throws Exception {
        long company = company();
        service(company, "2026-12-01", Long.MAX_VALUE);
        estimate(company, "APPROVED", "2026-12-01 00:00:00", 1);
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2026-12"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void countsReceiptsForReportOriginsAcrossMonthsAndRestoresPendingAfterReversal() throws Exception {
        long company = company();
        long other = company();
        day(company, "2026-10-01", true, true);
        jdbc.update("UPDATE work_day SET before_lunch_value_cents=1500, after_lunch_value_cents=500 WHERE company_id=?", company);
        service(company, "2026-10-01", 3000);
        estimate(company, "APPROVED", "2026-10-01 00:00:00", 4000);
        long dayId = sourceId("work_day", company);
        long serviceId = sourceId("additional_service", company);
        long estimateId = sourceId("estimate", company);
        assertPaymentTotals(company, 9000, 0, 9000);

        long dayIncome = receive(company, "WORK_REPORT", dayId, "2026-11-01");
        assertPaymentTotals(company, 9000, 2000, 7000);
        org.junit.jupiter.api.Assertions.assertEquals(dayIncome,
            receive(company, "WORK_REPORT", dayId, "2026-11-01"));
        long serviceIncome = receive(company, "ADDITIONAL_SERVICE", serviceId, "2026-11-02");
        receive(company, "APPROVED_ESTIMATE", estimateId, "2026-11-03");

        service(other, "2026-10-01", 8000);
        receive(other, "ADDITIONAL_SERVICE", sourceId("additional_service", other), "2026-11-01");
        service(company, "2026-11-01", 16000);
        long outsideMonth = jdbc.queryForObject("SELECT id FROM additional_service WHERE company_id=? AND service_date='2026-11-01'", Long.class, company);
        receive(company, "ADDITIONAL_SERVICE", outsideMonth, "2026-11-03");
        assertPaymentTotals(company, 9000, 9000, 0);

        mvc.perform(delete("/api/companies/{companyId}/incomes/{incomeId}", company, serviceIncome))
            .andExpect(status().isNoContent());
        assertPaymentTotals(company, 9000, 6000, 3000);
    }

    private void assertPaymentTotals(long company, long gross, long received, long pending) throws Exception {
        mvc.perform(get("/api/companies/{id}/reports", company).param("month", "2026-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalValueCents").value(gross))
            .andExpect(jsonPath("$.receivedValueCents").value(received))
            .andExpect(jsonPath("$.pendingValueCents").value(pending));
    }

    private long sourceId(String table, long company) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE company_id=?", Long.class, company);
    }

    private long receive(long company, String type, long sourceId, String date) throws Exception {
        String response = mvc.perform(post("/api/companies/{companyId}/incomes", company)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceType\":\"%s\",\"sourceId\":%d,\"incomeDate\":\"%s\"}".formatted(type, sourceId, date)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    long company() throws Exception {
        return json.readTree(mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Report company\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    void day(long company, String date, boolean before, boolean after) {
        jdbc.update("INSERT INTO work_day(company_id,work_date,worked_before_lunch,worked_after_lunch,before_lunch_value_cents,after_lunch_value_cents,created_at,updated_at) VALUES (?,?,?,?,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", company,date,before,after);
    }
    void service(long company, String date, long amount) {
        jdbc.update("INSERT INTO additional_service(company_id,service_date,description,amount_cents,created_at,updated_at) VALUES (?,?,'Service',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", company,date,amount);
    }
    void estimate(long company, String status, String approvedAt, long amount) {
        jdbc.update("INSERT INTO estimate(company_id,estimate_date,description,amount_cents,status,approved_at,created_at,updated_at) VALUES (?,'2020-01-01','Proposal',?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", company,amount,status,approvedAt);
    }
}
