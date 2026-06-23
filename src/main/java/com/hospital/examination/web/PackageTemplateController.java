package com.hospital.examination.web;

import com.hospital.examination.model.CheckupItem;
import com.hospital.examination.model.PackageTemplate;
import com.hospital.examination.model.PackageType;
import com.hospital.examination.repository.CheckupItemRepository;
import com.hospital.examination.repository.PackageTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;

@Controller
@RequestMapping("/package-templates")
public class PackageTemplateController {
    private final PackageTemplateRepository templateRepository;
    private final CheckupItemRepository itemRepository;

    public PackageTemplateController(PackageTemplateRepository templateRepository,
                                     CheckupItemRepository itemRepository) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", templateRepository.findAllByOrderByIdDesc());
        return "package-templates/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("packageTemplate", new PackageTemplate());
        addOptions(model);
        return "package-templates/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("packageTemplate", templateRepository.findById(id).orElseThrow());
        addOptions(model);
        return "package-templates/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("packageTemplate") PackageTemplate packageTemplate,
                       BindingResult bindingResult,
                       @RequestParam(required = false, name = "itemIds") List<Long> itemIds,
                       Model model, RedirectAttributes redirectAttributes) {
        List<CheckupItem> selectedItems = itemIds == null
                ? List.of()
                : itemRepository.findAllById(itemIds);
        packageTemplate.setItems(new LinkedHashSet<>(selectedItems));
        String name = packageTemplate.getName() == null ? "" : packageTemplate.getName().trim();
        packageTemplate.setName(name);
        boolean duplicate = packageTemplate.getId() == null
                ? templateRepository.existsByName(name)
                : templateRepository.existsByNameAndIdNot(name, packageTemplate.getId());
        if (duplicate) {
            bindingResult.rejectValue("name", "name.duplicate", "模板名称已存在");
        }
        if (itemIds == null || itemIds.isEmpty()) {
            bindingResult.reject("items.required", "请至少选择一个检查项目");
        }
        if (bindingResult.hasErrors()) {
            addOptions(model);
            return "package-templates/form";
        }
        templateRepository.save(packageTemplate);
        redirectAttributes.addFlashAttribute("success", "基础套餐模板已保存");
        return "redirect:/package-templates";
    }

    private void addOptions(Model model) {
        model.addAttribute("allItems", itemRepository.findAllOrdered());
        model.addAttribute("packageTypes", PackageType.values());
    }
}
