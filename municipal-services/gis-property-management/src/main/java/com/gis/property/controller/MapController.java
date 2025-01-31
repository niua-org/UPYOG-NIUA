package com.gis.property.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/maps")
public class MapController {

    @GetMapping("/view")
    public String viewMap() {
        return "maps/map"; // Thymeleaf template name (map.html)
    }
}
