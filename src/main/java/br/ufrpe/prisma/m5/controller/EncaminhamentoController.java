package br.ufrpe.prisma.m5.controller;

import br.ufrpe.prisma.m5.domain.dto.EncaminhamentoResponseDTO;
import br.ufrpe.prisma.m5.service.EncaminhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EncaminhamentoController {

    private final EncaminhamentoService service;

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> raiz() {
        return ResponseEntity.ok(Map.of(
                "modulo", "M5 - Roteamento e Encaminhamento",
                "docs",   "/swagger-ui.html",
                "health", "/health"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/encaminhamentos")
    public ResponseEntity<List<EncaminhamentoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/encaminhamentos/{id}")
    public ResponseEntity<EncaminhamentoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/encaminhamentos/nivel/{nivel}")
    public ResponseEntity<List<EncaminhamentoResponseDTO>> listarPorNivel(@PathVariable String nivel) {
        return ResponseEntity.ok(service.listarPorNivel(nivel));
    }

    @GetMapping("/encaminhamentos/secretaria/{sigla}")
    public ResponseEntity<List<EncaminhamentoResponseDTO>> listarPorSecretaria(@PathVariable String sigla) {
        return ResponseEntity.ok(service.listarPorSecretaria(sigla));
    }

    @GetMapping("/encaminhamentos/area/{area}")
    public ResponseEntity<List<EncaminhamentoResponseDTO>> listarPorArea(@PathVariable String area) {
        return ResponseEntity.ok(service.listarPorArea(area));
    }

    @PatchMapping("/encaminhamentos/{id}/redirecionar")
    public ResponseEntity<EncaminhamentoResponseDTO> redirecionar(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.redirecionarManualmente(id, UUID.fromString(body.get("secretariaId"))));
    }
}
