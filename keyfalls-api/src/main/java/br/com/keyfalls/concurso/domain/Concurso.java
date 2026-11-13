package br.com.keyfalls.concurso.domain;

import br.com.keyfalls.concurso.domain.event.GabaritoPublicado;
import br.com.keyfalls.concurso.domain.event.QuestaoAnulada;
import br.com.keyfalls.shared.domain.AggregateRoot;
import br.com.keyfalls.shared.domain.Alternativa;
import br.com.keyfalls.shared.domain.CargoId;
import br.com.keyfalls.shared.domain.ConcursoId;
import br.com.keyfalls.shared.domain.ConfiguracaoRegrasEdital;
import br.com.keyfalls.shared.domain.Periodo;
import br.com.keyfalls.shared.domain.Peso;
import br.com.keyfalls.shared.domain.PoliticaAnulacaoQuestao;
import br.com.keyfalls.shared.domain.ProvaId;
import br.com.keyfalls.shared.domain.QuestaoId;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "concursos")
public class Concurso extends AggregateRoot<ConcursoId> {

    public enum Abrangencia {
        NACIONAL, ESTADUAL, MUNICIPAL
    }

    public enum StatusConcurso {
        EDITAL_ABERTO,
        EM_FASE_RECURSAL,
        RESULTADO_FINAL_HOMOLOGADO
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "codigo_edital", nullable = false, unique = true, length = 50)
    private String codigoEdital;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "abrangencia", nullable = false, length = 20)
    private Abrangencia abrangencia;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "inicio", column = @Column(name = "periodo_inscricao_inicio", nullable = false)),
        @AttributeOverride(name = "fim", column = @Column(name = "periodo_inscricao_fim", nullable = false))
    })
    private Periodo periodoInscricao;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "inicio", column = @Column(name = "periodo_isencao_inicio", nullable = false)),
        @AttributeOverride(name = "fim", column = @Column(name = "periodo_isencao_fim", nullable = false))
    })
    private Periodo periodoIsencao;

    @Column(name = "politica_anulacao", nullable = false, length = 40)
    private String codigoPoliticaAnulacao;

    @Transient
    private ConfiguracaoRegrasEdital configuracaoRegrasEdital;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusConcurso status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "concurso_id", nullable = false)
    private List<Cargo> cargos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "concurso_id", nullable = false)
    private List<Prova> provas = new ArrayList<>();

    protected Concurso() {
    }

    private Concurso(
        ConcursoId id,
        String codigoEdital,
        String titulo,
        Abrangencia abrangencia,
        Periodo periodoInscricao,
        Periodo periodoIsencao,
        ConfiguracaoRegrasEdital configuracaoRegrasEdital
    ) {
        this.id = Objects.requireNonNull(id, "ConcursoId obrigatorio.").valor();
        if (codigoEdital == null || codigoEdital.isBlank()) {
            throw new RegraDeNegocioException("Codigo do edital obrigatorio.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("Titulo do concurso obrigatorio.");
        }
        this.abrangencia = Objects.requireNonNull(abrangencia, "Abrangencia do concurso obrigatoria.");
        this.periodoInscricao = Objects.requireNonNull(periodoInscricao, "Periodo de inscricao obrigatorio.");
        this.periodoIsencao = Objects.requireNonNull(periodoIsencao, "Periodo de isencao obrigatorio.");
        if (periodoIsencao.inicio().isBefore(periodoInscricao.inicio())
            || periodoIsencao.fim().isAfter(periodoInscricao.fim())) {
            throw new RegraDeNegocioException("O periodo de isencao deve estar contido dentro do periodo de inscricao.");
        }
        this.configuracaoRegrasEdital = Objects.requireNonNull(
            configuracaoRegrasEdital,
            "Configuracao parametrizada de regras do edital obrigatoria."
        );
        this.codigoPoliticaAnulacao = extrairCodigoPolitica(configuracaoRegrasEdital.politicaAnulacaoQuestao());
        this.codigoEdital = codigoEdital.trim();
        this.titulo = titulo.trim();
        this.status = StatusConcurso.EDITAL_ABERTO;
        this.cargos = new ArrayList<>();
        this.provas = new ArrayList<>();
    }

    public static Concurso abrirEditalComConfiguracao(
        ConcursoId id,
        String codigoEdital,
        String titulo,
        Abrangencia abrangencia,
        Periodo periodoInscricao,
        Periodo periodoIsencao,
        ConfiguracaoRegrasEdital configuracaoRegrasEdital
    ) {
        return new Concurso(
            id, codigoEdital, titulo, abrangencia, periodoInscricao, periodoIsencao, configuracaoRegrasEdital
        );
    }

    public static Concurso abrirEdital(
        ConcursoId id,
        String codigoEdital,
        String titulo,
        Abrangencia abrangencia,
        Periodo periodoInscricao,
        Periodo periodoIsencao,
        PoliticaAnulacaoQuestao politicaAnulacaoQuestao
    ) {
        ConfiguracaoRegrasEdital customizada = criarConfiguracaoComPolitica(
            Objects.requireNonNull(politicaAnulacaoQuestao, "Politica de anulacao de questao obrigatoria.")
        );
        return new Concurso(id, codigoEdital, titulo, abrangencia, periodoInscricao, periodoIsencao, customizada);
    }

    public Cargo adicionarCargo(CargoId cargoId, String nome, int vagas, BigDecimal taxaInscricao) {
        exigirConcursoNaoHomologado();
        boolean duplicado = cargos.stream()
            .anyMatch(c -> c.id().equals(cargoId) || c.nome().equalsIgnoreCase(nome));
        if (duplicado) {
            throw new RegraDeNegocioException("Cargo ja cadastrado neste concurso: " + nome);
        }
        Cargo cargo = new Cargo(cargoId, nome, vagas, taxaInscricao);
        this.cargos.add(cargo);
        return cargo;
    }

    public Prova configurarProva(ProvaId provaId, CargoId cargoId, String disciplina, Peso peso) {
        exigirConcursoNaoHomologado();
        validarExistenciaCargo(cargoId);
        Prova prova = new Prova(provaId, cargoId, disciplina, peso);
        this.provas.add(prova);
        return prova;
    }

    public Questao incluirQuestaoNaProva(
        ProvaId provaId,
        QuestaoId questaoId,
        int numeroOrdem,
        String enunciado,
        Map<Alternativa, String> opcoes
    ) {
        exigirConcursoNaoHomologado();
        return buscarProva(provaId).adicionarQuestao(questaoId, numeroOrdem, enunciado, opcoes);
    }

    public Gabarito publicarGabaritoPreliminar(ProvaId provaId, Map<QuestaoId, Alternativa> respostas, Instant publicadoEm) {
        exigirConcursoNaoHomologado();
        Prova prova = buscarProva(provaId);
        Gabarito gabarito = prova.publicarGabaritoPreliminar(respostas);
        this.status = StatusConcurso.EM_FASE_RECURSAL;
        registerEvent(GabaritoPublicado.novo(this.id(), prova.cargoId(), provaId, false, publicadoEm));
        return gabarito;
    }

    public void anularQuestao(ProvaId provaId, QuestaoId questaoId, String justificativa, Instant anuladoEm) {
        exigirConcursoNaoHomologado();
        buscarProva(provaId).anularQuestao(questaoId, justificativa);
        registerEvent(QuestaoAnulada.novo(this.id(), provaId, questaoId, justificativa, anuladoEm));
    }

    public void retificarGabarito(ProvaId provaId, QuestaoId questaoId, Alternativa novaAlternativa) {
        exigirConcursoNaoHomologado();
        buscarProva(provaId).retificarGabaritoQuestao(questaoId, novaAlternativa);
    }

    public void consolidarGabaritoDefinitivo(ProvaId provaId, Instant consolidadoEm) {
        exigirConcursoNaoHomologado();
        Prova prova = buscarProva(provaId);
        prova.consolidarGabaritoDefinitivo();
        registerEvent(GabaritoPublicado.novo(this.id(), prova.cargoId(), provaId, true, consolidadoEm));
    }

    public void atualizarConfiguracaoRegrasEdital(ConfiguracaoRegrasEdital novaConfiguracao) {
        exigirConcursoNaoHomologado();
        this.configuracaoRegrasEdital = Objects.requireNonNull(novaConfiguracao, "Configuracao do edital obrigatoria.");
        this.codigoPoliticaAnulacao = extrairCodigoPolitica(novaConfiguracao.politicaAnulacaoQuestao());
    }

    public void definirPoliticaAnulacaoQuestao(PoliticaAnulacaoQuestao novaPolitica) {
        exigirConcursoNaoHomologado();
        Objects.requireNonNull(novaPolitica, "Politica de anulacao obrigatoria.");
        ConfiguracaoRegrasEdital atual = configuracaoRegrasEdital();
        this.configuracaoRegrasEdital = new ConfiguracaoRegrasEdital(
            novaPolitica,
            atual.cadeiaDesempate(),
            atual.formatoArquivoOptico(),
            atual.prazoAberturaRecursoAposGabarito(),
            atual.limitarUmRecursoPorQuestaoPorCandidato(),
            atual.limiteMaximoRecursosPorCandidato(),
            atual.agruparRecursosIdenticosEmLote(),
            atual.modalidadesIsencaoAceitas(),
            atual.validacaoAutomatizadaCadUnico()
        );
        this.codigoPoliticaAnulacao = extrairCodigoPolitica(novaPolitica);
    }

    public void encerrarEHomologarResultadoFinal(boolean possuiRecursosPendentes) {
        exigirConcursoNaoHomologado();
        if (possuiRecursosPendentes) {
            throw new br.com.keyfalls.shared.domain.exception.RecursosPendentesException(
                "Nao e permitido homologar o resultado final enquanto existirem recursos pendentes de julgamento."
            );
        }
        for (Prova prova : provas) {
            if (prova.gabarito() != null && !prova.gabarito().isDefinitivo()) {
                prova.consolidarGabaritoDefinitivo();
            }
        }
        this.status = StatusConcurso.RESULTADO_FINAL_HOMOLOGADO;
    }

    private void exigirConcursoNaoHomologado() {
        if (this.status == StatusConcurso.RESULTADO_FINAL_HOMOLOGADO) {
            throw new br.com.keyfalls.shared.domain.exception.ConflitoDeEstadoException(
                "Concurso encerrado e homologado: nenhuma alteracao normativa ou de gabarito e permitida."
            );
        }
    }

    public StatusConcurso status() {
        return status;
    }

    private void validarExistenciaCargo(CargoId cargoId) {
        boolean existe = cargos.stream().anyMatch(c -> c.id().equals(cargoId));
        if (!existe) {
            throw new RegraDeNegocioException("Cargo nao pertence a este concurso.");
        }
    }

    private Prova buscarProva(ProvaId provaId) {
        return provas.stream()
            .filter(p -> p.id().equals(provaId))
            .findFirst()
            .orElseThrow(() -> new RegraDeNegocioException("Prova nao localizada neste concurso."));
    }

    private static String extrairCodigoPolitica(PoliticaAnulacaoQuestao politica) {
        return (politica instanceof PoliticaAnulacaoQuestao.RedistribuicaoPesoStrategy)
            ? "REDISTRIBUICAO_PESO_STRATEGY"
            : "PONTO_UNIVERSAL_STRATEGY";
    }

    private static PoliticaAnulacaoQuestao reconstruirPolitica(String codigo) {
        if ("REDISTRIBUICAO_PESO_STRATEGY".equals(codigo) || "DESCONSIDERAR_DO_TOTAL_DA_PROVA".equals(codigo)) {
            return new PoliticaAnulacaoQuestao.RedistribuicaoPesoStrategy();
        }
        return new PoliticaAnulacaoQuestao.PontoUniversalStrategy();
    }

    private static ConfiguracaoRegrasEdital criarConfiguracaoComPolitica(PoliticaAnulacaoQuestao politica) {
        ConfiguracaoRegrasEdital padrao = ConfiguracaoRegrasEdital.padraoRecomendado();
        return new ConfiguracaoRegrasEdital(
            politica,
            padrao.cadeiaDesempate(),
            padrao.formatoArquivoOptico(),
            padrao.prazoAberturaRecursoAposGabarito(),
            padrao.limitarUmRecursoPorQuestaoPorCandidato(),
            padrao.limiteMaximoRecursosPorCandidato(),
            padrao.agruparRecursosIdenticosEmLote(),
            padrao.modalidadesIsencaoAceitas(),
            padrao.validacaoAutomatizadaCadUnico()
        );
    }

    @Override
    public ConcursoId id() {
        return new ConcursoId(id);
    }

    public UUID getId() {
        return id;
    }

    public Long getVersao() {
        return versao;
    }

    public String codigoEdital() {
        return codigoEdital;
    }

    public String titulo() {
        return titulo;
    }

    public Abrangencia abrangencia() {
        return abrangencia;
    }

    public Periodo periodoInscricao() {
        return periodoInscricao;
    }

    public Periodo periodoIsencao() {
        return periodoIsencao;
    }

    public PoliticaAnulacaoQuestao politicaAnulacaoQuestao() {
        return configuracaoRegrasEdital().politicaAnulacaoQuestao();
    }

    public ConfiguracaoRegrasEdital configuracaoRegrasEdital() {
        if (configuracaoRegrasEdital == null) {
            configuracaoRegrasEdital = criarConfiguracaoComPolitica(reconstruirPolitica(codigoPoliticaAnulacao));
        }
        return configuracaoRegrasEdital;
    }

    public List<Cargo> cargos() {
        return Collections.unmodifiableList(cargos);
    }

    public List<Prova> provas() {
        return Collections.unmodifiableList(provas);
    }
}
