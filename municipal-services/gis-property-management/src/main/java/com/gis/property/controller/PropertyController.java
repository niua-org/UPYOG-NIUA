
package com.gis.property.controller;

import com.gis.property.entity.Property;
import com.gis.property.service.PropertyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/properties")
public class PropertyController {
    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @GetMapping
    public String listProperties(Model model) {
        model.addAttribute("properties", service.getAllProperties());
        return "property/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("property", new Property());
        return "property/form";
    }

    @PostMapping
    public String saveProperty(@ModelAttribute Property property) {
        service.saveProperty(property);
        return "redirect:/properties";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Property property = service.getPropertyById(id);
        if (property == null) return "redirect:/properties";
        model.addAttribute("property", property);
        return "property/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteProperty(@PathVariable Long id) {
        service.deleteProperty(id);
        return "redirect:/properties";
    }
}
