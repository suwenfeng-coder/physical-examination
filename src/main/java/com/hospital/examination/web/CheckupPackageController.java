package com.hospital.examination.web;

import com.hospital.examination.model.CheckupItem;
import com.hospital.examination.model.CheckupPackage;
import com.hospital.examination.model.PackageType;
import com.hospital.examination.repository.CheckupItemRepository;
import com.hospital.examination.repository.CheckupPackageRepository;
import com.hospital.examination.repository.PackageTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.math.BigDecimal;

@Controller
@RequestMapping("/packages")
public class CheckupPackageController {
    private final CheckupPackageRepository packageRepository;
    private final CheckupItemRepository itemRepository;
    private final PackageTemplateRepository templateRepository;

    public CheckupPackageController(CheckupPackageRepository packageRepository,
                                    CheckupItemRepository itemRepository,
                                    PackageTemplateRepository templateRepository) {
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
        this.templateRepository = templateRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) PackageType type, Model model) {
        model.addAttribute("packages", type == null
                ? packageRepository.findAllByOrderByIdDesc()
                : packageRepository.findByTypeOrderByIdDesc(type));
        model.addAttribute("selectedType", type);
        model.addAttribute("packageTypes", PackageType.values());
        return "packages/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("checkupPackage", new CheckupPackage());
        addOptions(model, true);
        return "packages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("checkupPackage", packageRepository.findById(id).orElseThrow());
        addOptions(model, false);
        return "packages/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("checkupPackage") CheckupPackage checkupPackage,
                       BindingResult bindingResult,
                       @RequestParam(required = false, name = "itemIds") List<Long> itemIds,
                       Model model, RedirectAttributes redirectAttributes) {
        List<CheckupItem> selectedItems = itemIds == null
                ? List.of()
                : itemRepository.findAllById(itemIds);
        checkupPackage.setItems(new LinkedHashSet<>(selectedItems));
        BigDecimal guidePrice = selectedItems.stream()
                .map(CheckupItem::getPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        checkupPackage.setGuidePrice(guidePrice);
        if (itemIds == null || itemIds.isEmpty()) {
            bindingResult.reject("items.required", "请至少选择一个检查项目");
        }
        if (bindingResult.hasErrors()) {
            addOptions(model, checkupPackage.getId() == null);
            return "packages/form";
        }
        packageRepository.save(checkupPackage);
        redirectAttributes.addFlashAttribute("success", "体检套餐已保存");
        return "redirect:/packages";
    }

    private void addOptions(Model model, boolean includeTemplates) {
        model.addAttribute("allItems", itemRepository.findAllOrdered());
        model.addAttribute("packageTypes", PackageType.values());
        if (includeTemplates) {
            model.addAttribute("packageTemplates", templateRepository.findByEnabledTrueOrderByIdDesc());
        }
    }
}
