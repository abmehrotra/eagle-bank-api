package com.eaglebank.controller;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.service.BankAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankAccountService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreateAccount_WhenCurrentUserIsAuthenticated_ShouldReturn201Created() throws Exception {
        BankAccountRequest request = new BankAccountRequest("SAVINGS", 1000.0);
        BankAccountResponse response = new BankAccountResponse(1L, "SAVINGS", 1000.0);

        Mockito.when(service.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(1000.0));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreateAccount_WithMissingFields_WhenCurrentUserIsAuthenticated_ShouldReturn400BadRequest() throws Exception {
        BankAccountRequest invalidRequest = new BankAccountRequest("", null);

        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetAccount_WhenCurrentUserIsAuthenticated_AndBankAccountProvidedExists_ShouldReturnBankDetails() throws Exception {
        BankAccountResponse response = new BankAccountResponse(2L, "CURRENT", 5000.0);
        Mockito.when(service.getAccountById(2L)).thenReturn(response);

        mockMvc.perform(get("/v1/accounts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.accountType").value("CURRENT"))
                .andExpect(jsonPath("$.balance").value(5000.0));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetAccount_WhenAccessingAnotherUsersAccount_ShouldReturn403Forbidden() throws Exception {
        Mockito.when(service.getAccountById(3L))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied to this account"));

        mockMvc.perform(get("/v1/accounts/3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied to this account"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetAccount_WhenAccountDoesNotExist_ShouldReturn404NotFound() throws Exception {
        Mockito.when(service.getAccountById(4L))
                .thenThrow(new java.util.NoSuchElementException("Bank account not found"));

        mockMvc.perform(get("/v1/accounts/4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Bank account not found"));
    }
}
