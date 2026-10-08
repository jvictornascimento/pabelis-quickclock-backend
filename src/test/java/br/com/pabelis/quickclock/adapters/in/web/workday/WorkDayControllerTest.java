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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void savesCurrentWorkDay() throws Exception {
        long companyId = createCompany("Work Day Company");
        LocalDate today = LocalDate.now();

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
        LocalDate today = LocalDate.now();

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
        LocalDate today = LocalDate.now();

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
        LocalDate today = LocalDate.now();
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
        LocalDate today = LocalDate.now();

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
