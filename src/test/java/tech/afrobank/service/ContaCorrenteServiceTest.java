package tech.afrobank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.afrobank.dto.TransferenciaRequestDTO;
import tech.afrobank.dto.TransferenciaResponseDTO;
import tech.afrobank.entity.Cliente;
import tech.afrobank.entity.ContaCorrente;
import tech.afrobank.entity.Transacao;
import tech.afrobank.exception.NegocioException;
import tech.afrobank.exception.SaldoInsuficienteException;
import tech.afrobank.repository.ContaCorrenteRepository;
import tech.afrobank.repository.TransacaoRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Unidade - ContaCorrenteService")
class ContaCorrenteServiceTest {

    @Mock
    private ContaCorrenteRepository contaCorrenteRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private ContaCorrenteService contaCorrenteService;

    private ContaCorrente contaOrigem;
    private ContaCorrente contaDestino;

    @BeforeEach
    void setUp() {
        Cliente cliente = Cliente.builder().id(1L).nome("Juliana Palm").build();

        contaOrigem = ContaCorrente.builder()
                .id(1L)
                .numeroConta("001-A")
                .saldo(new BigDecimal("1000.00"))
                .limiteChequeEspecial(new BigDecimal("200.00"))
                .cliente(cliente)
                .build();

        contaDestino = ContaCorrente.builder()
                .id(2L)
                .numeroConta("002-B")
                .saldo(new BigDecimal("500.00"))
                .limiteChequeEspecial(BigDecimal.ZERO)
                .cliente(cliente)
                .build();
    }

    @Test
    @DisplayName("Deve realizar transferência atômica com sucesso e registrar transação no ledger")
    void deveTransferirComSucesso() {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("300.00"));

        when(contaCorrenteRepository.findByIdWithLock(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaCorrenteRepository.findByIdWithLock(2L)).thenReturn(Optional.of(contaDestino));
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(invocation -> {
            Transacao t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransferenciaResponseDTO response = contaCorrenteService.transferir(request);

        assertThat(response).isNotNull();
        assertThat(response.valor()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.status()).isEqualTo("CONCLUIDA");

        // Validação de Saldos
        assertThat(contaOrigem.getSaldo()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(contaDestino.getSaldo()).isEqualByComparingTo(new BigDecimal("800.00"));

        verify(contaCorrenteRepository).save(contaOrigem);
        verify(contaCorrenteRepository).save(contaDestino);
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve impedir transferência quando origem e destino são a mesma conta")
    void deveImpedirTransferenciaParaMesmaConta() {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 1L, new BigDecimal("50.00"));

        assertThatThrownBy(() -> contaCorrenteService.transferir(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("A conta de origem e destino não podem ser as mesmas");

        verifyNoInteractions(transacaoRepository);
    }

    @Test
    @DisplayName("Deve impedir transferência quando saldo for insuficiente")
    void deveImpedirTransferenciaSemSaldo() {
        TransferenciaRequestDTO request = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("1500.00"));

        when(contaCorrenteRepository.findByIdWithLock(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaCorrenteRepository.findByIdWithLock(2L)).thenReturn(Optional.of(contaDestino));

        assertThatThrownBy(() -> contaCorrenteService.transferir(request))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(transacaoRepository, never()).save(any(Transacao.class));
    }
}
