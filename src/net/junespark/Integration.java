package net.junespark;

import com.google.gson.Gson;
import net.junespark.dto.BotPathing;
import net.junespark.dto.StallData;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class Integration {
    
    private static final boolean local = false;
    private static final boolean upload = true;
    private static final String MARKET = "Linch Market";
    private static final String SERVER_URL = local ? "http://localhost:5000" : "https://hnh-market.junespark.net";
    private static final String ADD_STALLS = SERVER_URL + "/api/stalls/add";
    private static final String INITIALIZE_STALLS = SERVER_URL + "/api/stalls/init";
    private static final String BOT_PATHING = SERVER_URL + "/api/bot/pathing";
    private static final Gson gson = new Gson();
    
    public static void sendStallData(final StallData stallData) {
        post(stallData, ADD_STALLS);
    }
    
    public static void sendMultipleStalls(final Collection<StallData> stallData) {
        post(stallData, INITIALIZE_STALLS);
    }
    
    public static BotPathing bot() {
        return getSync(Collections.singletonMap("market", MARKET), BOT_PATHING, BotPathing.class);
    }
    
    private static void post(final Object stallRows, String endpoint) {
        if(!upload) return;
        Runnable task = () -> {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                byte[] out = gson.toJson(stallRows).getBytes(StandardCharsets.UTF_8);
                int length = out.length;
                
                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.connect();
                try (OutputStream os = http.getOutputStream()) {
                    os.write(out);
                }
            } catch (IOException ignored) {
                //server not replying
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }
    
    private static <T> T getSync(Map<String, String> params, String endpoint, Class<T> clazz) {
        try {
            URL url = new URL(endpoint + getParamsString(params));
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setDoOutput(true);
            int responseCode = http.getResponseCode();
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(http.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return gson.fromJson(content.toString(), clazz);
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        return null;
    }
    
    private static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }
        
        String resultString = result.toString();
        return resultString.length() > 0
            ? "?" + resultString.substring(0, resultString.length() - 1)
            : resultString;
    }
}
