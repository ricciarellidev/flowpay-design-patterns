package dev.ricciarelli.flowpay.infrastructure.gateway;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class AsterPayGatewayAdapter implements PaymentGateway {

    private final AsterPayClient client;

    public AsterPayGatewayAdapter(AsterPayClient client) {
        this.client = client;
    }

    @Override
    public GatewayResult authorize(ProcessPaymentCommand command, String channel) {
        var charge = new AsterPayClient.AsterCharge(
                command.customerId(),
                command.amount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact(),
                command.currency().getCurrencyCode(),
                channel,
                command.paymentInstrumentToken());

        var response = client.authorize(charge);
        var status = "AUTHORIZED".equals(response.status())
                ? PaymentStatus.APPROVED
                : PaymentStatus.DECLINED;

        return new GatewayResult(status, response.authorizationId());
    }
}

