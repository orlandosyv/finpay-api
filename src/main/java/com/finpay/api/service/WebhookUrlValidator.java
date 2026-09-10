package com.finpay.api.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.finpay.api.exception.InvalidWebhookUrlException;

@Component
public class WebhookUrlValidator {

    private final boolean requireHttps;
    private final boolean allowPrivateAddresses;

    public WebhookUrlValidator(
            @Value("${finpay.webhook.require-https:false}") boolean requireHttps,
            @Value("${finpay.webhook.allow-private-addresses:true}") boolean allowPrivateAddresses) {
        this.requireHttps = requireHttps;
        this.allowPrivateAddresses = allowPrivateAddresses;
    }

    public String validateAndNormalize(String suppliedUrl) {
        try {
            URI uri = new URI(suppliedUrl.trim());
            String scheme = uri.getScheme() == null
                    ? ""
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https"))) {
                throw invalid("Webhook URL must use HTTP or HTTPS");
            }
            if (requireHttps && !scheme.equals("https")) {
                throw invalid("Webhook URL must use HTTPS");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw invalid("Webhook URL must include a valid host");
            }
            if (uri.getUserInfo() != null || uri.getFragment() != null) {
                throw invalid("Webhook URL must not contain credentials or a fragment");
            }
            if (!allowPrivateAddresses) {
                rejectPrivateAddresses(uri.getHost());
            }
            return uri.normalize().toASCIIString();
        } catch (URISyntaxException exception) {
            throw invalid("Webhook URL format is invalid");
        }
    }

    private void rejectPrivateAddresses(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw invalid("Webhook URL must not resolve to a private address");
                }
            }
        } catch (UnknownHostException exception) {
            throw invalid("Webhook URL host could not be resolved");
        }
    }

    private InvalidWebhookUrlException invalid(String message) {
        return new InvalidWebhookUrlException(message);
    }
}
