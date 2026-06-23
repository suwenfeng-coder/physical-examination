package com.hospital.examination;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.examination.repository.CheckupPackageRepository;
import com.hospital.examination.repository.PatientRepository;
import com.hospital.examination.repository.UserAccountRepository;
import com.hospital.examination.service.AppointmentService;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class PhysicalExaminationApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CheckupPackageRepository checkupPackageRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void publicLoginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void managementPagesRenderAfterLogin() throws Exception {
        String[] paths = {
                "/", "/patients", "/patients/new", "/doctors", "/doctors/new",
                "/items", "/items/new", "/departments", "/departments/new",
                "/packages", "/packages/new", "/package-templates", "/package-templates/new",
                "/orders", "/orders/new",
                "/appointments", "/appointments/new/personal", "/appointments/new/organization",
                "/appointments/organization/template"
        };
        for (String path : paths) {
            mockMvc.perform(get(path).sessionAttr("LOGIN_USER", "admin"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void userCanRegisterAndLoginWithPasswordOrSms() throws Exception {
        String phone = "13912345678";
        String password = "Health2026";

        String registerSmsBody = mockMvc.perform(post("/auth/sms/send")
                        .param("phone", phone)
                        .param("purpose", "REGISTER"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode registerSms = objectMapper.readTree(registerSmsBody);
        String registerCode = registerSms.get("devCode").asText();

        mockMvc.perform(post("/register")
                        .param("username", "health_user")
                        .param("phone", phone)
                        .param("verificationCode", registerCode)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        var savedUser = userAccountRepository.findByPhone(phone).orElseThrow();
        assertNotEquals(password, savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));

        mockMvc.perform(post("/login")
                        .param("mode", "password")
                        .param("identifier", phone)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        String loginSmsBody = mockMvc.perform(post("/auth/sms/send")
                        .param("phone", phone)
                        .param("purpose", "LOGIN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String loginCode = objectMapper.readTree(loginSmsBody).get("devCode").asText();

        mockMvc.perform(post("/login")
                        .param("mode", "sms")
                        .param("phone", phone)
                        .param("verificationCode", loginCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void organizationAppointmentCanImportParticipantsFromTemplate() throws Exception {
        var checkupPackage = checkupPackageRepository.findByEnabledTrueOrderByIdDesc().get(0);
        var appointment = appointmentService.createOrganization(
                "测试科技有限公司", "91310000TEST2026", "张联系人", "13812345678",
                "上海市测试路1号", checkupPackage.getId(), null, LocalDate.now().plusDays(3),
                "集体预约测试", null);

        byte[] template = appointmentService.createImportTemplate();
        byte[] filledTemplate;
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
             var output = new ByteArrayOutputStream()) {
            var row = workbook.getSheetAt(0).getRow(1);
            row.getCell(0).setCellValue("导入测试人员");
            row.getCell(1).setCellValue("男");
            row.getCell(2).setCellValue("1990-05-20");
            row.getCell(3).setCellValue("13712345678");
            row.getCell(4).setCellValue("310101199005201234");
            row.getCell(5).setCellValue("研发部");
            row.getCell(6).setCellValue("E2026001");
            workbook.write(output);
            filledTemplate = output.toByteArray();
        }

        int imported = appointmentService.importParticipants(appointment.getId(),
                new MockMultipartFile("file", "participants.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        filledTemplate));

        assertEquals(1, imported);
        var saved = appointmentService.get(appointment.getId());
        assertEquals(1, saved.getParticipants().size());
        assertEquals("研发部", saved.getParticipants().get(0).getDepartment());
        assertTrue(patientRepository.findByIdCard("310101199005201234").isPresent());
    }
}
