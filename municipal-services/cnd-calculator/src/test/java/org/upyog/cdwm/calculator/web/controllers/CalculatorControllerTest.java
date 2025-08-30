package org.upyog.cdwm.calculator.web.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.upyog.cdwm.calculator.TestConfiguration;
import org.junit.jupiter.api.Test;

/**
* API tests for V1ApiController
*/
@Disabled
@ExtendWith(SpringExtension.class)   // ✅ JUnit 5 extension
@WebMvcTest(CalculatorControllerTest.class)
@Import(TestConfiguration.class)
public class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void v1RegistrationCreatePostSuccess() throws Exception {
        mockMvc.perform(post("/cnd-calculator/v1/_create").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());
    }

    @Test
    public void v1RegistrationCreatePostFailure() throws Exception {
        mockMvc.perform(post("/cnd-calculator/v1/_create").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isBadRequest());
    }

 
   
}
