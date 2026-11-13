package br.com.keyfalls.concurso.domain;

import br.com.keyfalls.shared.domain.CargoId;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cargos")
public class Cargo {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "vagas", nullable = false)
    private int vagas;

    @Column(name = "taxa_inscricao", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaInscricao;

    protected Cargo() {
    }

    Cargo(CargoId id, String nome, int vagas, BigDecimal taxaInscricao) {
        this.id = Objects.requireNonNull(id, "CargoId obrigatorio.").valor();
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("Nome do cargo obrigatorio.");
        }
        if (vagas <= 0) {
            throw new RegraDeNegocioException("Quantidade de vagas do cargo deve ser maior que zero.");
        }
        Objects.requireNonNull(taxaInscricao, "Taxa de inscricao obrigatoria.");
        if (taxaInscricao.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Taxa de inscricao nao pode ser negativa.");
        }
        this.nome = nome.trim();
        this.vagas = vagas;
        this.taxaInscricao = taxaInscricao.setScale(2, RoundingMode.HALF_UP);
    }

    public CargoId id() {
        return new CargoId(id);
    }

    public UUID getId() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public int vagas() {
        return vagas;
    }

    public BigDecimal taxaInscricao() {
        return taxaInscricao;
    }
}
