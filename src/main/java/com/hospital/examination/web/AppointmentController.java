package com.hospital.examination.web;

import com.hospital.examination.model.AppointmentStatus;
import com.hospital.examination.model.AppointmentType;
import com.hospital.examination.repository.AppointmentRepository;
import com.hospital.examination.repository.CheckupPackageRepository;
import com.hospital.examination.repository.DoctorRepository;
import com.hospital.examination.repository.PatientRepository;
import com.hospital.examination.service.AppointmentImportException;
import com.hospital.examination.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final CheckupPackageRepository packageRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentRepository appointmentRepository,
                                 PatientRepository patientRepository,
                                 CheckupPackageRepository packageRepository,
                                 DoctorRepository doctorRepository,
                                 AppointmentService appointmentService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) AppointmentType type,
                       @RequestParam(required = false) AppointmentStatus status,
                       Model model) {
        if (!keyword.isBlank()) {
            model.addAttribute("appointments", appointmentRepository.search(keyword));
        } else if (type != null) {
            model.addAttribute("appointments",
                    appointmentRepository.findByTypeOrderByAppointmentDateDescIdDesc(type));
        } else if (status != null) {
            model.addAttribute("appointments",
                    appointmentRepository.findByStatusOrderByAppointmentDateDescIdDesc(status));
        } else {
            model.addAttribute("appointments", appointmentRepository.findAllByOrderByAppointmentDateDescIdDesc());
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("types", AppointmentType.values());
        model.addAttribute("statuses", AppointmentStatus.values());
        return "appointments/list";
    }

    @GetMapping("/new/personal")
    public String personalForm(Model model) {
        addFormOptions(model, AppointmentType.PERSONAL);
        model.addAttribute("patients", patientRepository.findAll());
        return "appointments/personal-form";
    }

    @PostMapping("/personal")
    public String createPersonal(@RequestParam Long patientId, @RequestParam Long packageId,
                                 @RequestParam(required = false) Long doctorId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointmentDate,
                                 @RequestParam(required = false) String remark,
                                 RedirectAttributes redirectAttributes) {
        try {
            var appointment = appointmentService.createPersonal(patientId, packageId, doctorId,
                    appointmentDate, remark);
            redirectAttributes.addFlashAttribute("success",
                    "个人体检预约成功，预约号：" + appointment.getAppointmentNo());
            return "redirect:/appointments/" + appointment.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/appointments/new/personal";
        }
    }

    @GetMapping("/new/organization")
    public String organizationForm(Model model) {
        addFormOptions(model, AppointmentType.ORGANIZATION);
        return "appointments/organization-form";
    }

    @PostMapping(value = "/organization", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createOrganization(@RequestParam String organizationName,
                                     @RequestParam(required = false) String creditCode,
                                     @RequestParam String contactName,
                                     @RequestParam String contactPhone,
                                     @RequestParam(required = false) String address,
                                     @RequestParam Long packageId,
                                     @RequestParam(required = false) Long doctorId,
                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointmentDate,
                                     @RequestParam(required = false) String remark,
                                     @RequestParam(required = false) MultipartFile participantFile,
                                     RedirectAttributes redirectAttributes) {
        try {
            var appointment = appointmentService.createOrganization(organizationName, creditCode,
                    contactName, contactPhone, address, packageId, doctorId, appointmentDate,
                    remark, participantFile);
            redirectAttributes.addFlashAttribute("success",
                    "单位体检预约成功，已关联 " + appointment.getParticipants().size() + " 名参检人");
            return "redirect:/appointments/" + appointment.getId();
        } catch (AppointmentImportException ex) {
            redirectAttributes.addFlashAttribute("error", "导入失败：" + String.join("；", ex.getErrors()));
            return "redirect:/appointments/new/organization";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/appointments/new/organization";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("appointment", appointmentService.get(id));
        return "appointments/detail";
    }

    @PostMapping("/{id}/participants/import")
    public String importParticipants(@PathVariable Long id, @RequestParam MultipartFile file,
                                     RedirectAttributes redirectAttributes) {
        try {
            int imported = appointmentService.importParticipants(id, file);
            redirectAttributes.addFlashAttribute("success", "成功导入并关联 " + imported + " 名参检人");
        } catch (AppointmentImportException ex) {
            redirectAttributes.addFlashAttribute("error", "导入失败：" + String.join("；", ex.getErrors()));
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    @GetMapping("/organization/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = appointmentService.createImportTemplate();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("单位体检参检人导入模板.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        appointmentService.cancel(id);
        redirectAttributes.addFlashAttribute("success", "预约已取消");
        return "redirect:/appointments/" + id;
    }

    private void addFormOptions(Model model, AppointmentType type) {
        model.addAttribute("packages", packageRepository.findByEnabledTrueAndTypeOrderByIdDesc(
                type == AppointmentType.PERSONAL
                        ? com.hospital.examination.model.PackageType.PERSONAL
                        : com.hospital.examination.model.PackageType.ORGANIZATION));
        model.addAttribute("doctors", doctorRepository.findEnabledOrdered());
        model.addAttribute("today", LocalDate.now());
    }
}
