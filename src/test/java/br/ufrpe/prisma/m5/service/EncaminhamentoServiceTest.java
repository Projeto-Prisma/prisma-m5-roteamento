package br.ufrpe.prisma.m5.service;

import br.ufrpe.prisma.m5.domain.dto.*;
import br.ufrpe.prisma.m5.domain.enums.StatusEncaminhamento;
import br.ufrpe.prisma.m5.domain.model.Encaminhamento;
import br.ufrpe.prisma.m5.domain.repository.EncaminhamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncaminhamentoServiceTest {

    @Mock
    private EncaminhamentoRepository repository;

    @Mock
    private SecretariaClientService secretariaClient;

    @InjectMocks
    private EncaminhamentoService service;


    private DenunciaPriorizadaEvent eventPadrao() {
        return new DenunciaPriorizadaEvent(
                UUID.randomUUID().toString(),
                0.9,
                "ALTO",
                "FURTO",
                "SEGURANCA",
                1.0,
                1.0,
                1.0,
                LocalDateTime.now().toString()
        );
    }

    private SecretariaDTO secretariaPadrao() {
        return new SecretariaDTO(
                UUID.randomUUID(),
                "Secretaria Teste",
                "ST",
                List.of("SEGURANCA"),
                true
        );
    }


    @Test
    void deveProcessarDenunciaComSucesso() {

        var event = eventPadrao();
        var secretaria = secretariaPadrao();

        when(repository.existsById(any())).thenReturn(false);
        when(secretariaClient.encontrarSecretaria(any(), any()))
                .thenReturn(secretaria);

        service.processar(event);

        verify(repository).save(any(Encaminhamento.class));
    }


    @Test
    void naoDeveProcessarSeJaExistir() {

        var event = eventPadrao();

        when(repository.existsById(any())).thenReturn(true);

        service.processar(event);

        verify(repository, never()).save(any());
    }


    @Test
    void devePersistirMesmoComFalhaNoRabbit() {

        var event = eventPadrao();
        var secretaria = secretariaPadrao();

        when(repository.existsById(any())).thenReturn(false);
        when(secretariaClient.encontrarSecretaria(any(), any()))
                .thenReturn(secretaria);

        assertDoesNotThrow(() -> service.processar(event));

        verify(repository).save(any(Encaminhamento.class));
    }


    @Test
    void deveBuscarPorId() {

        UUID id = UUID.randomUUID();

        Encaminhamento enc = Encaminhamento.builder()
                .id(id)
                .secretariaNome("Secretaria Teste")
                .secretariaSigla("ST")
                .status(StatusEncaminhamento.ENCAMINHADA)
                .score(0.9)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(enc));

        var result = service.buscarPorId(id);

        assertEquals(id, result.id());
        assertEquals("Secretaria Teste", result.secretariaNome());
    }


    @Test
    void deveLancarErroSeNaoEncontrar() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.buscarPorId(id));
    }


    @Test
    void deveRedirecionarManualmente() {

        UUID id = UUID.randomUUID();
        UUID secId = UUID.randomUUID();

        Encaminhamento enc = Encaminhamento.builder()
                .id(id)
                .secretariaNome("Antiga")
                .secretariaSigla("AN")
                .score(0.9)
                .build();

        SecretariaDTO nova = secretariaPadrao();

        when(repository.findById(id)).thenReturn(Optional.of(enc));
        when(secretariaClient.buscarPorId(secId)).thenReturn(nova);

        var result = service.redirecionarManualmente(id, secId);

        assertEquals("Secretaria Teste", result.secretariaNome());
        assertEquals("ST", result.secretariaSigla());

        verify(repository).save(enc);
    }


    @Test
    void deveListarPorNivel() {

        Encaminhamento enc = Encaminhamento.builder()
                .id(UUID.randomUUID())
                .nivel("ALTO")
                .score(0.9)
                .build();

        when(repository.findByNivelIgnoreCase("ALTO"))
                .thenReturn(List.of(enc));

        var result = service.listarPorNivel("ALTO");

        assertEquals(1, result.size());
    }

    @Test
    void deveListarPorSecretaria() {

        Encaminhamento enc = Encaminhamento.builder()
                .id(UUID.randomUUID())
                .secretariaSigla("ST")
                .score(0.9)
                .build();

        when(repository.findBySecretariaSiglaIgnoreCase("ST"))
                .thenReturn(List.of(enc));

        var result = service.listarPorSecretaria("ST");

        assertEquals(1, result.size());
    }

    @Test
    void deveListarPorArea() {

        Encaminhamento enc = Encaminhamento.builder()
                .id(UUID.randomUUID())
                .areaResponsavel("SEGURANCA")
                .score(0.9)
                .build();

        when(repository.findByAreaResponsavelIgnoreCase("SEGURANCA"))
                .thenReturn(List.of(enc));

        var result = service.listarPorArea("SEGURANCA");

        assertEquals(1, result.size());
    }
}