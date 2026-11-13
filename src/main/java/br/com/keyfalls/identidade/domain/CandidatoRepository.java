package br.com.keyfalls.identidade.domain;

import br.com.keyfalls.shared.domain.CPF;
import br.com.keyfalls.shared.domain.CandidatoId;
import br.com.keyfalls.shared.domain.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidato, UUID> {

    Optional<Candidato> findByCpfNumero(String numero);

    Optional<Candidato> findByEmailEnderecoIgnoreCase(String endereco);

    default void salvar(Candidato candidato) {
        saveAndFlush(candidato);
    }

    default Optional<Candidato> buscarPorId(CandidatoId id) {
        return findById(id.valor());
    }

    default Optional<Candidato> buscarPorCpf(CPF cpf) {
        return findByCpfNumero(cpf.numero());
    }

    default Optional<Candidato> buscarPorEmail(Email email) {
        return findByEmailEnderecoIgnoreCase(email.endereco());
    }
}
