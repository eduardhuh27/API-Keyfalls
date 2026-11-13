package br.com.keyfalls.inscricao.domain;

import br.com.keyfalls.inscricao.domain.event.InscricaoRealizada;
import br.com.keyfalls.inscricao.domain.event.IsencaoJulgada;
import br.com.keyfalls.shared.domain.AggregateRoot;
import br.com.keyfalls.shared.domain.CandidatoId;
import br.com.keyfalls.shared.domain.CargoId;
import br.com.keyfalls.shared.domain.ConcursoId;
import br.com.keyfalls.shared.domain.InscricaoId;
import br.com.keyfalls.shared.domain.NumeroInscricao;
import br.com.keyfalls.shared.domain.Periodo;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import br.com.keyfalls.shared.domain.exception.PrazoIsencaoEncerradoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "inscricoes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inscricao_candidato_concurso",
        columnNames = {"candidato_id", "concurso_id"}
    )
)
public class Inscricao extends AggregateRoot<InscricaoId> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Embedded
    private NumeroInscricao numero;

    @Column(name = "candidato_id", nullable = false, updatable = false)
    private UUID candidatoId;

    @Column(name = "concurso_id", nullable = false, updatable = false)
    private UUID concursoId;

    @Column(name = "cargo_id", nullable = false, updatable = false)
    private UUID cargoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusInscricao status;

    @Column(name = "boleto_linha_digitavel", length = 64)
    private String boletoLinhaDigitavel;

    @Column(name = "boleto_valor", precision = 10, scale = 2)
    private BigDecimal boletoValor;

    @Column(name = "boleto_vencimento")
    private LocalDate boletoVencimento;

    @Column(name = "boleto_pago")
    private boolean boletoPago;

    @Column(name = "boleto_liquidado_em")
    private Instant boletoLiquidadoEm;

    @Transient
    private Boleto boleto;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_isencao_id")
    private PedidoIsencao pedidoIsencao;

    protected Inscricao() {
    }

    private Inscricao(
        InscricaoId id,
        NumeroInscricao numero,
        CandidatoId candidatoId,
        ConcursoId concursoId,
        CargoId cargoId
    ) {
        this.id = Objects.requireNonNull(id, "InscricaoId obrigatorio.").valor();
        this.numero = Objects.requireNonNull(numero, "NumeroInscricao obrigatorio.");
        this.candidatoId = Objects.requireNonNull(candidatoId, "CandidatoId obrigatorio.").valor();
        this.concursoId = Objects.requireNonNull(concursoId, "ConcursoId obrigatorio.").valor();
        this.cargoId = Objects.requireNonNull(cargoId, "CargoId obrigatorio.").valor();
        this.status = StatusInscricao.AGUARDANDO_PAGAMENTO;
    }

    public static Inscricao realizar(
        InscricaoId id,
        NumeroInscricao numero,
        CandidatoId candidatoId,
        ConcursoId concursoId,
        CargoId cargoId,
        Periodo periodoInscricaoConcurso,
        Instant instanteAtual
    ) {
        Objects.requireNonNull(periodoInscricaoConcurso, "Periodo de inscricao do concurso obrigatorio.");
        if (!periodoInscricaoConcurso.contemEstritamente(instanteAtual)) {
            throw new RegraDeNegocioException("Fora do prazo regulamentar de inscricao do concurso.");
        }
        Inscricao inscricao = new Inscricao(id, numero, candidatoId, concursoId, cargoId);
        inscricao.registerEvent(
            InscricaoRealizada.novo(id, numero, candidatoId, concursoId, cargoId, instanteAtual)
        );
        return inscricao;
    }

    public static Inscricao reconstituir(
        InscricaoId id,
        NumeroInscricao numero,
        CandidatoId candidatoId,
        ConcursoId concursoId,
        CargoId cargoId,
        StatusInscricao status,
        Boleto boleto,
        PedidoIsencao pedidoIsencao
    ) {
        Inscricao inscricao = new Inscricao(id, numero, candidatoId, concursoId, cargoId);
        inscricao.status = Objects.requireNonNull(status, "Status obrigatorio na reconstituicao.");
        inscricao.definirBoletoInterno(boleto);
        inscricao.pedidoIsencao = pedidoIsencao;
        return inscricao;
    }

    public Boleto gerarBoleto(String linhaDigitavel, BigDecimal valorTaxa, LocalDate vencimento) {
        if (status == StatusInscricao.HOMOLOGADA || status == StatusInscricao.CANCELADA) {
            throw new RegraDeNegocioException("Nao e possivel emitir boleto para inscricao com status " + status);
        }
        if (status == StatusInscricao.ISENCAO_EM_ANALISE) {
            throw new RegraDeNegocioException("Ha um pedido de isencao pendente de julgamento para esta inscricao.");
        }
        definirBoletoInterno(Boleto.emitir(linhaDigitavel, valorTaxa, vencimento));
        return this.boleto;
    }

    public void homologarPorPagamento(Instant dataPagamento) {
        Boleto b = boleto();
        if (b == null) {
            throw new RegraDeNegocioException("Nenhum boleto emitido para liquidacao nesta inscricao.");
        }
        if (this.status == StatusInscricao.CANCELADA) {
            throw new RegraDeNegocioException("Inscricao cancelada nao pode ser homologada.");
        }
        definirBoletoInterno(b.liquidar(dataPagamento));
        this.status = StatusInscricao.HOMOLOGADA;
    }

    public PedidoIsencao solicitarIsencao(
        String justificativaLegal,
        Periodo periodoIsencaoConcurso,
        Instant instanteSolicitacao
    ) {
        validarPrazoEstritoDeIsencao(periodoIsencaoConcurso, instanteSolicitacao, "solicitar");
        if (this.status == StatusInscricao.HOMOLOGADA || this.status == StatusInscricao.CANCELADA) {
            throw new RegraDeNegocioException("Nao e permitido solicitar isencao para inscricao com status " + status);
        }
        if (this.pedidoIsencao != null) {
            throw new RegraDeNegocioException("Ja existe pedido de isencao registrado para esta inscricao.");
        }
        this.pedidoIsencao = new PedidoIsencao(justificativaLegal, instanteSolicitacao);
        this.status = StatusInscricao.ISENCAO_EM_ANALISE;
        return this.pedidoIsencao;
    }

    public PedidoIsencao solicitarIsencaoComEspecificacao(
        EspecificacaoElegibilidadeIsencao.ContextoSolicitacaoIsencao contextoElegibilidade
    ) {
        Objects.requireNonNull(contextoElegibilidade, "Contexto de elegibilidade da isencao obrigatorio.");
        EspecificacaoElegibilidadeIsencao.validarOuRejeitar(contextoElegibilidade);
        return solicitarIsencao(
            contextoElegibilidade.justificativaLegal(),
            contextoElegibilidade.periodoIsencaoEdital(),
            contextoElegibilidade.instanteSolicitacao()
        );
    }

    public void deferirIsencao(String parecer, Periodo periodoIsencaoConcurso, Instant instanteAvaliacao) {
        validarPrazoEstritoDeIsencao(periodoIsencaoConcurso, instanteAvaliacao, "avaliar (deferir)");
        exigirPedidoIsencaoExistente();
        this.pedidoIsencao.deferir(parecer, instanteAvaliacao);
        this.status = StatusInscricao.HOMOLOGADA;
        registerEvent(IsencaoJulgada.novo(
            this.id(), this.numero, this.candidatoId(), this.concursoId(), true, this.pedidoIsencao.parecerAvaliacao(), instanteAvaliacao
        ));
    }

    public void indeferirIsencao(String parecer, Periodo periodoIsencaoConcurso, Instant instanteAvaliacao) {
        validarPrazoEstritoDeIsencao(periodoIsencaoConcurso, instanteAvaliacao, "avaliar (indeferir)");
        exigirPedidoIsencaoExistente();
        this.pedidoIsencao.indeferir(parecer, instanteAvaliacao);
        this.status = StatusInscricao.AGUARDANDO_PAGAMENTO;
        registerEvent(IsencaoJulgada.novo(
            this.id(), this.numero, this.candidatoId(), this.concursoId(), false, this.pedidoIsencao.parecerAvaliacao(), instanteAvaliacao
        ));
    }

    public void cancelar() {
        if (this.status == StatusInscricao.CANCELADA) {
            throw new RegraDeNegocioException("A inscricao ja se encontra cancelada.");
        }
        this.status = StatusInscricao.CANCELADA;
    }

    public boolean estaHomologada() {
        return this.status == StatusInscricao.HOMOLOGADA;
    }

    private void definirBoletoInterno(Boleto novoBoleto) {
        this.boleto = novoBoleto;
        if (novoBoleto != null) {
            this.boletoLinhaDigitavel = novoBoleto.linhaDigitavel();
            this.boletoValor = novoBoleto.valor();
            this.boletoVencimento = novoBoleto.vencimento();
            this.boletoPago = novoBoleto.pago();
            this.boletoLiquidadoEm = novoBoleto.liquidadoEm();
        } else {
            this.boletoLinhaDigitavel = null;
            this.boletoValor = null;
            this.boletoVencimento = null;
            this.boletoPago = false;
            this.boletoLiquidadoEm = null;
        }
    }

    private static void validarPrazoEstritoDeIsencao(Periodo periodoIsencao, Instant instanteAtual, String acao) {
        Objects.requireNonNull(periodoIsencao, "Periodo de isencao obrigatorio.");
        Objects.requireNonNull(instanteAtual, "Instante atual obrigatorio.");
        if (!periodoIsencao.contemEstritamente(instanteAtual)) {
            throw new PrazoIsencaoEncerradoException(
                "Nao e permitido " + acao + " pedido de isencao fora do periodo estrito estabelecido no edital."
            );
        }
    }

    private void exigirPedidoIsencaoExistente() {
        if (this.pedidoIsencao == null) {
            throw new RegraDeNegocioException("Esta inscricao nao possui pedido de isencao registrado.");
        }
    }

    @Override
    public InscricaoId id() {
        return new InscricaoId(id);
    }

    public UUID getId() {
        return id;
    }

    public Long getVersao() {
        return versao;
    }

    public NumeroInscricao numero() {
        return numero;
    }

    public CandidatoId candidatoId() {
        return new CandidatoId(candidatoId);
    }

    public ConcursoId concursoId() {
        return new ConcursoId(concursoId);
    }

    public CargoId cargoId() {
        return new CargoId(cargoId);
    }

    public StatusInscricao status() {
        return status;
    }

    public Boleto boleto() {
        if (boleto == null && boletoLinhaDigitavel != null && boletoValor != null && boletoVencimento != null) {
            boleto = new Boleto(boletoLinhaDigitavel, boletoValor, boletoVencimento, boletoPago, boletoLiquidadoEm);
        }
        return boleto;
    }

    public PedidoIsencao pedidoIsencao() {
        return pedidoIsencao;
    }
}
