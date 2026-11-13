package br.com.keyfalls.inscricao.domain;

import br.com.keyfalls.shared.domain.CandidatoId;
import br.com.keyfalls.shared.domain.ConcursoId;
import br.com.keyfalls.shared.domain.InscricaoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {

    boolean existsByCandidatoIdAndConcursoIdAndStatusNot(UUID candidatoId, UUID concursoId, StatusInscricao status);

    List<Inscricao> findByConcursoIdAndStatus(UUID concursoId, StatusInscricao status);

    List<Inscricao> findByConcursoId(UUID concursoId);

    Optional<Inscricao> findByCandidatoIdAndConcursoId(UUID candidatoId, UUID concursoId);

    default void salvar(Inscricao inscricao) {
        saveAndFlush(inscricao);
    }

    default Optional<Inscricao> buscarPorId(InscricaoId id) {
        return findById(id.valor());
    }

    default boolean existeInscricaoAtiva(CandidatoId candidatoId, ConcursoId concursoId) {
        return existsByCandidatoIdAndConcursoIdAndStatusNot(
            candidatoId.valor(),
            concursoId.valor(),
            StatusInscricao.CANCELADA
        );
    }

    default List<Inscricao> listarHomologadasPorConcurso(ConcursoId concursoId) {
        return findByConcursoIdAndStatus(concursoId.valor(), StatusInscricao.HOMOLOGADA);
    }
}
