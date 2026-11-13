package br.com.keyfalls.identidade.domain;

import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

@Embeddable
public record Credencial(
    @Column(name = "senha_hash", nullable = false, length = 255)
    String senhaHash,
    @Column(name = "bloqueada", nullable = false)
    boolean bloqueada,
    @Column(name = "credencial_atualizada_em", nullable = false)
    Instant atualizadaEm
) {

    public Credencial {
        if (senhaHash == null || senhaHash.isBlank() || senhaHash.trim().length() < 20) {
            throw new RegraDeNegocioException("Hash de senha invalido para credencial.");
        }
        Objects.requireNonNull(atualizadaEm, "Data de atualizacao da credencial obrigatoria.");
        senhaHash = senhaHash.trim();
    }

    public static Credencial ativa(String senhaHash, Instant agora) {
        return new Credencial(senhaHash, false, agora);
    }

    public Credencial bloquear(Instant agora) {
        return new Credencial(this.senhaHash, true, agora);
    }
}
