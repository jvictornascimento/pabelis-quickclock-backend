package br.com.pabelis.quickclock.adapters.in.web.company;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsCompanyWithDefaultSettings() throws Exception {
        String companyLocation = createCompany("Belis Eletronica");

        mockMvc.perform(get(companyLocation + "/settings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.halfDayValueCents", is(0)))
            .andExpect(jsonPath("$.weekdays.mondayActive", is(true)))
            .andExpect(jsonPath("$.weekdays.fridayActive", is(true)))
            .andExpect(jsonPath("$.weekdays.saturdayActive", is(false)))
            .andExpect(jsonPath("$.weekdays.sundayActive", is(false)));
    }

    @Test
    void listsAndUpdatesCompanies() throws Exception {
        String companyLocation = createCompany("Old Name");

        mockMvc.perform(put(companyLocation)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "New Name"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("New Name")))
            .andExpect(jsonPath("$.active", is(true)));

        mockMvc.perform(get("/api/companies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("New Name")));
    }

    @Test
    void deactivatesCompany() throws Exception {
        String companyLocation = createCompany("Company To Disable");

        mockMvc.perform(delete(companyLocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void updatesCompanySettings() throws Exception {
        String companyLocation = createCompany("Settings Company");

        mockMvc.perform(put(companyLocation + "/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "halfDayValueCents": 8500,
                      "weekdays": {
                        "mondayActive": true,
                        "tuesdayActive": true,
                        "wednesdayActive": true,
                        "thursdayActive": true,
                        "fridayActive": false,
                        "saturdayActive": false,
                        "sundayActive": false
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.halfDayValueCents", is(8500)))
            .andExpect(jsonPath("$.weekdays.fridayActive", is(false)));
    }

    @Test
    void returnsValidationErrorForBlankCompanyName() throws Exception {
        mockMvc.perform(post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": " "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Validation failed.")))
            .andExpect(jsonPath("$.details", hasItem(containsString("name"))));
    }

    @Test
    void returnsNotFoundForUnknownCompany() throws Exception {
        mockMvc.perform(get("/api/companies/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", is("Company not found.")));
    }

    private String createCompany(String name) throws Exception {
        String response = mockMvc.perform(post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is(name)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");
        return "/api/companies/" + id;
    }
}
