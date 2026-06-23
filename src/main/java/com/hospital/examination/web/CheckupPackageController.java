package com.hospital.examination.web;

import com.hospital.examination.model.CheckupItem;
import com.hospital.examination.model.CheckupPackage;
import com.hospital.examination.repository.CheckupItemRepository;
import com.hospital.examination.repository.CheckupPackageRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;

@Controller
@RequestMapping("/packages")
public class CheckupPackageController {
    private final CheckupPackageRepository packageRepository;
    private final CheckupItemRepository itemRepository;

    public CheckupPackageController(CheckupPackageRepository packageRepository,
                                    CheckupItemRepository itemRepository) {
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("packages", packageRepository.findAllByOrderByIdDesc());
        return "packages/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("checkupPackage", new CheckupPackage());
        model.addAttribute("allItems", itemRepository.findAllOrdered());
        return "packages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("checkupPackage", packageRepository.findById(id).orElseThrow());
        model.addAttribute("allItems", itemRepository.findAllOrdered());
        return "packages/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("checkupPackage") CheckupPackage checkupPackage,
                       BindingResult bindingResult,
                       @RequestParam(required = false, name = "itemIds") List<Long> itemIds,
                       Model model, RedirectAttributes redirectAttributes) {
        if (itemIds == null || itemIds.isEmpty()) {
            bindingResult.reject("items.required", "请至少选择一个检查项目");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("allItems", itemRepository.findAllOrdered());
            return "packages/form";
        }
        List<CheckupItem> selectedItems = itemRepository.findAllById(itemIds);
        checkupPackage.setItems(new LinkedHashSet<>(selectedItems));
        packageRepository.save(checkupPackage);
        redirectAttributes.addFlashAttribute("success", "体检套餐已保存");
        return "redirect:/packages";
    }
}
