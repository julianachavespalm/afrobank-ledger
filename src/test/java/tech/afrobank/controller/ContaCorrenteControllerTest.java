package tech.afrobank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tech.afrobank.config.SecurityConfig;
import tech.afrobank.dto.TransferenciaRequestDTO;
import tech.afrobank.dto.TransferenciaResponseDTO;
import tech.afrobank.exception.IdempotencyException;
import tech.afrobank.exception.SaldoInsuficienteException;
import tech.afrobank.idempotency.IdempotencyManager;
import tech.afrobank.service.ContaCorrenteService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContaCorrenteController.class)
@Import(SecurityConfig.class)
@DisplayName("Testes de Integracao de API - ContaCorrenteController")
class ContaCorrenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContaCorrenteService contaCorrenteService;

    @MockBean
    private IdempotencyManager idempotencyManager;

    @Test
    @DisplayName("POST /api/v1/contas-corrente/transferir - Deve retornar 201 Created quando payload for valido")
    void deveRetornar201AoTransferirComSucesso() throws Exception {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("150.00"));
        TransferenciaResponseDTO response = new TransferenciaResponseDTO(
                10L, 1L, 2L, new BigDecimal("150.00"), "CONCLUIDA", LocalDateTime.now()
        );

        when(contaCorrenteService.transferir(any(TransferenciaRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/contas-corrente/transferir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transacaoId").value(10))
                .andExpect(jsonPath("$.valor").value(150.00))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("POST /api/v1/contas-corrente/transferir - Deve retornar 400 Bad Request quando valor for menor que R$ 0,01")
    void deveRetornar400QuandoValorInvalido() throws Exception {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/contas-corrente/transferir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição Inválida"))
                .andExpect(jsonPath("$.erros.valor").exists());
    }

    @Test
    @DisplayName("POST /api/v1/contas-corrente/transferir - Deve retornar 422 Unprocessable Entity quando saldo insuficiente")
    void deveRetornar422QuandoSaldoInsuficiente() throws Exception {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("5000.00"));

        when(contaCorrenteService.transferir(any(TransferenciaRequestDTO.class)))
                .thenThrow(new SaldoInsuficienteException("Saldo insuficiente"));

        mockMvc.perform(post("/api/v1/contas-corrente/transferir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Saldo Insuficiente"));
    }

    @Test
    @DisplayName("POST /api/v1/contas-corrente/transferir - Deve retornar 409 Conflict quando idempotency key duplicada")
    void deveRetornar409QuandoIdempotencyKeyDuplicada() throws Exception {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("100.00"));
        String idempotencyKey = "uuid-chave-duplicada";

        doThrow(new IdempotencyException("Operacao ja processada"))
                .when(idempotencyManager).acquire(idempotencyKey);

        mockMvc.perform(post("/api/v1/contas-corrente/transferir")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de Idempotência"));
    }
}
