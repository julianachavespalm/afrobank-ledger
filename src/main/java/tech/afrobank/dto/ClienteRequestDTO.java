package tech.afrobank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ClienteRequestDTO(
    @NotBlank(message = "O nome é obrigatório.")
    String nome,

    @NotBlank(message = "O CPF é obrigatório.")
    String cpf,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Formato de e-mail inválido.")
    String email,

    String endereco,

    @NotNull(message = "A renda salarial é obrigatória.")
    @PositiveOrZero(message = "A renda salarial não pode ser negativa.")
    BigDecimal rendaSalarial,

    @NotBlank(message = "A senha é obrigatória.")
    String senha
) {}
