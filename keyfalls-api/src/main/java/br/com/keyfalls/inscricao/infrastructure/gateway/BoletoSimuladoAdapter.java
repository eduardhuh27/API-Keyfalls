package br.com.keyfalls.inscricao.infrastructure.gateway;

import br.com.keyfalls.concurso.domain.Cargo;
import br.com.keyfalls.concurso.domain.Concurso;
import br.com.keyfalls.concurso.domain.ConcursoRepository;
import br.com.keyfalls.inscricao.application.ConsultaOfertaConcursoPort;
import br.com.keyfalls.inscricao.application.EmissorBoletoBancarioPort;
import br.com.keyfalls.inscricao.application.GeradorNumeroInscricaoPort;
import br.com.keyfalls.shared.domain.CargoId;
import br.com.keyfalls.shared.domain.ConcursoId;
import br.com.keyfalls.shared.domain.NumeroInscricao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BoletoSimuladoAdapter
    implements EmissorBoletoBancarioPort, ConsultaOfertaConcursoPort, GeradorNumeroInscricaoPort {

    private static final LocalDate DATA_BASE_FEBRABAN = LocalDate.of(1997, 10, 7);
    private static final AtomicLong SEQUENCIAL_ATOMICO = new AtomicLong(100000L);

    private final ConcursoRepository concursoRepository;

    public BoletoSimuladoAdapter(ConcursoRepository concursoRepository) {
        this.concursoRepository = Objects.requireNonNull(concursoRepository);
    }

    @Override
    public String registrarCobrancaBancaria(NumeroInscricao numeroInscricao, BigDecimal valor, LocalDate vencimento) {
        String codigoNumerico = numeroInscricao.codigo().replaceAll("\\D", "");
        String dezDigitos = (codigoNumerico + "0000000000").substring(0, 10);
        long fatorVencimento = Math.max(1000L, ChronoUnit.DAYS.between(DATA_BASE_FEBRABAN, vencimento));
        String valorCentavos = "%010d".formatted(
            valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        );

        return "00190.00009 02026.%s9 %s.17 8 %04d%s".formatted(
            dezDigitos.substring(0, 5),
            dezDigitos,
            fatorVencimento % 10000,
            valorCentavos
        );
    }

    @Override
    public Optional<DadosOfertaCargo> consultarOferta(ConcursoId concursoId, CargoId cargoId) {
        Optional<Concurso> concursoOpt = concursoRepository.buscarPorId(concursoId);
        if (concursoOpt.isEmpty()) {
            return Optional.empty();
        }
        Concurso concurso = concursoOpt.get();
        Optional<Cargo> cargoOpt = concurso.cargos().stream()
            .filter(c -> c.id().equals(cargoId))
            .findFirst();
        if (cargoOpt.isEmpty()) {
            return Optional.empty();
        }
        Cargo cargo = cargoOpt.get();
        LocalDate vencimento = LocalDate.ofInstant(concurso.periodoInscricao().fim(), ZoneOffset.UTC).plusDays(5);
        return Optional.of(new DadosOfertaCargo(
            concurso.periodoInscricao(),
            concurso.periodoIsencao(),
            cargo.taxaInscricao(),
            vencimento
        ));
    }

    @Override
    public NumeroInscricao proximoNumero(ConcursoId concursoId) {
        int ano = LocalDate.now(ZoneOffset.UTC).getYear();
        long proximo = SEQUENCIAL_ATOMICO.incrementAndGet();
        return new NumeroInscricao("%04d%06d".formatted(ano, proximo));
    }
}
