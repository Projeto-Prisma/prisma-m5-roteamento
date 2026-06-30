package br.ufrpe.prisma.m5.service;

import br.ufrpe.prisma.m5.domain.dto.SecretariaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta a API REST do M9 (Secretarias) para descobrir o órgão de destino.
 *
 * Estratégia de resiliência:
 *   1. Tenta GET /secretarias?area={area} no M9.
 *   2. Se o M9 retornar vazio, tenta buscar por categoria.
 *   3. Se o M9 estiver indisponível (timeout / erro), usa o mapa de fallback local.
 *
 * O fallback garante que o roteamento funcione mesmo sem M9 no ar —
 * comportamento essencial em ambiente de demo onde nem todos os módulos
 * precisam estar rodando ao mesmo tempo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecretariaClientService {

    private final RestTemplate restTemplate;

    @Value("${m5.secretarias-api-url}")
    private String secretariasApiUrl;

    /**
     * Mapa de fallback: area_responsavel → {nome completo, sigla}.
     * Reflete as secretarias reais do Recife responsáveis por cada área.
     */
    private static final Map<String, String[]> FALLBACK = Map.of(
            "SEINFRA",  new String[]{"Secretaria de Infraestrutura e Obras",              "SEINFRA"},
            "SEMAS",    new String[]{"Secretaria de Meio Ambiente e Sustentabilidade",     "SEMAS"},
            "SEDESOL",  new String[]{"Secretaria de Desenvolvimento Social e Direitos Humanos", "SEDESOL"},
            "SECEPE",   new String[]{"Secretaria de Educação",                             "SECEPE"},
            "SESAU",    new String[]{"Secretaria de Saúde",                                "SESAU"},
            "SEURB",    new String[]{"Secretaria de Urbanismo",                            "SEURB"},
            "EMLURB",   new String[]{"Empresa de Manutenção e Limpeza Urbana do Recife",  "EMLURB"},
            "SESP",     new String[]{"Secretaria de Serviços Públicos",                   "SESP"}
    );

    public SecretariaDTO buscarPorId(UUID secretariaId) {
        try {
            String url = secretariasApiUrl + "/secretarias/" + secretariaId;
            SecretariaDTO dto = restTemplate.getForObject(url, SecretariaDTO.class);
            if (dto == null) throw new RuntimeException("Secretaria não encontrada: " + secretariaId);
            return dto;
        } catch (Exception e) {
            log.warn("M9 indisponível ao buscar secretaria {}: {}", secretariaId, e.getMessage());
            throw new RuntimeException("Não foi possível obter a secretaria " + secretariaId + ": " + e.getMessage(), e);
        }
    }

    public SecretariaDTO encontrarSecretaria(String area, String categoria) {
        // 1) Tenta buscar no M9 pelo campo area_responsavel
        Optional<SecretariaDTO> encontrada = buscarNoM9("?area=" + area);
        if (encontrada.isPresent()) return encontrada.get();

        // 2) Tenta buscar no M9 pela categoria da denúncia
        if (categoria != null && !categoria.isBlank()) {
            encontrada = buscarNoM9("?categoria=" + categoria);
            if (encontrada.isPresent()) return encontrada.get();
        }

        // 3) Fallback local
        return fallback(area);
    }

    private Optional<SecretariaDTO> buscarNoM9(String queryString) {
        try {
            String url = secretariasApiUrl + "/secretarias" + queryString;
            SecretariaDTO[] resultado = restTemplate.getForObject(url, SecretariaDTO[].class);
            if (resultado != null && resultado.length > 0) {
                return Arrays.stream(resultado)
                        .filter(SecretariaDTO::ativo)
                        .findFirst();
            }
        } catch (Exception e) {
            log.warn("M9 indisponível ({}): {}. Usando fallback.", queryString, e.getMessage());
        }
        return Optional.empty();
    }

    private SecretariaDTO fallback(String area) {
        log.warn("Nenhuma secretaria encontrada no M9 para area='{}' — usando triagem geral.", area);
        return new SecretariaDTO(null, "Triagem Geral (sem órgão definido)", "TRIAGEM",
                List.of(area != null ? area : ""), true);
    }
}
