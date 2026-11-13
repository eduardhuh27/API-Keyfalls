package br.com.keyfalls.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

@Embeddable
public record Periodo(
    @Column(name = "inicio", nullable = false)
    Instant inicio,
    @Column(name = "fim", nullable = false)
    Instant fim
) {

    public Periodo {
        Objects.requireNonNull(inicio, "Data/hora de inicio do periodo obrigatoria.");
        Objects.requireNonNull(fim, "Data/hora de fim do periodo obrigatoria.");
        if (!inicio.isBefore(fim)) {
            throw new RegraDeNegocioException("O inicio do periodo deve ser estritamente anterior ao fim.");
        }
    }

    public boolean contemEstritamente(Instant instante) {
        Objects.requireNonNull(instante, "Instante para verificacao de periodo obrigatorio.");
        return instante.isAfter(inicio) && instante.isBefore(fim);
    }
}
