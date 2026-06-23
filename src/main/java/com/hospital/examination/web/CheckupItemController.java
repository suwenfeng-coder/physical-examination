package com.hospital.examination.web;

import com.hospital.examination.model.CheckupItem;
import com.hospital.examination.repository.CheckupItemRepository;
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

    public CheckupItemController(CheckupItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAllByOrderByDepartmentAscNameAsc());
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
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "items/form";
        }
        repository.save(item);
        redirectAttributes.addFlashAttribute("success", "检查项目已保存");
        return "redirect:/items";
    }
}
