package br.com.keyfalls.identidade.domain;

import br.com.keyfalls.shared.domain.AggregateRoot;
import br.com.keyfalls.shared.domain.CPF;
import br.com.keyfalls.shared.domain.CandidatoId;
import br.com.keyfalls.shared.domain.Email;
import br.com.keyfalls.shared.domain.Endereco;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "candidatos")
public class Candidato extends AggregateRoot<CandidatoId> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "cpf", nullable = false, unique = true, length = 11))
    private CPF cpf;

    @Column(name = "nome_completo", nullable = false, length = 180)
    private String nomeCompleto;

    @Embedded
    @AttributeOverride(name = "endereco", column = @Column(name = "email", nullable = false, unique = true, length = 255))
    private Email email;

    @Embedded
    private Endereco endereco;

    @Embedded
    private Credencial credencial;

    protected Candidato() {
    }

    private Candidato(
        CandidatoId id,
        String nomeCompleto,
        CPF cpf,
        Email email,
        Endereco endereco,
        Credencial credencial
    ) {
        this.id = Objects.requireNonNull(id, "CandidatoId obrigatorio.").valor();
        if (nomeCompleto == null || nomeCompleto.trim().length() < 3) {
            throw new RegraDeNegocioException("Nome completo do candidato deve possuir ao menos 3 caracteres.");
        }
        this.nomeCompleto = nomeCompleto.trim();
        this.cpf = Objects.requireNonNull(cpf, "CPF do candidato obrigatorio.");
        this.email = Objects.requireNonNull(email, "E-mail do candidato obrigatorio.");
        this.endereco = Objects.requireNonNull(endereco, "Endereco do candidato obrigatorio.");
        this.credencial = Objects.requireNonNull(credencial, "Credencial do candidato obrigatoria.");
    }

    public static Candidato cadastrar(
        CandidatoId id,
        String nomeCompleto,
        CPF cpf,
        Email email,
        Endereco endereco,
        String senhaHash,
        Instant agora
    ) {
        return new Candidato(id, nomeCompleto, cpf, email, endereco, Credencial.ativa(senhaHash, agora));
    }

    public static Candidato reconstituir(
        CandidatoId id,
        String nomeCompleto,
        CPF cpf,
        Email email,
        Endereco endereco,
        Credencial credencial
    ) {
        return new Candidato(id, nomeCompleto, cpf, email, endereco, credencial);
    }

    public void atualizarDadosContato(Email novoEmail, Endereco novoEndereco) {
        this.email = Objects.requireNonNull(novoEmail, "Novo e-mail obrigatorio.");
        this.endereco = Objects.requireNonNull(novoEndereco, "Novo endereco obrigatorio.");
    }

    public void redefinirSenha(String novoSenhaHash, Instant agora) {
        if (credencial.bloqueada()) {
            throw new RegraDeNegocioException("Nao e permitido redefinir senha de conta bloqueada.");
        }
        this.credencial = Credencial.ativa(novoSenhaHash, agora);
    }

    public void bloquearAcesso(Instant agora) {
        this.credencial = this.credencial.bloquear(agora);
    }

    @Override
    public CandidatoId id() {
        return new CandidatoId(id);
    }

    public UUID getId() {
        return id;
    }

    public String nomeCompleto() {
        return nomeCompleto;
    }

    public CPF cpf() {
        return cpf;
    }

    public Email email() {
        return email;
    }

    public Endereco endereco() {
        return endereco;
    }

    public Credencial credencial() {
        return credencial;
    }
}
