package tech.afrobank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.afrobank.dto.*;
import tech.afrobank.service.ContaCorrenteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contas-corrente")
@RequiredArgsConstructor
public class ContaCorrenteController {

    private final ContaCorrenteService contaCorrenteService;

    @GetMapping
    public ResponseEntity<List<ContaCorrenteResponseDTO>> listarTodas() {
        return ResponseEntity.ok(contaCorrenteService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaCorrenteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contaCorrenteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ContaCorrenteResponseDTO> abrirConta(@RequestBody @Valid ContaCorrenteRequestDTO dto) {
        ContaCorrenteResponseDTO conta = contaCorrenteService.abrirConta(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(conta);
    }

    @PostMapping("/{id}/depositar")
    public ResponseEntity<ContaCorrenteResponseDTO> depositar(
            @PathVariable Long id,
            @RequestBody @Valid OperacaoFinanceiraRequestDTO dto) {
        return ResponseEntity.ok(contaCorrenteService.depositar(id, dto));
    }

    @PostMapping("/{id}/sacar")
    public ResponseEntity<ContaCorrenteResponseDTO> sacar(
            @PathVariable Long id,
            @RequestBody @Valid OperacaoFinanceiraRequestDTO dto) {
        return ResponseEntity.ok(contaCorrenteService.sacar(id, dto));
    }

    @PostMapping("/transferir")
    public ResponseEntity<TransferenciaResponseDTO> transferir(
            @RequestBody @Valid TransferenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contaCorrenteService.transferir(dto));
    }
}
