package br.ufrpe.prisma.m5.controller;

import br.ufrpe.prisma.m5.domain.dto.EncaminhamentoResponseDTO;
import br.ufrpe.prisma.m5.service.EncaminhamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EncaminhamentoController.class)
class EncaminhamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EncaminhamentoService service;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------
    // ROOT
    // -------------------------

    @Test
    void deveRetornarRaiz() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulo").value("M5 - Roteamento e Encaminhamento"))
                .andExpect(jsonPath("$.docs").value("/swagger-ui.html"))
                .andExpect(jsonPath("$.health").value("/health"));
    }

    // -------------------------
    // HEALTH
    // -------------------------

    @Test
    void deveRetornarHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // -------------------------
    // LISTAR TODOS
    // -------------------------

    @Test
    void deveListarTodos() throws Exception {

        when(service.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/encaminhamentos"))
                .andExpect(status().isOk());

        verify(service).listarTodos();
    }

    // -------------------------
    // BUSCAR POR ID
    // -------------------------

    @Test
    void deveBuscarPorId() throws Exception {

        UUID id = UUID.randomUUID();

        EncaminhamentoResponseDTO dto = mock(EncaminhamentoResponseDTO.class);

        when(service.buscarPorId(id)).thenReturn(dto);

        mockMvc.perform(get("/encaminhamentos/" + id))
                .andExpect(status().isOk());

        verify(service).buscarPorId(id);
    }

    // -------------------------
    // FILTRO NIVEL
    // -------------------------

    @Test
    void deveListarPorNivel() throws Exception {

        when(service.listarPorNivel("ALTO")).thenReturn(List.of());

        mockMvc.perform(get("/encaminhamentos/nivel/ALTO"))
                .andExpect(status().isOk());

        verify(service).listarPorNivel("ALTO");
    }

    // -------------------------
    // FILTRO SECRETARIA
    // -------------------------

    @Test
    void deveListarPorSecretaria() throws Exception {

        when(service.listarPorSecretaria("ST")).thenReturn(List.of());

        mockMvc.perform(get("/encaminhamentos/secretaria/ST"))
                .andExpect(status().isOk());

        verify(service).listarPorSecretaria("ST");
    }

    // -------------------------
    // FILTRO AREA
    // -------------------------

    @Test
    void deveListarPorArea() throws Exception {

        when(service.listarPorArea("SEGURANCA")).thenReturn(List.of());

        mockMvc.perform(get("/encaminhamentos/area/SEGURANCA"))
                .andExpect(status().isOk());

        verify(service).listarPorArea("SEGURANCA");
    }

    // -------------------------
    // REDIRECIONAMENTO
    // -------------------------

    @Test
    void deveRedirecionar() throws Exception {

        UUID id = UUID.randomUUID();
        UUID secId = UUID.randomUUID();

        EncaminhamentoResponseDTO dto = mock(EncaminhamentoResponseDTO.class);

        when(service.redirecionarManualmente(id, secId)).thenReturn(dto);

        mockMvc.perform(patch("/encaminhamentos/" + id + "/redirecionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("secretariaId", secId.toString())
                        )))
                .andExpect(status().isOk());

        verify(service).redirecionarManualmente(id, secId);
    }
}