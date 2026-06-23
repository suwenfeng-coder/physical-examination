package com.hospital.examination.web;

import com.hospital.examination.model.Gender;
import com.hospital.examination.model.Patient;
import com.hospital.examination.repository.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patients")
public class PatientController {
    private final PatientRepository repository;

    public PatientController(PatientRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAttribute("patients", keyword.isBlank()
                ? repository.findAll().stream().sorted((a, b) -> b.getId().compareTo(a.getId())).toList()
                : repository.findByNameContainingIgnoreCaseOrPhoneContainingOrderByIdDesc(keyword, keyword));
        model.addAttribute("keyword", keyword);
        return "patients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("genders", Gender.values());
        return "patients/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("patient", repository.findById(id).orElseThrow());
        model.addAttribute("genders", Gender.values());
        return "patients/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Patient patient, BindingResult bindingResult,
                       Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            return "patients/form";
        }
        if (patient.getId() == null) {
            repository.save(patient);
        } else {
            Patient saved = repository.findById(patient.getId()).orElseThrow();
            saved.setName(patient.getName());
            saved.setGender(patient.getGender());
            saved.setBirthday(patient.getBirthday());
            saved.setPhone(patient.getPhone());
            saved.setIdCard(patient.getIdCard());
            saved.setAddress(patient.getAddress());
            saved.setMedicalHistory(patient.getMedicalHistory());
            repository.save(saved);
        }
        redirectAttributes.addFlashAttribute("success", "受检人信息已保存");
        return "redirect:/patients";
    }
}
