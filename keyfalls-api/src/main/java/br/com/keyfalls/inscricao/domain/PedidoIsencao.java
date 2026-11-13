package br.com.keyfalls.inscricao.domain;

import br.com.keyfalls.shared.domain.IsencaoId;
import br.com.keyfalls.shared.domain.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pedidos_isencao")
public class PedidoIsencao {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "justificativa_legal", nullable = false, length = 1000)
    private String justificativaLegal;

    @Column(name = "solicitado_em", nullable = false)
    private Instant solicitadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoIsencao situacao;

    @Column(name = "parecer_avaliacao", length = 1000)
    private String parecerAvaliacao;

    @Column(name = "avaliado_em")
    private Instant avaliadoEm;

    protected PedidoIsencao() {
    }

    PedidoIsencao(String justificativaLegal, Instant solicitadoEm) {
        this(IsencaoId.novo(), justificativaLegal, solicitadoEm);
    }

    PedidoIsencao(IsencaoId id, String justificativaLegal, Instant solicitadoEm) {
        this.id = Objects.requireNonNull(id, "IsencaoId obrigatorio.").valor();
        if (justificativaLegal == null || justificativaLegal.isBlank()) {
            throw new RegraDeNegocioException("Justificativa legal para pedido de isencao obrigatoria.");
        }
        this.justificativaLegal = justificativaLegal.trim();
        this.solicitadoEm = Objects.requireNonNull(solicitadoEm, "Data de solicitacao da isencao obrigatoria.");
        this.situacao = SituacaoIsencao.EM_ANALISE;
    }

    public static PedidoIsencao reconstituir(
        String justificativaLegal,
        Instant solicitadoEm,
        SituacaoIsencao situacao,
        String parecerAvaliacao,
        Instant avaliadoEm
    ) {
        PedidoIsencao pedido = new PedidoIsencao(justificativaLegal, solicitadoEm);
        pedido.situacao = Objects.requireNonNull(situacao, "Situacao obrigatoria na reconstituicao.");
        pedido.parecerAvaliacao = parecerAvaliacao;
        pedido.avaliadoEm = avaliadoEm;
        return pedido;
    }

    void deferir(String parecer, Instant instanteAvaliacao) {
        validarPendenteDeAnalise();
        validarParecer(parecer);
        this.situacao = SituacaoIsencao.DEFERIDA;
        this.parecerAvaliacao = parecer.trim();
        this.avaliadoEm = Objects.requireNonNull(instanteAvaliacao, "Instante de avaliacao obrigatorio.");
    }

    void indeferir(String parecer, Instant instanteAvaliacao) {
        validarPendenteDeAnalise();
        validarParecer(parecer);
        this.situacao = SituacaoIsencao.INDEFERIDA;
        this.parecerAvaliacao = parecer.trim();
        this.avaliadoEm = Objects.requireNonNull(instanteAvaliacao, "Instante de avaliacao obrigatorio.");
    }

    private void validarPendenteDeAnalise() {
        if (this.situacao != SituacaoIsencao.EM_ANALISE) {
            throw new RegraDeNegocioException("O pedido de isencao ja foi avaliado (" + this.situacao + ").");
        }
    }

    private static void validarParecer(String parecer) {
        if (parecer == null || parecer.isBlank()) {
            throw new RegraDeNegocioException("Parecer fundamentado da analise de isencao obrigatorio.");
        }
    }

    public IsencaoId id() {
        return new IsencaoId(id);
    }

    public UUID getId() {
        return id;
    }

    public String justificativaLegal() {
        return justificativaLegal;
    }

    public Instant solicitadoEm() {
        return solicitadoEm;
    }

    public SituacaoIsencao situacao() {
        return situacao;
    }

    public String parecerAvaliacao() {
        return parecerAvaliacao;
    }

    public Instant avaliadoEm() {
        return avaliadoEm;
    }
}
