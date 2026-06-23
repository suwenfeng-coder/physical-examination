package com.hospital.examination.web;

import com.hospital.examination.model.Doctor;
import com.hospital.examination.repository.DoctorRepository;
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

    public DoctorController(DoctorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("doctors", repository.findAllByOrderByDepartmentAscNameAsc());
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
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "doctors/form";
        }
        repository.save(doctor);
        redirectAttributes.addFlashAttribute("success", "医生信息已保存");
        return "redirect:/doctors";
    }
}
