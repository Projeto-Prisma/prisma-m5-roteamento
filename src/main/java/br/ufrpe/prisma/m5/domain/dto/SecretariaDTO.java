package br.ufrpe.prisma.m5.domain.dto;

import java.util.List;
import java.util.UUID;

/**
 * Representação de uma secretaria retornada pela API REST do M9.
 * id pode ser null quando o roteamento usa o mapa de fallback local.
 */
public record SecretariaDTO(
        UUID id,
        String nome,
        String sigla,
        List<String> areasAtendidas,
        boolean ativo
) {}
