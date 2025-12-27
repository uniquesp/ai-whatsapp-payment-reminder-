package com.example.yoga_reminder.domain.entity;

import java.time.LocalDateTime;

import com.example.yoga_reminder.domain.enums.PaymentIntent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Size(max = 1000)
    @Column(name = "user_reply_text", length = 1000)
    private String userReplyText;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_intent", nullable = false, length = 20)
    private PaymentIntent detectedIntent;

    @NotNull
    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;
}

