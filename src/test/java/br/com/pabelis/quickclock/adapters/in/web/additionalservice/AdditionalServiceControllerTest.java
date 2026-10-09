package br.com.pabelis.quickclock.adapters.in.web.additionalservice;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdditionalServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsAdditionalServicesByMonth() throws Exception {
        long companyId = createCompany("Service Company");
        LocalDate date = LocalDate.now();

        createService(companyId, date, "Board repair", 12000);

        mockMvc.perform(get("/api/companies/{companyId}/additional-services", companyId)
                .param("month", YearMonth.from(date).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].description", is("Board repair")))
            .andExpect(jsonPath("$[0].amountCents", is(12000)));
    }

    @Test
    void updatesAdditionalService() throws Exception {
        long companyId = createCompany("Update Service Company");
        LocalDate date = LocalDate.now();
        long serviceId = createService(companyId, date, "Old service", 1000);

        mockMvc.perform(put("/api/companies/{companyId}/additional-services/{serviceId}", companyId, serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "%s",
                      "description": "New service",
                      "amountCents": 2500
                    }
                    """.formatted(date)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description", is("New service")))
            .andExpect(jsonPath("$.amountCents", is(2500)));
    }

    @Test
    void removesAdditionalService() throws Exception {
        long companyId = createCompany("Delete Service Company");
        LocalDate date = LocalDate.now();
        long serviceId = createService(companyId, date, "Temporary service", 5000);

        mockMvc.perform(delete("/api/companies/{companyId}/additional-services/{serviceId}", companyId, serviceId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/companies/{companyId}/additional-services", companyId)
                .param("month", YearMonth.from(date).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void keepsServicesScopedByCompany() throws Exception {
        long firstCompanyId = createCompany("First Service Company");
        long secondCompanyId = createCompany("Second Service Company");
        LocalDate date = LocalDate.now();

        createService(firstCompanyId, date, "First service", 3000);
        createService(secondCompanyId, date, "Second service", 4000);

        mockMvc.perform(get("/api/companies/{companyId}/additional-services", firstCompanyId)
                .param("month", YearMonth.from(date).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].description", is("First service")));

        mockMvc.perform(get("/api/companies/{companyId}/additional-services", secondCompanyId)
                .param("month", YearMonth.from(date).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].description", is("Second service")));
    }

    @Test
    void returnsNotFoundWhenServiceBelongsToAnotherCompany() throws Exception {
        long ownerCompanyId = createCompany("Owner Service Company");
        long otherCompanyId = createCompany("Other Service Company");
        long serviceId = createService(ownerCompanyId, LocalDate.now(), "Private service", 3000);

        mockMvc.perform(get("/api/companies/{companyId}/additional-services/{serviceId}", otherCompanyId, serviceId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", is("Additional service not found.")));
    }

    @Test
    void returnsValidationErrorForInvalidService() throws Exception {
        long companyId = createCompany("Invalid Service Company");

        mockMvc.perform(post("/api/companies/{companyId}/additional-services", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": null,
                      "description": "",
                      "amountCents": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Validation failed.")))
            .andExpect(jsonPath("$.details", hasItem(containsString("date"))))
            .andExpect(jsonPath("$.details", hasItem(containsString("description"))))
            .andExpect(jsonPath("$.details", hasItem(containsString("amountCents"))));
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

    private long createService(long companyId, LocalDate date, String description, long amountCents) throws Exception {
        String response = mockMvc.perform(post("/api/companies/{companyId}/additional-services", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "%s",
                      "description": "%s",
                      "amountCents": %d
                    }
                    """.formatted(date, description, amountCents)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
