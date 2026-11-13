package br.com.keyfalls.concurso.domain;

import br.com.keyfalls.shared.domain.ConcursoId;
import br.com.keyfalls.shared.domain.ProvaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConcursoRepository extends JpaRepository<Concurso, UUID> {

    @Query("SELECT DISTINCT c FROM Concurso c JOIN c.provas p WHERE p.id = :provaUuid")
    Optional<Concurso> findByProvaUuid(@Param("provaUuid") UUID provaUuid);

    default void salvar(Concurso concurso) {
        saveAndFlush(concurso);
    }

    default Optional<Concurso> buscarPorId(ConcursoId id) {
        return findById(id.valor());
    }

    default Optional<Concurso> buscarPorProvaId(ProvaId provaId) {
        return findByProvaUuid(provaId.valor());
    }
}
