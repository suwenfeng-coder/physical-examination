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
import com.hospital.examination.service.ExamOrderService;
import com.hospital.examination.service.ReportAttachmentService;
import com.hospital.examination.service.ReportPdfService;
import com.hospital.examination.model.Gender;
import com.hospital.examination.model.AppointmentStatus;
import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.model.PackageType;
import com.hospital.examination.model.Patient;
import com.hospital.examination.model.ResultStatus;
import com.hospital.examination.repository.AppointmentRepository;
import com.hospital.examination.repository.DoctorRepository;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;

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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CheckupPackageRepository checkupPackageRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ExamOrderService examOrderService;

    @Autowired
    private ReportAttachmentService reportAttachmentService;

    @Autowired
    private ReportPdfService reportPdfService;

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
                "/orders", "/orders/new", "/reports",
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

    @Test
    void appointmentListFiltersCancelledStatusTogetherWithKeyword() {
        var personalPackage = checkupPackageRepository.findByEnabledTrueOrderByIdDesc().stream()
                .filter(item -> item.getType() == PackageType.PERSONAL)
                .findFirst()
                .orElseThrow();

        Patient cancelledPatient = new Patient();
        cancelledPatient.setName("预约筛选取消人员");
        cancelledPatient.setGender(Gender.MALE);
        cancelledPatient.setBirthday(LocalDate.of(1991, 1, 1));
        cancelledPatient.setPhone("13500000001");
        cancelledPatient = patientRepository.save(cancelledPatient);

        Patient bookedPatient = new Patient();
        bookedPatient.setName("预约筛选未取消人员");
        bookedPatient.setGender(Gender.FEMALE);
        bookedPatient.setBirthday(LocalDate.of(1992, 2, 2));
        bookedPatient.setPhone("13500000002");
        bookedPatient = patientRepository.save(bookedPatient);

        var cancelled = appointmentService.createPersonal(cancelledPatient.getId(), personalPackage.getId(),
                null, LocalDate.now().plusDays(2), null);
        appointmentService.cancel(cancelled.getId());
        appointmentService.createPersonal(bookedPatient.getId(), personalPackage.getId(),
                null, LocalDate.now().plusDays(2), null);

        var results = appointmentRepository.searchWithFilters("预约筛选", null, AppointmentStatus.CANCELLED);

        assertEquals(1, results.size());
        assertEquals(AppointmentStatus.CANCELLED, results.get(0).getStatus());
        var savedCancelled = appointmentService.get(results.get(0).getId());
        assertEquals("预约筛选取消人员", savedCancelled.getParticipants().get(0).getPatient().getName());
    }

    @Test
    void reportCanBeEnteredUploadedReviewedNotifiedAndExportedAsPdf() throws Exception {
        Patient patient = new Patient();
        patient.setName("报告流程测试");
        patient.setGender(Gender.FEMALE);
        patient.setBirthday(LocalDate.of(1992, 6, 15));
        patient.setPhone("13612345678");
        patient.setIdCard("310101199206151234");
        patient = patientRepository.save(patient);

        var checkupPackage = checkupPackageRepository.findByEnabledTrueOrderByIdDesc().get(0);
        var doctor = doctorRepository.findEnabledOrdered().get(0);
        var order = examOrderService.create(patient.getId(), checkupPackage.getId(), doctor.getId(), LocalDate.now());

        var resultIds = order.getResults().stream().map(result -> result.getId()).toList();
        var values = order.getResults().stream()
                .map(result -> "检查所见：未见明显异常。\n这是用于验证大段落结果录入的第二行。")
                .toList();
        var statuses = Collections.nCopies(order.getResults().size(), ResultStatus.NORMAL);
        var remarks = Collections.nCopies(order.getResults().size(), "");
        examOrderService.saveResults(order.getId(), resultIds, values, statuses, remarks);

        reportAttachmentService.store(examOrderService.get(order.getId()),
                new MockMultipartFile("file", "影像报告.png", "image/png", new byte[]{1, 2, 3, 4}));
        examOrderService.submitForReview(order.getId(), "总体健康状况良好", "保持规律作息并定期复查");
        assertEquals(OrderStatus.PENDING_REVIEW, examOrderService.get(order.getId()).getStatus());
        mockMvc.perform(get("/orders/" + order.getId())
                        .sessionAttr("LOGIN_USER", "admin")
                        .sessionAttr("LOGIN_ROLE", "ADMIN"))
                .andExpect(status().isOk());

        examOrderService.approve(order.getId(), doctor.getId(), "审核通过");
        var approved = examOrderService.get(order.getId());
        assertEquals(OrderStatus.COMPLETED, approved.getStatus());
        assertEquals(1, approved.getAttachments().size());
        assertTrue(approved.getPickupNotifiedAt() != null);

        byte[] pdf = reportPdfService.create(approved);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        mockMvc.perform(get("/orders/" + order.getId() + "/report.pdf")
                        .sessionAttr("LOGIN_USER", "admin")
                        .sessionAttr("LOGIN_ROLE", "ADMIN"))
                .andExpect(status().isOk());
    }
}
