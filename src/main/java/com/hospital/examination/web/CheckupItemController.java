package com.hospital.examination.web;

import com.hospital.examination.model.CheckupItem;
import com.hospital.examination.repository.CheckupItemRepository;
import com.hospital.examination.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/items")
public class CheckupItemController {
    private final CheckupItemRepository repository;
    private final DepartmentRepository departmentRepository;

    public CheckupItemController(CheckupItemRepository repository, DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAllOrdered());
        return "items/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("item", new CheckupItem());
        return "items/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", repository.findById(id).orElseThrow());
        return "items/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("item") CheckupItem item, BindingResult bindingResult,
                       @RequestParam(required = false) Long departmentId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (departmentId == null) {
            bindingResult.rejectValue("department", "department.required", "请选择所属科室");
        } else {
            item.setDepartment(departmentRepository.findById(departmentId).orElse(null));
            if (item.getDepartment() == null) {
                bindingResult.rejectValue("department", "department.invalid", "所选科室不存在");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("selectedDepartment", item.getDepartment());
            return "items/form";
        }
        repository.save(item);
        redirectAttributes.addFlashAttribute("success", "检查项目已保存");
        return "redirect:/items";
    }
}
