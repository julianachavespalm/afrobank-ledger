package tech.afrobank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 255)
    private String endereco;

    @Column(name = "renda_salarial", precision = 15, scale = 2)
    private BigDecimal rendaSalarial;

    @Column(nullable = false, length = 100)
    private String senha;

    @Column(name = "criado_em", updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
