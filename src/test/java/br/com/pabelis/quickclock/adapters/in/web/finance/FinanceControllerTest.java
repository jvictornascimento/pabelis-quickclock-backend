package br.com.pabelis.quickclock.adapters.in.web.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @Test void validatesMoneyMethodMonthAndCardDays() throws Exception {
        mvc.perform(get("/api/finance").param("month","not-a-month")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/finance")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Visa\",\"closingDay\":32,\"dueDay\":1}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Bad\",\"date\":\"2026-01-01\",\"amountCents\":-1,\"paymentMethod\":\"PIX\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Bad\",\"date\":\"2026-01-01\",\"amountCents\":1,\"paymentMethod\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void rejectsFractionalCentsInsteadOfSilentlyTruncatingThem() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Invalid cents\",\"date\":\"2026-01-01\",\"amountCents\":1.9,\"paymentMethod\":\"PIX\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void rejectsMissingCentsAndFractionalCentsInExistingEndpoints() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Missing cents\",\"date\":\"2026-01-01\",\"paymentMethod\":\"PIX\"}"))
            .andExpect(status().isBadRequest());
        jdbc.update("INSERT INTO company(name,active,created_at,updated_at) VALUES ('Strict cents',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        long company = jdbc.queryForObject("SELECT MAX(id) FROM company",Long.class);
        mvc.perform(post("/api/companies/{company}/additional-services",company).contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Fractional cents\",\"date\":\"2026-01-01\",\"amountCents\":1.9}"))
            .andExpect(status().isBadRequest());
    }

    @Test void persistsExpenseDefaultsPaysAndExposesActualPaymentDetails() throws Exception {
        String json = mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Lunch\",\"date\":\"2026-02-02\",\"amountCents\":1250,\"paymentMethod\":\"PIX\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$[0].installmentTotal").value(1))
            .andExpect(jsonPath("$[0].paid").value(false)).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(json).get(0).get("id").asLong();
        mvc.perform(post("/api/expenses/{id}/pay",id).contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-01-31\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.paidAt").value("2026-01-31"));
        mvc.perform(get("/api/finance").param("month","2026-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.month").value("2026-01"))
            .andExpect(jsonPath("$.expenseCents").value(1250)).andExpect(jsonPath("$.expenses").isEmpty())
            .andExpect(jsonPath("$.paidExpenses[0].id").value(id));
        mvc.perform(delete("/api/expenses/{id}",id)).andExpect(status().isBadRequest());
    }

    @Test void incomeUsesServerSourceAmountAndIncludesCompanyNameAndSimpleReverse() throws Exception {
        jdbc.update("INSERT INTO company(name,active,created_at,updated_at) VALUES ('Finance API company',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        long company = jdbc.queryForObject("SELECT MAX(id) FROM company",Long.class);
        jdbc.update("INSERT INTO additional_service(company_id,service_date,description,amount_cents,created_at,updated_at) VALUES (?,DATE '2026-01-01','Repair',500,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",company);
        long source = jdbc.queryForObject("SELECT MAX(id) FROM additional_service",Long.class);
        String body = "{\"sourceType\":\"ADDITIONAL_SERVICE\",\"sourceId\":" + source + ",\"incomeDate\":\"2026-01-02\",\"amountCents\":1}";
        String response = mvc.perform(post("/api/companies/{companyId}/incomes",company).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.amountCents").value(500))
            .andExpect(jsonPath("$.companyName").value("Finance API company")).andReturn().getResponse().getContentAsString();
        long income = mapper.readTree(response).get("id").asLong();
        mvc.perform(post("/api/companies/{companyId}/incomes",company).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(income));
        mvc.perform(delete("/api/companies/{companyId}/incomes/{incomeId}",company,income)).andExpect(status().isNoContent());
        mvc.perform(get("/api/finance").param("month","2026-01"))
            .andExpect(jsonPath("$.pendingIncome[0].companyName").value("Finance API company"))
            .andExpect(jsonPath("$.expectedIncomeCents").value(500));
    }

    @Test void cardsRoundTripAndEditingOneInstallmentReturnsOnlyThatInstallment() throws Exception {
        String card = mvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Visa\",\"closingDay\":20,\"dueDay\":5}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long cardId = mapper.readTree(card).get("id").asLong();
        mvc.perform(put("/api/cards/{id}",cardId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Mastercard\",\"closingDay\":20,\"dueDay\":5}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mastercard"));
        mvc.perform(get("/api/cards")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(cardId));
        String expenses = mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Tools\",\"date\":\"2026-01-10\",\"amountCents\":100,\"paymentMethod\":\"CREDIT_CARD\",\"cardId\":" + cardId + ",\"installmentTotal\":3}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$[0].amountCents").value(34))
            .andExpect(jsonPath("$[0].date").value("2026-02-05")).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(expenses).get(0).get("id").asLong();
        mvc.perform(put("/api/expenses/{id}",id).param("scope","ONE").contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\":\"Changed\",\"amountCents\":50}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/expenses/{id}",id)).andExpect(status().isNoContent());
    }
}
