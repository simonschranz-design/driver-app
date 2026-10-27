package online.sscbusiness.fahrtenplan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Firma wählen / wechseln. Aufruf aus der Startseite (www/index.html) bzw. aus dem Fahrtenplan (Login-Maske):
 *   Capacitor.Plugins.FpTenant.choose({url, name, code})   → speichert und startet die App mit dieser Firma neu
 *   Capacitor.Plugins.FpTenant.current()                   → {url, name, code}
 *   Capacitor.Plugins.FpTenant.reset()                     → zurück zur Firmen-Auswahl
 */
@CapacitorPlugin(name = "FpTenant")
public class FpTenantPlugin extends Plugin {

    @PluginMethod
    public void choose(PluginCall call) {
        String url = call.getString("url", "");
        Uri u = Uri.parse(url);
        if (!url.startsWith("https://") || u.getHost() == null || u.getHost().isEmpty()) { call.reject("Ungültige Adresse."); return; }
        if (!url.equals(FpTenant.url(getContext()))) leaveTenant();
        FpTenant.sp(getContext()).edit()
                .putString("url", url)
                .putString("name", call.getString("name", ""))
                .putString("code", call.getString("code", ""))
                .apply();
        call.resolve();
        restart();
    }

    @PluginMethod
    public void current(PluginCall call) {
        JSObject o = new JSObject();
        o.put("url", FpTenant.sp(getContext()).getString("url", ""));
        o.put("name", FpTenant.sp(getContext()).getString("name", ""));
        o.put("code", FpTenant.sp(getContext()).getString("code", ""));
        call.resolve(o);
    }

    @PluginMethod
    public void reset(PluginCall call) {
        leaveTenant();
        FpTenant.sp(getContext()).edit().clear().apply();
        call.resolve();
        restart();
    }

    /** Beim Firmenwechsel: Standort-Übertragung und Anmeldung des bisherigen Kunden beenden, zwischengespeicherte Dokumente löschen. */
    private void leaveTenant() {
        Context c = getContext();
        try { if (TrackService.running) c.startService(new Intent(c, TrackService.class).setAction(TrackService.ACTION_STOP)); } catch (Exception ignored) {}
        try { TrackStore.clearLogin(c); TrackReceiver.cancelWatchdog(c); TrackService.cancelProblem(c); } catch (Exception ignored) {}
        try { deleteContents(new File(c.getCacheDir(), "docs")); } catch (Exception ignored) {}
    }

    private static void deleteContents(File dir) {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return;
        for (File f : files) { if (f.isDirectory()) deleteContents(f); f.delete(); }
    }

    private void restart() {
        Activity a = getActivity();
        if (a == null) return;
        // kurz warten, damit die Antwort noch bei der Seite ankommt, dann mit neuer Konfiguration neu aufbauen
        a.getWindow().getDecorView().postDelayed(() -> { if (!a.isFinishing()) a.recreate(); }, 150);
    }
}
