package online.sscbusiness.fahrtenplan;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Lädt ein Dokument von der eigenen Fahrtenplan-Domain herunter (z. B. PDF, Word, Excel, Foto) in den
 * App-Cache und öffnet es mit der passenden App auf dem Handy (ACTION_VIEW über den FileProvider).
 * Die WebView selbst kann weder Downloads noch neue Fenster – deshalb dieser kleine native Helfer.
 * Aufruf aus der Web-App: Capacitor.Plugins.DocOpener.open({url, filename})
 */
@CapacitorPlugin(name = "DocOpener")
public class DocOpenerPlugin extends Plugin {

    /** Gibt zurück, ob die App von der Akku-Optimierung ausgenommen ist (wichtig für zuverlässiges Hintergrund-GPS). */
    @PluginMethod
    public void batteryStatus(PluginCall call) {
        JSObject ret = new JSObject();
        boolean ign = true;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            ign = pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
        }
        ret.put("ignoring", ign);
        call.resolve(ret);
    }

    /** Öffnet die App-Einstellungen (Akku → «Uneingeschränkt») – ohne die eingeschränkte Berechtigung REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. */
    @PluginMethod
    public void requestBatteryExemption(PluginCall call) {
        if (FpDialogs.openAppSettings(getContext())) call.resolve(); else call.reject("Einstellungen konnten nicht geöffnet werden.");
    }

    @PluginMethod
    public void open(final PluginCall call) {
        final String urlStr = call.getString("url");
        final String filename = call.getString("filename", "dokument");
        if (urlStr == null || urlStr.isEmpty()) {
            call.reject("Keine Adresse übergeben.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlStr);
                    // Sicherheit: nur HTTPS und nur von der eigenen Server-Domain der App.
                    String ownHost = Uri.parse(getBridge().getServerUrl()).getHost();
                    if (!"https".equalsIgnoreCase(url.getProtocol())
                            || (ownHost != null && !ownHost.equalsIgnoreCase(url.getHost()))) {
                        call.reject("Adresse nicht erlaubt.");
                        return;
                    }
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        call.reject("Server meldet Fehler (HTTP " + code + ").");
                        return;
                    }
                    String safe = filename.replaceAll("[^A-Za-z0-9._ \\-]", "_");
                    if (safe.isEmpty()) safe = "dokument";
                    String mime = conn.getContentType();
                    if (mime != null && mime.contains(";")) mime = mime.substring(0, mime.indexOf(';')).trim();
                    String ext = MimeTypeMap.getFileExtensionFromUrl(safe.replace(" ", "_"));
                    if (mime == null || mime.isEmpty() || mime.equals("application/octet-stream") || mime.startsWith("text/")) {
                        String guessed = (ext == null || ext.isEmpty()) ? null
                                : MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.ROOT));
                        if (guessed != null) mime = guessed;
                    }
                    if (mime == null || mime.isEmpty()) mime = "*/*";

                    Context ctx = getContext();
                    File dir = new File(ctx.getCacheDir(), "docs");
                    if (!dir.exists()) dir.mkdirs();
                    File[] old = dir.listFiles();
                    if (old != null) for (File f : old) f.delete(); // Cache klein halten
                    File out = new File(dir, safe);
                    InputStream in = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(out);
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.close();
                    in.close();

                    Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", out);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        ctx.startActivity(intent);
                        JSObject ret = new JSObject();
                        ret.put("mime", mime);
                        call.resolve(ret);
                    } catch (ActivityNotFoundException e) {
                        call.reject("Keine App zum Öffnen dieses Dateityps installiert.");
                    }
                } catch (Exception e) {
                    call.reject("Dokument konnte nicht geladen werden: " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }
}
