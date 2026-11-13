package br.com.keyfalls.concurso.domain;

import br.com.keyfalls.shared.domain.Alternativa;
import br.com.keyfalls.shared.domain.QuestaoId;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "questoes")
public class Questao {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero_ordem", nullable = false)
    private int numeroOrdem;

    @Column(name = "enunciado", nullable = false, length = 2000)
    private String enunciado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "questao_opcoes", joinColumns = @JoinColumn(name = "questao_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "alternativa", length = 5)
    @Column(name = "texto", nullable = false, length = 1000)
    private Map<Alternativa, String> opcoes = new EnumMap<>(Alternativa.class);

    @Column(name = "anulada", nullable = false)
    private boolean anulada;

    @Column(name = "justificativa_anulacao", length = 1000)
    private String justificativaAnulacao;

    protected Questao() {
    }

    Questao(QuestaoId id, int numeroOrdem, String enunciado, Map<Alternativa, String> opcoes) {
        this.id = Objects.requireNonNull(id, "QuestaoId obrigatorio.").valor();
        if (numeroOrdem <= 0) {
            throw new RegraDeNegocioException("Numero de ordem da questao deve ser positivo.");
        }
        if (enunciado == null || enunciado.isBlank()) {
            throw new RegraDeNegocioException("Enunciado da questao obrigatorio.");
        }
        Objects.requireNonNull(opcoes, "Opcoes da questao obrigatorias.");
        if (opcoes.size() < 2) {
            throw new RegraDeNegocioException("A questao deve possuir ao menos 2 alternativas.");
        }
        Map<Alternativa, String> copia = new EnumMap<>(Alternativa.class);
        opcoes.forEach((letra, texto) -> {
            Objects.requireNonNull(letra, "Letra da alternativa obrigatoria.");
            if (texto == null || texto.isBlank()) {
                throw new RegraDeNegocioException("Texto da alternativa " + letra + " nao pode ser vazio.");
            }
            copia.put(letra, texto.trim());
        });

        this.numeroOrdem = numeroOrdem;
        this.enunciado = enunciado.trim();
        this.opcoes = copia;
        this.anulada = false;
    }

    void anular(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("Justificativa de anulacao da questao obrigatoria.");
        }
        if (this.anulada) {
            throw new RegraDeNegocioException("A questao " + numeroOrdem + " ja encontra-se anulada.");
        }
        this.anulada = true;
        this.justificativaAnulacao = justificativa.trim();
    }

    public boolean possuiAlternativa(Alternativa alternativa) {
        return opcoes.containsKey(alternativa);
    }

    public QuestaoId id() {
        return new QuestaoId(id);
    }

    public UUID getId() {
        return id;
    }

    public int numeroOrdem() {
        return numeroOrdem;
    }

    public String enunciado() {
        return enunciado;
    }

    public Map<Alternativa, String> opcoes() {
        return Collections.unmodifiableMap(opcoes);
    }

    public boolean isAnulada() {
        return anulada;
    }

    public String justificativaAnulacao() {
        return justificativaAnulacao;
    }
}
