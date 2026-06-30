package br.ufrpe.prisma.m5.service;

import br.ufrpe.prisma.m5.config.RabbitConfig;
import br.ufrpe.prisma.m5.domain.dto.*;
import br.ufrpe.prisma.m5.domain.enums.StatusEncaminhamento;
import br.ufrpe.prisma.m5.domain.model.Encaminhamento;
import br.ufrpe.prisma.m5.domain.repository.EncaminhamentoRepository;
import br.ufrpe.prisma.m5.exception.EncaminhamentoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncaminhamentoService {

    private final EncaminhamentoRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final SecretariaClientService secretariaClient;

    // -------------------------------------------------------------------------
    // Consumidor RabbitMQ
    // -------------------------------------------------------------------------

    /**
     * Processa cada evento denuncia.priorizada recebido do M3.
     *
     * Fluxo:
     *   1. Idempotência — descarta se a denúncia já foi roteada.
     *   2. Consulta o M9 para encontrar a secretaria de destino.
     *   3. Persiste o encaminhamento no banco próprio.
     *   4. Publica denuncia.encaminhada para M6 e M7.
     *
     * Se o publish falhar, o encaminhamento já está gravado no banco
     * e pode ser recuperado manualmente ou via retry manual — seguindo
     * o mesmo padrão simples adotado pelo M1 (sem outbox).
     */
    @RabbitListener(queues = RabbitConfig.FILA_ROTEAMENTO)
    @Transactional
    public void processar(DenunciaPriorizadaEvent evento) {
        UUID denunciaId = UUID.fromString(evento.id());

        // 1) Idempotência
        if (repository.existsById(denunciaId)) {
            log.info("Denuncia {} já foi roteada, ignorando reentrega.", evento.id());
            return;
        }

        // 2) Busca secretaria (M9 com fallback)
        SecretariaDTO secretaria = secretariaClient.encontrarSecretaria(
                evento.area_responsavel(), evento.categoria()
        );

        // 3) Persiste
        Encaminhamento enc = Encaminhamento.builder()
                .id(denunciaId)
                .secretariaId(secretaria.id())
                .secretariaNome(secretaria.nome())
                .secretariaSigla(secretaria.sigla())
                .nivel(evento.nivel())
                .categoria(evento.categoria())
                .areaResponsavel(evento.area_responsavel())
                .score(evento.score())
                .status(StatusEncaminhamento.ENCAMINHADA)
                .priorizadaEm(parseDatetime(evento.priorizado_em()))
                .encaminhadaEm(LocalDateTime.now())
                .build();

        repository.save(enc);

        // 4) Publica denuncia.encaminhada
        var encaminhadaEvent = new DenunciaEncaminhadaEvent(
                evento.id(),
                secretaria.id() != null ? secretaria.id().toString() : null,
                secretaria.nome(),
                secretaria.sigla(),
                evento.nivel(),
                evento.categoria(),
                evento.area_responsavel(),
                evento.score(),
                enc.getEncaminhadaEm().toString()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_DENUNCIAS,
                    RabbitConfig.ROUTING_KEY_OUT,
                    encaminhadaEvent
            );
            log.info("denuncia {} -> {} [{}] | nivel={} score={}",
                    evento.id(), secretaria.nome(), secretaria.sigla(),
                    evento.nivel(), String.format("%.1f", evento.score()));
        } catch (Exception e) {
            log.error("Falha ao publicar denuncia.encaminhada para {}: {}", evento.id(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // API HTTP (observabilidade)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EncaminhamentoResponseDTO> listarTodos() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public EncaminhamentoResponseDTO buscarPorId(UUID id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Encaminhamento não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoResponseDTO> listarPorNivel(String nivel) {
        return repository.findByNivelIgnoreCase(nivel).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoResponseDTO> listarPorSecretaria(String sigla) {
        return repository.findBySecretariaSiglaIgnoreCase(sigla).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoResponseDTO> listarPorArea(String area) {
        return repository.findByAreaResponsavelIgnoreCase(area).stream().map(this::toDTO).toList();
    }

    // -------------------------------------------------------------------------
    // Redirecionamento manual (corrige encaminhamentos em Triagem Geral)
    // -------------------------------------------------------------------------

    @Transactional
    public EncaminhamentoResponseDTO redirecionarManualmente(UUID id, UUID secretariaId) {
        Encaminhamento enc = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Encaminhamento não encontrado: " + id));
        SecretariaDTO secretaria = secretariaClient.buscarPorId(secretariaId);
        enc.setSecretariaId(secretaria.id());
        enc.setSecretariaNome(secretaria.nome());
        enc.setSecretariaSigla(secretaria.sigla());
        repository.save(enc);
        log.info("Encaminhamento {} redirecionado manualmente para {} [{}]",
                id, secretaria.nome(), secretaria.sigla());
        return toDTO(enc);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EncaminhamentoResponseDTO toDTO(Encaminhamento e) {
        return new EncaminhamentoResponseDTO(
                e.getId(), e.getSecretariaId(), e.getSecretariaNome(),
                e.getSecretariaSigla(), e.getNivel(), e.getCategoria(),
                e.getAreaResponsavel(), e.getScore(), e.getStatus(),
                e.getPriorizadaEm(), e.getEncaminhadaEm()
        );
    }

    private LocalDateTime parseDatetime(String dt) {
        if (dt == null || dt.isBlank()) return null;
        try {
            return OffsetDateTime.parse(dt).toLocalDateTime();
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dt);
            } catch (Exception e2) {
                log.warn("Não foi possível parsear datetime: {}", dt);
                return null;
            }
        }
    }
}
