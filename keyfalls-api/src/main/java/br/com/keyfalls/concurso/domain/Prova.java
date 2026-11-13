package br.com.keyfalls.concurso.domain;

import br.com.keyfalls.shared.domain.Alternativa;
import br.com.keyfalls.shared.domain.CargoId;
import br.com.keyfalls.shared.domain.Peso;
import br.com.keyfalls.shared.domain.ProvaId;
import br.com.keyfalls.shared.domain.QuestaoId;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "provas")
public class Prova {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cargo_id", nullable = false)
    private UUID cargoId;

    @Column(name = "disciplina", nullable = false)
    private String disciplina;

    @Embedded
    private Peso peso;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "prova_id", nullable = false)
    @OrderBy("numeroOrdem ASC")
    private List<Questao> questoes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prova_gabarito_respostas", joinColumns = @JoinColumn(name = "prova_id"))
    @MapKeyColumn(name = "questao_id")
    @Enumerated(EnumType.STRING)
    @Column(name = "alternativa", nullable = false, length = 5)
    private Map<UUID, Alternativa> gabaritoRespostasPersistidas = new LinkedHashMap<>();

    @Column(name = "gabarito_definitivo", nullable = false)
    private boolean gabaritoDefinitivo = false;

    @Transient
    private Gabarito gabarito;

    protected Prova() {
    }

    Prova(ProvaId id, CargoId cargoId, String disciplina, Peso peso) {
        this.id = Objects.requireNonNull(id, "ProvaId obrigatorio.").valor();
        this.cargoId = Objects.requireNonNull(cargoId, "CargoId obrigatorio.").valor();
        if (disciplina == null || disciplina.isBlank()) {
            throw new RegraDeNegocioException("Disciplina/titulo da prova obrigatorio.");
        }
        this.disciplina = disciplina.trim();
        this.peso = Objects.requireNonNull(peso, "Peso da prova obrigatorio.");
        this.questoes = new ArrayList<>();
    }

    Questao adicionarQuestao(QuestaoId questaoId, int numeroOrdem, String enunciado, Map<Alternativa, String> opcoes) {
        if (gabarito() != null) {
            throw new RegraDeNegocioException("Nao e permitido incluir questoes apos a publicacao do gabarito.");
        }
        boolean numeroDuplicado = questoes.stream().anyMatch(q -> q.numeroOrdem() == numeroOrdem);
        if (numeroDuplicado) {
            throw new RegraDeNegocioException("Ja existe questao com numero de ordem " + numeroOrdem + " nesta prova.");
        }
        Questao nova = new Questao(questaoId, numeroOrdem, enunciado, opcoes);
        this.questoes.add(nova);
        return nova;
    }

    Gabarito publicarGabaritoPreliminar(Map<QuestaoId, Alternativa> respostas) {
        if (questoes.isEmpty()) {
            throw new RegraDeNegocioException("A prova deve possuir questoes antes da publicacao do gabarito.");
        }
        if (respostas.size() != questoes.size()) {
            throw new RegraDeNegocioException("O gabarito deve contemplar exatamente todas as questoes da prova.");
        }
        for (Questao questao : questoes) {
            Alternativa resposta = respostas.get(questao.id());
            if (resposta == null || !questao.possuiAlternativa(resposta)) {
                throw new RegraDeNegocioException("Alternativa de gabarito invalida ou ausente para a questao " + questao.numeroOrdem());
            }
        }
        this.gabarito = new Gabarito(respostas);
        sincronizarGabaritoPersistido();
        return this.gabarito;
    }

    void anularQuestao(QuestaoId questaoId, String justificativa) {
        Gabarito gab = gabarito();
        if (gab != null && gab.isDefinitivo()) {
            throw new RegraDeNegocioException("Nao e permitido anular questao apos consolidacao do gabarito definitivo.");
        }
        buscarQuestao(questaoId).anular(justificativa);
    }

    void retificarGabaritoQuestao(QuestaoId questaoId, Alternativa novaAlternativa) {
        Gabarito gab = gabarito();
        if (gab == null) {
            throw new RegraDeNegocioException("Gabarito ainda nao publicado para esta prova.");
        }
        Questao questao = buscarQuestao(questaoId);
        if (questao.isAnulada()) {
            throw new RegraDeNegocioException("Nao e permitido alterar alternativa de uma questao anulada.");
        }
        if (!questao.possuiAlternativa(novaAlternativa)) {
            throw new RegraDeNegocioException("A questao nao possui a alternativa informada.");
        }
        gab.retificarResposta(questaoId, novaAlternativa);
        sincronizarGabaritoPersistido();
    }

    void consolidarGabaritoDefinitivo() {
        Gabarito gab = gabarito();
        if (gab == null) {
            throw new RegraDeNegocioException("Nao ha gabarito preliminar publicado para consolidar.");
        }
        gab.consolidarDefinitivo();
        sincronizarGabaritoPersistido();
    }

    private void sincronizarGabaritoPersistido() {
        this.gabaritoRespostasPersistidas.clear();
        if (this.gabarito != null) {
            this.gabarito.respostasOficiais().forEach((qId, alt) -> this.gabaritoRespostasPersistidas.put(qId.valor(), alt));
            this.gabaritoDefinitivo = this.gabarito.isDefinitivo();
        }
    }

    private Questao buscarQuestao(QuestaoId questaoId) {
        return questoes.stream()
            .filter(q -> q.id().equals(questaoId))
            .findFirst()
            .orElseThrow(() -> new RegraDeNegocioException("Questao nao encontrada nesta prova."));
    }

    public ProvaId id() {
        return new ProvaId(id);
    }

    public UUID getId() {
        return id;
    }

    public CargoId cargoId() {
        return new CargoId(cargoId);
    }

    public String disciplina() {
        return disciplina;
    }

    public Peso peso() {
        return peso;
    }

    public List<Questao> questoes() {
        return Collections.unmodifiableList(questoes);
    }

    public Gabarito gabarito() {
        if (gabarito == null && !gabaritoRespostasPersistidas.isEmpty()) {
            Map<QuestaoId, Alternativa> hidratado = new LinkedHashMap<>();
            gabaritoRespostasPersistidas.forEach((uuid, alt) -> hidratado.put(new QuestaoId(uuid), alt));
            gabarito = new Gabarito(hidratado, gabaritoDefinitivo);
        }
        return gabarito;
    }
}
