package br.ufrpe.prisma.m5.domain.repository;

import br.ufrpe.prisma.m5.domain.model.Encaminhamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, UUID> {

    List<Encaminhamento> findByNivelIgnoreCase(String nivel);

    List<Encaminhamento> findBySecretariaSiglaIgnoreCase(String sigla);

    List<Encaminhamento> findByAreaResponsavelIgnoreCase(String area);
}
