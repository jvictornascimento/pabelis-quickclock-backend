package br.com.pabelis.quickclock.adapters.in.web.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EstimateControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void retainsSnapshotsAndApprovesOnlyOnceWithoutCreatingAdditionalService() throws Exception {
        long company = company();
        long id = estimate(company);
        mvc.perform(put(path(company) + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("Changed proposal", 2500)))
            .andExpect(status().isOk());
        String approval = mvc.perform(post(path(company) + "/" + id + "/approve"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedAt").isNotEmpty()).andReturn().getResponse().getContentAsString();
        mvc.perform(post(path(company) + "/" + id + "/approve"))
            .andExpect(status().isOk()).andExpect(content().json(approval));
        mvc.perform(get(path(company) + "/" + id + "/history"))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].action").value("CREATED"))
            .andExpect(jsonPath("$[0].note").value("CREATED"))
            .andExpect(jsonPath("$[0].description").value("Original proposal"))
            .andExpect(jsonPath("$[0].amountCents").value(1000))
            .andExpect(jsonPath("$[1].action").value("UPDATED"))
            .andExpect(jsonPath("$[1].description").value("Changed proposal"))
            .andExpect(jsonPath("$[2].action").value("APPROVED"));
        mvc.perform(get(path(company))).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].status").value("APPROVED"));
        mvc.perform(put(path(company) + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("Forbidden", 1))).andExpect(status().isBadRequest());
        mvc.perform(delete(path(company) + "/" + id)).andExpect(status().isBadRequest());
        mvc.perform(get(path(company) + "/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Changed proposal"));
        mvc.perform(get("/api/companies/{id}/additional-services", company).param("month", "2026-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cancelsDraftWithoutDeletingRecordOrHistory() throws Exception {
        long company = company();
        long id = estimate(company);
        mvc.perform(delete(path(company) + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete(path(company) + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(path(company) + "/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELED"));
        mvc.perform(post(path(company) + "/" + id + "/approve")).andExpect(status().isBadRequest());
        mvc.perform(put(path(company) + "/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("Forbidden", 1))).andExpect(status().isBadRequest());
        mvc.perform(get(path(company) + "/" + id + "/history")).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[1].action").value("CANCELED"));
    }

    @Test
    void scopesEveryOperationByCompanyAndListsByProposalMonth() throws Exception {
        long owner = company();
        long other = company();
        long id = estimate(owner);
        mvc.perform(get(path(owner)).param("month", "2026-01")).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(path(owner)).param("month", "2026-02")).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(path(other))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(path(other) + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(get(path(other) + "/" + id + "/history")).andExpect(status().isNotFound());
        mvc.perform(post(path(other) + "/" + id + "/approve")).andExpect(status().isNotFound());
        mvc.perform(delete(path(other) + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(put(path(other) + "/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("Forbidden", 1))).andExpect(status().isNotFound());
        mvc.perform(get(path(Long.MAX_VALUE))).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidAndMissingAmountsAndInvalidMonth() throws Exception {
        long company = company();
        for (String invalid : new String[]{"{}", "{\"date\":\"2026-01-01\",\"description\":\"x\"}",
                body(" ", 0), body("x".repeat(256), 0), body("x", -1)}) {
            mvc.perform(post(path(company)).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get(path(company)).param("month", "2026-13")).andExpect(status().isBadRequest());
        mvc.perform(post(path(company)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"date\":\"2026-01-01\",\"description\":\"Fractional cents\",\"amountCents\":1.5}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path(company)).contentType(MediaType.APPLICATION_JSON).content(body("Free", 0)))
            .andExpect(status().isCreated());
    }

    long company() throws Exception {
        return json.readTree(mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Estimate company\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    long estimate(long company) throws Exception {
        return json.readTree(mvc.perform(post(path(company)).contentType(MediaType.APPLICATION_JSON)
            .content(body("Original proposal", 1000))).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    String path(long company) { return "/api/companies/" + company + "/estimates"; }
    String body(String description, long amount) {
        return "{\"date\":\"2026-01-01\",\"description\":\"%s\",\"amountCents\":%d}".formatted(description, amount);
    }
}
