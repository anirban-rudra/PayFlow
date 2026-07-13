package com.paypal.wallet_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.wallet_service.dto.CreateWalletRequest;
import com.paypal.wallet_service.dto.CreditRequest;
import com.paypal.wallet_service.dto.HoldRequest;
import com.paypal.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }

    @Test
    void walletMutationRequiresInternalApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRequest(200L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalServiceCanCreateWalletAndOwnerCanReadIt() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRequest(201L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(201))
                .andExpect(jsonPath("$.currency").value("INR"));

        mockMvc.perform(get("/api/v1/wallets/201")
                        .header("X-User-Id", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(201));
    }

    @Test
    void userCannotReadAnotherUsersWallet() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRequest(202L))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wallets/202")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanTopUpOwnWalletButNotAnotherUsersWallet() throws Exception {
        createWallet(205L);

        mockMvc.perform(post("/api/v1/wallets/205/top-ups")
                        .header("X-User-Id", "205")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"INR","amount":250.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00))
                .andExpect(jsonPath("$.availableBalance").value(250.00));

        mockMvc.perform(post("/api/v1/wallets/205/top-ups")
                        .header("X-User-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"INR","amount":1.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalServiceCanCreditHoldCaptureAndReleaseWalletFunds() throws Exception {
        createWallet(203L);

        mockMvc.perform(post("/api/v1/wallets/credit")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditRequest(203L, "100.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.availableBalance").value(100.00));

        String holdReference = mockMvc.perform(post("/api/v1/wallets/hold")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdRequest(203L, "25.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reference = objectMapper.readTree(holdReference).get("holdReference").asText();

        mockMvc.perform(get("/api/v1/wallets/203")
                        .header("X-User-Id", "203"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.availableBalance").value(75.00));

        mockMvc.perform(post("/api/v1/wallets/release/{holdReference}", reference)
                        .header("X-Internal-Api-Key", "test-internal-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        String secondHold = mockMvc.perform(post("/api/v1/wallets/hold")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdRequest(203L, "30.00"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/wallets/capture")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holdReference":"%s"}
                                """.formatted(objectMapper.readTree(secondHold).get("holdReference").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00))
                .andExpect(jsonPath("$.availableBalance").value(70.00));
    }

    @Test
    void validationRejectsMissingUserIdInvalidCurrencyAndOverPreciseAmount() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"INR"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":204,"currency":"INRR"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/wallets/credit")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":204,"currency":"INR","amount":1.001}
                                """))
                .andExpect(status().isBadRequest());
    }

    private CreateWalletRequest createWalletRequest(Long userId) {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);
        request.setCurrency("INR");
        return request;
    }

    private void createWallet(Long userId) throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Internal-Api-Key", "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRequest(userId))))
                .andExpect(status().isOk());
    }

    private CreditRequest creditRequest(Long userId, String amount) {
        CreditRequest request = new CreditRequest();
        request.setUserId(userId);
        request.setCurrency("INR");
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private HoldRequest holdRequest(Long userId, String amount) {
        HoldRequest request = new HoldRequest();
        request.setUserId(userId);
        request.setCurrency("INR");
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
