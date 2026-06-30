package br.ufrpe.prisma.m5.domain.dto;

import br.ufrpe.prisma.m5.domain.enums.StatusEncaminhamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EncaminhamentoResponseDTO(
        UUID id,
        UUID secretariaId,
        String secretariaNome,
        String secretariaSigla,
        String nivel,
        String categoria,
        String areaResponsavel,
        double score,
        StatusEncaminhamento status,
        LocalDateTime priorizadaEm,
        LocalDateTime encaminhadaEm
) {}
