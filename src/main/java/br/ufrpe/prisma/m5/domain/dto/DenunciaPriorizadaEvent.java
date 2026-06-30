package br.ufrpe.prisma.m5.domain.dto;

/**
 * Payload do evento denuncia.priorizada, produzido pelo M3 (Python).
 * Campos em snake_case para compatibilidade com a serialização JSON do M3.
 */
public record DenunciaPriorizadaEvent(
        String id,
        double score,
        String nivel,
        String categoria,
        String area_responsavel,
        double urgencia_categoria,
        double peso_confianca,
        double boost_recorrencia,
        String priorizado_em
) {}
