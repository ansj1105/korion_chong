package com.korion.chong.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class WalletAddressSupport {
    private static final Pattern TRON_ADDRESS_PATTERN = Pattern.compile("^T[1-9A-HJ-NP-Za-km-z]{33}$");
    private static final Pattern EVM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final Pattern BTC_LEGACY_ADDRESS_PATTERN = Pattern.compile("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
    private static final Pattern BTC_BECH32_ADDRESS_PATTERN = Pattern.compile("^(bc1|tb1)[ac-hj-np-z02-9]{11,87}$", Pattern.CASE_INSENSITIVE);

    private WalletAddressSupport() {
    }

    static Optional<String> detectNetwork(String walletAddress) {
        String normalized = walletAddress == null ? "" : walletAddress.trim();
        if (TRON_ADDRESS_PATTERN.matcher(normalized).matches()) {
            return Optional.of("TRON");
        }
        if (EVM_ADDRESS_PATTERN.matcher(normalized).matches()) {
            return Optional.of("EVM");
        }
        if (BTC_LEGACY_ADDRESS_PATTERN.matcher(normalized).matches()
                || BTC_BECH32_ADDRESS_PATTERN.matcher(normalized.toLowerCase(Locale.ROOT)).matches()) {
            return Optional.of("BTC");
        }
        return Optional.empty();
    }

    static boolean isTronAddress(String walletAddress) {
        return detectNetwork(walletAddress)
                .filter("TRON"::equals)
                .isPresent();
    }
}
