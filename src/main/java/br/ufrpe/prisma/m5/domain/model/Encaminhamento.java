package br.ufrpe.prisma.m5.domain.model;

import br.ufrpe.prisma.m5.domain.enums.StatusEncaminhamento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "encaminhamentos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Encaminhamento {

    /**
     * Chave primária = UUID da denúncia (vindo do M1 via M3).
     * Garante idempotência: se o mesmo evento chegar duas vezes, não duplica.
     */
    @Id
    private UUID id;

    // Secretaria de destino (dados do M9; secretariaId é null quando fallback é usado)
    private UUID secretariaId;
    private String secretariaNome;
    private String secretariaSigla;

    private String nivel;           // CRITICO | ALTO | MEDIO | BAIXO
    private String categoria;
    private String areaResponsavel;
    private Double score;

    @Enumerated(EnumType.STRING)
    private StatusEncaminhamento status;

    private LocalDateTime priorizadaEm;
    private LocalDateTime encaminhadaEm;

    @PrePersist
    private void prePersist() {
        if (encaminhadaEm == null) encaminhadaEm = LocalDateTime.now();
        if (status == null) status = StatusEncaminhamento.ENCAMINHADA;
    }
}
