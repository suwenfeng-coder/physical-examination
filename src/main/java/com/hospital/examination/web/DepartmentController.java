package com.hospital.examination.web;

import com.hospital.examination.model.Department;
import com.hospital.examination.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/departments")
public class DepartmentController {
    private final DepartmentRepository repository;

    public DepartmentController(DepartmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", repository.findAllByOrderBySortOrderAscNameAsc());
        return "departments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("department", new Department());
        return "departments/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("department", repository.findById(id).orElseThrow());
        return "departments/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Department department,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        String name = department.getName() == null ? "" : department.getName().trim();
        department.setName(name);
        if (department.getId() == null ? repository.existsByName(name)
                : repository.existsByNameAndIdNot(name, department.getId())) {
            bindingResult.rejectValue("name", "name.duplicate", "科室名称已存在");
        }
        if (bindingResult.hasErrors()) {
            return "departments/form";
        }
        repository.save(department);
        redirectAttributes.addFlashAttribute("success", "科室信息已保存");
        return "redirect:/departments";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam(defaultValue = "") String keyword) {
        List<Map<String, Object>> result = repository
                .findByEnabledTrueAndNameContainingIgnoreCaseOrderBySortOrderAscNameAsc(keyword.trim())
                .stream()
                .limit(20)
                .map(department -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", department.getId());
                    item.put("name", department.getName());
                    item.put("code", department.getCode() == null ? "" : department.getCode());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(result);
    }
}
