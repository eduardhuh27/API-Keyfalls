package br.com.keyfalls.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public record Peso(
    @Column(name = "peso", nullable = false, precision = 10, scale = 2)
    BigDecimal valor
) {

    public Peso {
        Objects.requireNonNull(valor, "Peso obrigatorio.");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Peso deve ser estritamente maior que zero.");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Peso de(String valor) {
        return new Peso(new BigDecimal(valor));
    }

    public static Peso de(int valor) {
        return new Peso(BigDecimal.valueOf(valor));
    }
}
