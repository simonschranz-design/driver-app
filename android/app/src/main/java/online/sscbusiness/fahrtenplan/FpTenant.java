package online.sscbusiness.fahrtenplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.getcapacitor.CapConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Mandanten-Wahl der App (SaaS): Beim ersten Start zeigt die App die lokale Seite www/index.html
 * («Firmen-Code eingeben»). Die gewählte Firmen-Adresse wird gespeichert; ab dann startet die App direkt
 * mit dieser Adresse als Capacitor-Server – dadurch funktionieren dort alle eigenen Plugins (Standort, Dokumente).
 */
final class FpTenant {
    private FpTenant() {}

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("fp_tenant", Context.MODE_PRIVATE); }

    static String url(Context c) { return sp(c).getString("url", ""); }

    /** Capacitor-Konfiguration aus assets/capacitor.config.json, mit der gewählten Firmen-Adresse als server.url. */
    static CapConfig buildConfig(Context c) {
        try {
            JSONObject json;
            try (InputStream in = c.getAssets().open("capacitor.config.json")) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                json = new JSONObject(new String(bo.toByteArray(), StandardCharsets.UTF_8));
            }
            JSONObject server = json.optJSONObject("server");
            if (server == null) { server = new JSONObject(); json.put("server", server); }
            String u = url(c);
            if (u.startsWith("https://")) {
                server.put("url", u);
                String host = Uri.parse(u).getHost();
                JSONArray nav = server.optJSONArray("allowNavigation");
                if (nav == null) { nav = new JSONArray(); server.put("allowNavigation", nav); }
                boolean has = false;
                for (int i = 0; i < nav.length(); i++) if (host != null && host.equalsIgnoreCase(nav.optString(i))) has = true;
                if (!has && host != null) nav.put(host);
            } else {
                server.remove("url");   // ohne Firma: lokale Startseite (Firmen-Code)
            }
            File dir = new File(c.getFilesDir(), "fpcfg");
            if (!dir.exists() && !dir.mkdirs()) return CapConfig.loadDefault(c);
            try (FileOutputStream out = new FileOutputStream(new File(dir, "capacitor.config.json"))) {
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
            return CapConfig.loadFromFile(c, dir.getAbsolutePath() + "/");
        } catch (Exception e) {
            return CapConfig.loadDefault(c);
        }
    }
}
