package com.klos.automation.taxable.exchange;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Exchange rate provider using Czech National Bank (CNB) API.
 * Fetches real exchange rates from CNB.
 */
public class CnbExchangeRateProvider implements ExchangeRateProvider {
    
    private static final String DAILY_RATE_URL = "https://api.cnb.cz/cnbapi/exrates/daily?date=%s&lang=EN";
    private static final String YEARLY_AVG_URL = "https://www.cnb.cz/cs/financni-trhy/devizovy-trh/kurzy-devizoveho-trhu/kurzy-devizoveho-trhu/prumerne_rok.txt?rok=%d";
    private static final String CURRENCY_CODE = "USD";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final Map<LocalDate, BigDecimal> rateCache = new HashMap<>();
    private final Map<Integer, BigDecimal> yearlyAverageCache = new HashMap<>();
    private final Gson gson = new Gson();
    
    @Override
    public BigDecimal getRate(LocalDate date) {
        return rateCache.computeIfAbsent(date, this::fetchDailyRate);
    }
    
    @Override
    public BigDecimal getYearlyAverageRate(int year) {
        return yearlyAverageCache.computeIfAbsent(year, this::fetchYearlyAverageRate);
    }
    
    @Override
    public String getDescription() {
        return "CNB (Czech National Bank) API";
    }
    
    private BigDecimal fetchDailyRate(LocalDate date) {
        String url = String.format(DAILY_RATE_URL, date.format(DATE_FORMAT));
        try {
            String json = fetchUrl(url);
            JsonObject response = gson.fromJson(json, JsonObject.class);
            JsonArray rates = response.getAsJsonArray("rates");
            
            for (int i = 0; i < rates.size(); i++) {
                JsonObject rate = rates.get(i).getAsJsonObject();
                if (CURRENCY_CODE.equals(rate.get("currencyCode").getAsString())) {
                    return BigDecimal.valueOf(rate.get("rate").getAsDouble());
                }
            }
            throw new RuntimeException("USD rate not found for date: " + date);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch CNB rate for date: " + date, e);
        }
    }
    
    private BigDecimal fetchYearlyAverageRate(int year) {
        String url = String.format(YEARLY_AVG_URL, year);
        try {
            String text = fetchUrl(url);
            return parseYearlyAverageFromText(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch CNB yearly average for year: " + year, e);
        }
    }
    
    /**
     * Parse yearly average from CNB text format.
     * Looks for the USD row in the cumulative averages section (leden-prosinec = Jan-Dec).
     */
    private BigDecimal parseYearlyAverageFromText(String text) {
        String[] lines = text.split("\n");
        boolean inCumulativeSection = false;
        
        for (String line : lines) {
            // The cumulative section starts with header containing "leden-prosinec"
            if (line.contains("leden-prosinec")) {
                inCumulativeSection = true;
                continue;
            }
            
            if (inCumulativeSection && line.startsWith("USD|")) {
                // Format: USD|1|value1|value2|...|valueN (last value is leden-prosinec = full year avg)
                String[] parts = line.split("\\|");
                if (parts.length >= 14) {
                    // Index 13 is leden-prosinec (Jan-Dec cumulative average)
                    String rate = parts[13].replace(",", ".");
                    return new BigDecimal(rate);
                }
            }
        }
        throw new RuntimeException("Could not parse yearly average from CNB response");
    }
    
    private String fetchUrl(String urlString) throws IOException {
        URI uri = URI.create(urlString);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}

