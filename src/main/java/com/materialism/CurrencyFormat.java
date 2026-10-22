package com.materialism;

public record CurrencyFormat(String symbol, boolean prefix) {
    public static final CurrencyFormat DEFAULT = new CurrencyFormat("c", false);

    public CurrencyFormat {
        if (symbol == null) {
            symbol = DEFAULT.symbol;
        }
    }

    public static CurrencyFormat parse(String symbol, String position) {
        boolean prefix = position != null && "prefix".equalsIgnoreCase(position.trim());
        return new CurrencyFormat(symbol == null ? DEFAULT.symbol : symbol, prefix);
    }

    public String format(long units) {
        String amount = Money.formatShort(units);
        return this.prefix ? this.symbol + amount : amount + this.symbol;
    }
}
