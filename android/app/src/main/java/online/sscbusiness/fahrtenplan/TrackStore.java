package online.sscbusiness.fahrtenplan;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Speicher des nativen Tracking-Dienstes: Anmeldedaten (SharedPreferences), Warteschlange der
 * Standortpunkte (Datei im App-Speicher, überlebt Neustart/Wegwischen) und der Upload an track.php.
 */
final class TrackStore {
    private TrackStore() {}

    static final int MAX_QUEUE = 4000;
    private static final Object LOCK = new Object();

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("fp_track", Context.MODE_PRIVATE);
    }
    static boolean active(Context c) { return sp(c).getBoolean("active", false); }
    static String get(Context c, String k) { return sp(c).getString(k, ""); }

    // ---- Warteschlange ----
    private static File qFile(Context c) { return new File(c.getApplicationContext().getFilesDir(), "trackq.json"); }

    private static JSONArray readAll(Context c) {
        File f = qFile(c);
        if (!f.exists()) return new JSONArray();
        try {
            FileInputStream in = new FileInputStream(f);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return new JSONArray(sb.toString());
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void writeAll(Context c, JSONArray a) {
        try {
            File f = qFile(c);
            File tmp = new File(f.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
            out.close();
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f); }
        } catch (Exception ignored) {}
    }

    static void queueAdd(Context c, JSONObject item) {
        synchronized (LOCK) {
            JSONArray a = readAll(c);
            a.put(item);
            if (a.length() > MAX_QUEUE) {
                JSONArray b = new JSONArray();
                for (int i = a.length() - MAX_QUEUE; i < a.length(); i++) b.put(a.opt(i));
                a = b;
            }
            writeAll(c, a);
        }
    }

    static int queueSize(Context c) {
        synchronized (LOCK) { return readAll(c).length(); }
    }

    /** Die ersten n Einträge (höchstens max) lesen, ohne sie zu entfernen. */
    static JSONArray queuePeek(Context c, int max) {
        synchronized (LOCK) {
            JSONArray a = readAll(c), b = new JSONArray();
            for (int i = 0; i < a.length() && i < max; i++) b.put(a.opt(i));
            return b;
        }
    }

    static void queueDrop(Context c, int n) {
        synchronized (LOCK) {
            JSONArray a = readAll(c), b = new JSONArray();
            for (int i = n; i < a.length(); i++) b.put(a.opt(i));
            writeAll(c, b);
        }
    }

    static void queueClear(Context c) {
        synchronized (LOCK) { qFile(c).delete(); }
    }

    // ---- Upload ----
    /** Ergebnis: HTTP-Code (200 = ok), -1 = kein Netz/Fehler. Verlängerten Token übernehmen. */
    /** Von der letzten Antwort: neue Hinweise fürs Handy (JSON-Array als Text), sonst leer. */
    static volatile String lastNotify = "";

    static int post(Context c, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            String base = get(c, "baseUrl");
            if (base.isEmpty() || !base.startsWith("https://")) return -1;
            URL url = new URL(base.replaceAll("/+$", "") + "/track.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(25000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", get(c, "apiKey"));
            conn.setRequestProperty("X-Auth-Token", get(c, "token"));
            conn.setRequestProperty("X-FP-App", "1");
            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(data);
            os.close();
            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                lastNotify = "";
                try {
                    JSONObject ro = new JSONObject(sb.toString());
                    org.json.JSONArray na = ro.optJSONArray("notify");
                    if (na != null && na.length() > 0) lastNotify = na.toString();
                    String nt = ro.optString("_token", "");
                    if (!nt.isEmpty()) sp(c).edit().putString("token", nt).apply();
                } catch (Exception ignored) {}
            }
            return code;
        } catch (Exception e) {
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    static void clearLogin(Context c) {
        sp(c).edit().putBoolean("active", false).remove("token").remove("employeeId").remove("employeeName").apply();
        queueClear(c);
    }
}
