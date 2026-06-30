package br.ufrpe.prisma.m5.domain.dto;

/**
 * Payload do evento denuncia.encaminhada, publicado pelo M5.
 * Consumido por M6 (Notificações) e M7 (Analytics).
 * Campos em snake_case para compatibilidade com os consumidores Python.
 */
public record DenunciaEncaminhadaEvent(
        String id,
        String secretaria_id,
        String secretaria_nome,
        String secretaria_sigla,
        String nivel,
        String categoria,
        String area_responsavel,
        double score,
        String encaminhada_em
) {}
