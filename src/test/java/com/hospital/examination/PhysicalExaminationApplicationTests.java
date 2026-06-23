package com.hospital.examination;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhysicalExaminationApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void publicLoginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void managementPagesRenderAfterLogin() throws Exception {
        String[] paths = {
                "/", "/patients", "/patients/new", "/doctors", "/doctors/new",
                "/items", "/items/new", "/packages", "/packages/new", "/orders", "/orders/new"
        };
        for (String path : paths) {
            mockMvc.perform(get(path).sessionAttr("LOGIN_USER", "admin"))
                    .andExpect(status().isOk());
        }
    }
}
