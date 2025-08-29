package org.refactor.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NotifyUtils {
    @SuppressWarnings("CallToPrintStackTrace")
    public static void notifyMyself() {
        try {
            URL url = new URL(AppProperties.getString("notifyUrl"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
            String message = "Finished, pray 😀";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
