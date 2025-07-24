package com.eaglebank.controller;

import com.eaglebank.dto.TransactionRequest;
import com.eaglebank.dto.TransactionResponse;
import com.eaglebank.model.TransactionType;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private BankAccountRepository accountRepo;

    @Autowired
    private UserRepository userRepo;

    @BeforeEach
    void setup() {
        transactionRepo.deleteAll();
        accountRepo.deleteAll();
        userRepo.deleteAll();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreateTransaction_ShouldReturn200Ok() throws Exception {
        TransactionRequest request = new TransactionRequest(100.0, TransactionType.DEPOSIT);
        TransactionResponse response = new TransactionResponse(1L, TransactionType.DEPOSIT, 100.0, 600.0);

        Mockito.when(transactionService.createTransaction(eq(1L), any(TransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/accounts/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.updatedBalance").value(600.0));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreateTransaction_MissingData_ShouldReturn400_BadRequest() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/v1/accounts/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("required")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetTransaction_ShouldReturnTransactionDetails() throws Exception {
        long accountId = 1L;
        long transactionId = 100L;

        TransactionResponse mockResponse = new TransactionResponse(
                transactionId,
                TransactionType.DEPOSIT,
                200.0,
                700.0
        );

        Mockito.when(transactionService.getTransaction(accountId, transactionId))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/v1/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(200.0))
                .andExpect(jsonPath("$.updatedBalance").value(700.0));
    }

    @Test
    @WithMockUser(username = "intruder@example.com")
    void testGetTransaction_WhenAccountNotOwnedByUser_ShouldReturn403_Forbidden() throws Exception {
        long accountId = 1L;
        long transactionId = 100L;

        Mockito.when(transactionService.getTransaction(accountId, transactionId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied to this transaction"));

        mockMvc.perform(get("/v1/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied to this transaction"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetTransaction_WhenAccountDoesNotExist_ShouldReturn404_NotFound() throws Exception {
        long nonExistentAccountId = 999L;
        long transactionId = 100L;

        Mockito.when(transactionService.getTransaction(nonExistentAccountId, transactionId))
                .thenThrow(new NoSuchElementException("Account not found with id 999"));

        mockMvc.perform(get("/v1/accounts/{accountId}/transactions/{transactionId}", nonExistentAccountId, transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account not found with id 999"));
    }
}
