package tech.afrobank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tech.afrobank.dto.*;
import tech.afrobank.entity.Cliente;
import tech.afrobank.entity.ContaCorrente;
import tech.afrobank.entity.Transacao;
import tech.afrobank.exception.NegocioException;
import tech.afrobank.exception.RecursoNaoEncontradoException;
import tech.afrobank.repository.ContaCorrenteRepository;
import tech.afrobank.repository.TransacaoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaCorrenteService {

    private final ContaCorrenteRepository contaCorrenteRepository;
    private final ClienteService clienteService;
    private final TransacaoRepository transacaoRepository;

    @Transactional(readOnly = true)
    public List<ContaCorrenteResponseDTO> listarTodas() {
        return contaCorrenteRepository.findAll().stream()
                .map(ContaCorrenteResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaCorrenteResponseDTO buscarPorId(Long id) {
        ContaCorrente conta = buscarEntidadePorId(id);
        return ContaCorrenteResponseDTO.fromEntity(conta);
    }

    @Transactional
    public ContaCorrenteResponseDTO abrirConta(ContaCorrenteRequestDTO dto) {
        if (contaCorrenteRepository.existsByNumeroConta(dto.numeroConta())) {
            throw new NegocioException("Já existe uma conta com o número informado: " + dto.numeroConta());
        }

        Cliente cliente = clienteService.buscarEntidadePorId(dto.clienteId());

        BigDecimal saldoInicial = dto.saldoInicial() != null ? dto.saldoInicial() : BigDecimal.ZERO;
        BigDecimal limite = dto.limiteChequeEspecial() != null ? dto.limiteChequeEspecial() : BigDecimal.ZERO;

        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta(dto.numeroConta())
                .saldo(saldoInicial.setScale(2, RoundingMode.HALF_EVEN))
                .limiteChequeEspecial(limite.setScale(2, RoundingMode.HALF_EVEN))
                .cliente(cliente)
                .build();

        ContaCorrente salva = contaCorrenteRepository.save(conta);
        return ContaCorrenteResponseDTO.fromEntity(salva);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ContaCorrenteResponseDTO depositar(Long contaId, OperacaoFinanceiraRequestDTO dto) {
        ContaCorrente conta = contaCorrenteRepository.findByIdWithLock(contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada com ID: " + contaId));

        conta.creditar(dto.valor());
        contaCorrenteRepository.save(conta);

        transacaoRepository.save(Transacao.builder()
                .contaDestinoId(contaId)
                .valor(dto.valor())
                .tipo(Transacao.TipoTransacao.DEPOSITO)
                .build());

        return ContaCorrenteResponseDTO.fromEntity(conta);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ContaCorrenteResponseDTO sacar(Long contaId, OperacaoFinanceiraRequestDTO dto) {
        ContaCorrente conta = contaCorrenteRepository.findByIdWithLock(contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada com ID: " + contaId));

        conta.debitar(dto.valor());
        contaCorrenteRepository.save(conta);

        transacaoRepository.save(Transacao.builder()
                .contaOrigemId(contaId)
                .contaDestinoId(contaId)
                .valor(dto.valor())
                .tipo(Transacao.TipoTransacao.SAQUE)
                .build());

        return ContaCorrenteResponseDTO.fromEntity(conta);
    }

    /**
     * Transferência Atômica com Prevenção de Deadlock via Ordenação Determinística de Locks
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferenciaResponseDTO transferir(TransferenciaRequestDTO dto) {
        if (dto.contaOrigemId().equals(dto.contaDestinoId())) {
            throw new NegocioException("A conta de origem e destino não podem ser as mesmas.");
        }

        // Ordenação determinística de lock por ID para prevenir deadlocks
        Long primeiroId = Math.min(dto.contaOrigemId(), dto.contaDestinoId());
        Long segundoId = Math.max(dto.contaOrigemId(), dto.contaDestinoId());

        ContaCorrente primeiraConta = contaCorrenteRepository.findByIdWithLock(primeiroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + primeiroId));
        ContaCorrente segundaConta = contaCorrenteRepository.findByIdWithLock(segundoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + segundoId));

        ContaCorrente contaOrigem = dto.contaOrigemId().equals(primeiroId) ? primeiraConta : segundaConta;
        ContaCorrente contaDestino = dto.contaDestinoId().equals(primeiroId) ? primeiraConta : segundaConta;

        // Débito e Crédito com precisão de domínio
        contaOrigem.debitar(dto.valor());
        contaDestino.creditar(dto.valor());

        contaCorrenteRepository.save(contaOrigem);
        contaCorrenteRepository.save(contaDestino);

        // Registro de Auditoria / Ledger
        Transacao transacao = transacaoRepository.save(Transacao.builder()
                .contaOrigemId(contaOrigem.getId())
                .contaDestinoId(contaDestino.getId())
                .valor(dto.valor())
                .tipo(Transacao.TipoTransacao.TRANSFERENCIA)
                .status("CONCLUIDA")
                .build());

        return new TransferenciaResponseDTO(
                transacao.getId(),
                contaOrigem.getId(),
                contaDestino.getId(),
                transacao.getValor(),
                transacao.getStatus(),
                transacao.getCriadoEm()
        );
    }

    @Transactional(readOnly = true)
    public ContaCorrente buscarEntidadePorId(Long id) {
        return contaCorrenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada com ID: " + id));
    }
}
