package com.hospital.examination.web;

import com.hospital.examination.model.Doctor;
import com.hospital.examination.repository.DoctorRepository;
import com.hospital.examination.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/doctors")
public class DoctorController {
    private final DoctorRepository repository;
    private final DepartmentRepository departmentRepository;

    public DoctorController(DoctorRepository repository, DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("doctors", repository.findAllOrdered());
        return "doctors/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        return "doctors/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("doctor", repository.findById(id).orElseThrow());
        return "doctors/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Doctor doctor, BindingResult bindingResult,
                       @RequestParam(required = false) Long departmentId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (departmentId == null) {
            bindingResult.rejectValue("department", "department.required", "请选择所属科室");
        } else {
            doctor.setDepartment(departmentRepository.findById(departmentId).orElse(null));
            if (doctor.getDepartment() == null) {
                bindingResult.rejectValue("department", "department.invalid", "所选科室不存在");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("selectedDepartment", doctor.getDepartment());
            return "doctors/form";
        }
        repository.save(doctor);
        redirectAttributes.addFlashAttribute("success", "医生信息已保存");
        return "redirect:/doctors";
    }
}
