package br.com.keyfalls.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public record NumeroInscricao(
    @Column(name = "numero_inscricao", nullable = false, unique = true, length = 20)
    String codigo
) {

    private static final Pattern PADRAO_INSCRICAO = Pattern.compile("^\\d{4}\\d{6,10}$");

    public NumeroInscricao {
        Objects.requireNonNull(codigo, "Numero de inscricao obrigatorio.");
        String limpo = codigo.trim();
        if (!PADRAO_INSCRICAO.matcher(limpo).matches()) {
            throw new RegraDeNegocioException(
                "Numero de inscricao invalido. Esperado ano (4 digitos) seguido de sequencial numerico (6 a 10 digitos)."
            );
        }
        codigo = limpo;
    }
}
