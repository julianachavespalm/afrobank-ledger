package tech.afrobank.dto;

import tech.afrobank.entity.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResponseDTO(
    Long id,
    String nome,
    String cpf,
    String email,
    String endereco,
    BigDecimal rendaSalarial,
    LocalDateTime criadoEm
) {
    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        return new ClienteResponseDTO(
            cliente.getId(),
            cliente.getNome(),
            cliente.getCpf(),
            cliente.getEmail(),
            cliente.getEndereco(),
            cliente.getRendaSalarial(),
            cliente.getCriadoEm()
        );
    }
}
