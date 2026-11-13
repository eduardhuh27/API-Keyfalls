package br.com.keyfalls.shared.domain;

import br.com.keyfalls.shared.domain.event.DomainEvent;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Classe base para Raizes de Agregado (Aggregate Roots), responsavel por acumular
 * Eventos de Dominio em memoria durante mutacoes de negocio.
 */
@MappedSuperclass
public abstract class AggregateRoot<ID> {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public abstract ID id();

    protected final void registerEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Evento de dominio registrado nao pode ser nulo.");
        this.domainEvents.add(event);
    }

    public final List<DomainEvent> pullDomainEvents() {
        if (domainEvents.isEmpty()) {
            return List.of();
        }
        List<DomainEvent> snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    public final List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}
