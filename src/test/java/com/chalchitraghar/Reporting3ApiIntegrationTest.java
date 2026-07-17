package com.chalchitraghar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chalchitraghar.modules.users.enums.Role;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class Reporting3ApiIntegrationTest extends AbstractIntegrationTest {
    private static final String START = "2026-07-01";
    private static final String END = "2026-07-03";

    @Test
    void csvAndXlsxExportsAreSafeDeterministicAndAuthorized() throws Exception {
        String admin = tokenFor("report3-export-admin@example.com", Role.ADMIN);
        String customer = tokenFor("report3-export-customer@example.com", Role.CUSTOMER);
        mockMvc.perform(export("/revenue", null, "CSV")).andExpect(status().isUnauthorized());
        mockMvc.perform(export("/revenue", customer, "CSV")).andExpect(status().isForbidden());
        mockMvc.perform(export("/revenue", admin, "CSV").param("currency", "NPR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"revenue-report-2026-07-01-to-2026-07-03.csv\""))
                .andExpect(content().string(containsString("grossRevenue")));
        byte[] xlsx =
                mockMvc.perform(export("/movies", admin, "XLSX"))
                        .andExpect(status().isOk())
                        .andExpect(
                                content()
                                        .contentType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            org.assertj.core.api.Assertions.assertThat(
                            workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("movieId");
        }
    }

    @Test
    void previousAndCustomPeriodComparisonsHandleZeroDenominators() throws Exception {
        String admin = tokenFor("report3-comparison-admin@example.com", Role.ADMIN);
        mockMvc.perform(
                        get("/api/admin/reports/comparisons/periods")
                                .header("Authorization", bearer(admin))
                                .param("currentStartDate", START)
                                .param("currentEndDate", END)
                                .param("currency", "NPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comparisonPeriod.startDate").value("2026-06-28"))
                .andExpect(jsonPath("$.data.totalBookings.comparable").value(true))
                .andExpect(jsonPath("$.data.totalBookings.percentageChange").value(0.00));
        mockMvc.perform(
                        get("/api/admin/reports/comparisons/periods")
                                .header("Authorization", bearer(admin))
                                .param("currentStartDate", START)
                                .param("currentEndDate", END)
                                .param("comparisonMode", "CUSTOM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleCrudManualRunAndIdempotencyUseOneDelivery() throws Exception {
        String admin = tokenFor("report3-schedule-admin@example.com", Role.ADMIN);
        Map<String, Object> request =
                Map.of(
                        "name",
                        "Daily revenue",
                        "reportType",
                        "REVENUE",
                        "scheduleFrequency",
                        "DAILY",
                        "deliveryFormat",
                        "CSV",
                        "recipientEmail",
                        "REPORTS@EXAMPLE.COM",
                        "currency",
                        "npr",
                        "enabled",
                        false);
        String body =
                mockMvc.perform(
                                post("/api/admin/reports/schedules")
                                        .header("Authorization", bearer(admin))
                                        .contentType("application/json")
                                        .content(json(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.recipientEmail").value("reports@example.com"))
                        .andExpect(jsonPath("$.data.currency").value("NPR"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = objectMapper.readTree(body).path("data").path("id").asLong();
        mockMvc.perform(
                        post("/api/admin/reports/schedules/{id}/run", id)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"));
        mockMvc.perform(
                        post("/api/admin/reports/schedules/{id}/run", id)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(reportDeliveryRepository.count()).isEqualTo(1);
        mockMvc.perform(
                        post("/api/admin/reports/schedules/{id}/enable", id)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
        mockMvc.perform(
                        delete("/api/admin/reports/schedules/{id}", id)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(reportDeliveryRepository.count()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder export(
            String endpoint, String token, String format) {
        var request =
                get("/api/admin/reports/exports" + endpoint)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("format", format);
        return token == null ? request : request.header("Authorization", bearer(token));
    }
}
