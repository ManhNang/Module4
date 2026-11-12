package com.example.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public UI should be accessible without credentials")
    void testIndexHtmlPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Valid credentials and existing location should return 200 OK")
    void testGetWeatherSuccess() throws Exception {
        mockMvc.perform(get("/weather/client1/123456/hanoi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("hanoi"))
                .andExpect(jsonPath("$.cityName").value("Hà Nội"))
                .andExpect(jsonPath("$.temperature").isNumber())
                .andExpect(jsonPath("$.condition").isString());
    }

    @Test
    @DisplayName("Valid credentials but non-existing location should return 400 Bad Request")
    void testGetWeatherLocationNotFoundReturns400() throws Exception {
        mockMvc.perform(get("/weather/client1/123456/nonexistinglocation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Invalid password should return 401 Unauthorized")
    void testGetWeatherWrongPasswordReturns401() throws Exception {
        mockMvc.perform(get("/weather/client1/wrongpass/hanoi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Invalid username should return 401 Unauthorized")
    void testGetWeatherWrongUserReturns401() throws Exception {
        mockMvc.perform(get("/weather/unknownuser/123456/hanoi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

