package br.ufrn.imd.imdtravel.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Venda concluída (R-08). O id é interno e não sai no JSON. */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "transaction_id")
    private String transactionId;

    private String flight;

    // guardado como veio (yyyy-MM-dd)
    @Column(name = "day")
    private String day;

    // "user" é palavra reservada em vários SQLs
    @Column(name = "user_name")
    private String user;

    @Column(name = "value_usd")
    private BigDecimal valueUSD;

    @Column(name = "value_brl")
    private BigDecimal valueBRL;

    @Column(name = "ts")
    private Instant ts;

    protected Sale() {
    }

    public Sale(String correlationId, String transactionId, String flight, String day,
                String user, BigDecimal valueUSD, BigDecimal valueBRL, Instant ts) {
        this.correlationId = correlationId;
        this.transactionId = transactionId;
        this.flight = flight;
        this.day = day;
        this.user = user;
        this.valueUSD = valueUSD;
        this.valueBRL = valueBRL;
        this.ts = ts;
    }

    public Long getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getTransactionId() { return transactionId; }
    public String getFlight() { return flight; }
    public String getDay() { return day; }
    public String getUser() { return user; }
    public BigDecimal getValueUSD() { return valueUSD; }
    public BigDecimal getValueBRL() { return valueBRL; }
    public Instant getTs() { return ts; }
}
