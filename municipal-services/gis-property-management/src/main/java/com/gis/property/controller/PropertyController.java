package com.gis.property.controller;

import com.gis.property.model.Property;
import com.gis.property.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("properties")
public class PropertyController {

    private PropertyService propertyService;

    @Autowired
    public void setPropertyService(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public String index() {
        return "redirect:/properties/1";
    }

    /**
     * REST API: Get a list of properties as JSON.
     * @return List of properties.
     */
    @GetMapping("/api/all")
    @ResponseBody
    public List<Property> getAllProperties() {
        return propertyService.getAllProperties(); // Return JSON response
    }

    @GetMapping("/search")
    public String search(@RequestParam("search") String search,
                         @RequestParam(defaultValue = "1") int pageNumber,
                         Model model) {
        Page<Property> page = propertyService.searchProperties(search, pageNumber);

        int current = page.getNumber() + 1;
        int begin = Math.max(1, current - 10);
        int end = Math.min(begin + 10, page.getTotalPages());

        model.addAttribute("list", page);
        model.addAttribute("beginIndex", begin);
        model.addAttribute("endIndex", end);
        model.addAttribute("currentIndex", current);
        model.addAttribute("search", search); // Retain the search term in the UI
        return "properties/list";
    }

    @GetMapping(value = "/{pageNumber}")
    public String list(@PathVariable Integer pageNumber, Model model) {
        Page<Property> page = propertyService.getList(pageNumber);

        int current = page.getNumber() + 1;
        int begin = Math.max(1, current - 10);
        //int end = Math.min(begin + 10, page.getTotalPages());
        int end =  page.getTotalPages();

        model.addAttribute("list", page);
        model.addAttribute("beginIndex", begin);
        model.addAttribute("endIndex", end);
        model.addAttribute("currentIndex", current);

        return "properties/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("property", new Property());
        return "properties/form";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("property", propertyService.get(id));
        return "properties/form";
    }


    @PostMapping("/edit/{id}")
    public String editProperty(@PathVariable Long id,
                                 @ModelAttribute Property property,
                                 @RequestParam("pictureFile") MultipartFile pictureFile,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Update property with file handling
            property.setId(id);
            propertyService.update(property, pictureFile);

            redirectAttributes.addFlashAttribute("success", "Property updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update property: " + e.getMessage());
        }

        return "properties/form";
    }

    @PostMapping("/save")
    public String saveProperty(@ModelAttribute Property property,
                               @RequestParam("pictureFile") MultipartFile pictureFile,
                               RedirectAttributes redirectAttributes) {
        try {
            // Save property with file handling
            propertyService.save(property, pictureFile);

            redirectAttributes.addFlashAttribute("success", "Property saved successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save property: " + e.getMessage());
        }

        return "redirect:/properties";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        propertyService.delete(id);
        return "redirect:/properties";
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getPropertiesCount() {
        long count = propertyService.getPropertiesCount();
        return ResponseEntity.ok(count);
    }
}
