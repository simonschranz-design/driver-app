package online.sscbusiness.fahrtenplan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * Brücke zwischen der Webseite und dem nativen Tracking-Dienst.
 * Die Webseite übergibt nur die Anmeldung (start/update) und beendet den Dienst beim Abmelden (stop);
 * Aufzeichnen, Zwischenspeichern, Senden, Neustart und Warnungen macht der Dienst selbst.
 */
@CapacitorPlugin(
        name = "NativeTrack",
        permissions = {
                @Permission(alias = "location", strings = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}),
                @Permission(alias = "background", strings = {Manifest.permission.ACCESS_BACKGROUND_LOCATION}),
                @Permission(alias = "notifications", strings = {Manifest.permission.POST_NOTIFICATIONS})
        }
)
public class NativeTrackPlugin extends Plugin {

    /** start({baseUrl, apiKey, token, employeeId, employeeName, webV, vehicleId, vehicleLabel, vehicleSrc}) */
    @PluginMethod
    public void start(PluginCall call) {
        Context c = getContext();
        String base = call.getString("baseUrl", "");
        String token = call.getString("token", "");
        if (base.isEmpty() || token.isEmpty()) { call.reject("Anmeldedaten fehlen."); return; }
        TrackStore.sp(c).edit()
                .putBoolean("active", true).putBoolean("lost", false)
                .putString("baseUrl", base)
                .putString("apiKey", call.getString("apiKey", ""))
                .putString("token", token)
                .putString("employeeId", call.getString("employeeId", ""))
                .putString("employeeName", call.getString("employeeName", ""))
                .putString("webV", call.getString("webV", ""))
                .putString("vehicleId", call.getString("vehicleId", ""))
                .putString("vehicleLabel", call.getString("vehicleLabel", ""))
                .putString("vehicleSrc", call.getString("vehicleSrc", ""))
                .apply();
        TrackService.cancelProblem(c);
        // Berechtigungen der Reihe nach erfragen (Standort → Benachrichtigungen → «Immer zulassen»)
        stepPermissions(call);
    }

    private void stepPermissions(PluginCall call) {
        if (getPermissionState("location") != com.getcapacitor.PermissionState.GRANTED) {
            // Google Play: vor der Android-Abfrage erklären, wozu der Standort (auch im Hintergrund) gebraucht wird
            if (!TrackStore.sp(getContext()).getBoolean("disclosureOk", false)) {
                FpDialogs.locationDisclosure(getActivity(), ok -> {
                    if (ok) {
                        TrackStore.sp(getContext()).edit().putBoolean("disclosureOk", true).apply();
                        requestPermissionForAlias("location", call, "permStep");
                    } else {
                        finishStart(call); // ohne Standort: der Dienst meldet «Berechtigung fehlt», beim nächsten Start wird erneut gefragt
                    }
                });
                return;
            }
            requestPermissionForAlias("location", call, "permStep");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != com.getcapacitor.PermissionState.GRANTED
                && !getContext().getSharedPreferences("fp_track", Context.MODE_PRIVATE).getBoolean("askedNotif", false)) {
            getContext().getSharedPreferences("fp_track", Context.MODE_PRIVATE).edit().putBoolean("askedNotif", true).apply();
            requestPermissionForAlias("notifications", call, "permStep");
            return;
        }
        if (Build.VERSION.SDK_INT >= 29 && getPermissionState("background") != com.getcapacitor.PermissionState.GRANTED
                && !getContext().getSharedPreferences("fp_track", Context.MODE_PRIVATE).getBoolean("askedBg", false)) {
            getContext().getSharedPreferences("fp_track", Context.MODE_PRIVATE).edit().putBoolean("askedBg", true).apply();
            requestPermissionForAlias("background", call, "permStep");
            return;
        }
        finishStart(call);
    }

    @PermissionCallback
    private void permStep(PluginCall call) {
        stepPermissions(call);
    }

    private void finishStart(PluginCall call) {
        Context c = getContext();
        TrackService.startSafely(c, false);
        TrackReceiver.scheduleWatchdog(c);
        // Akku-Hinweis beim ersten Start (höchstens zweimal, damit es nicht nervt) – nur wenn der Standort erlaubt ist
        boolean ign = batteryIgnoring();
        int asked = TrackStore.sp(c).getInt("battAsked", 0);
        if (!ign && asked < 2 && TrackService.hasFine(c)) {
            TrackStore.sp(c).edit().putInt("battAsked", asked + 1).apply();
            FpDialogs.batteryHint(getActivity());
        }
        call.resolve(statusObj());
    }

    /** update({token, vehicleId, vehicleLabel, vehicleSrc, webV}) – laufend aufgerufen, hält Dienst und Daten aktuell. */
    @PluginMethod
    public void update(PluginCall call) {
        Context c = getContext();
        if (!TrackStore.active(c)) { call.resolve(statusObj()); return; }
        android.content.SharedPreferences.Editor e = TrackStore.sp(c).edit();
        String t = call.getString("token", "");
        if (!t.isEmpty()) e.putString("token", t);
        if (call.hasOption("vehicleId")) {
            e.putString("vehicleId", call.getString("vehicleId", ""))
             .putString("vehicleLabel", call.getString("vehicleLabel", ""))
             .putString("vehicleSrc", call.getString("vehicleSrc", ""));
        }
        String wv = call.getString("webV", "");
        if (!wv.isEmpty()) e.putString("webV", wv);
        e.apply();
        if (!TrackService.running && TrackService.hasFine(c)) TrackService.startSafely(c, false);
        call.resolve(statusObj());
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Context c = getContext();
        if (TrackService.running) {
            try { c.startService(new Intent(c, TrackService.class).setAction(TrackService.ACTION_STOP)); }
            catch (Exception e) { TrackStore.clearLogin(c); TrackReceiver.cancelWatchdog(c); }
        } else {
            TrackStore.clearLogin(c);
            TrackReceiver.cancelWatchdog(c);
            TrackService.cancelProblem(c);
        }
        TrackStore.sp(c).edit().putBoolean("lost", false).apply();
        call.resolve();
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(statusObj());
    }

    private JSObject statusObj() {
        Context c = getContext();
        JSObject o = new JSObject();
        o.put("running", TrackService.running);
        o.put("active", TrackStore.active(c));
        o.put("lost", TrackStore.sp(c).getBoolean("lost", false));
        o.put("perm", TrackService.hasFine(c) ? "ok" : "denied");
        o.put("permBg", TrackService.hasBackground(c));
        o.put("gpsOff", TrackService.gpsOff(c));
        o.put("battIgnoring", batteryIgnoring());
        o.put("queued", TrackStore.queueSize(c));
        o.put("lastFixMs", TrackService.lastFixMs);
        long sent = Math.max(TrackService.lastSentMs, TrackStore.sp(c).getLong("lastSentMs", 0));
        o.put("lastSentMs", sent);
        o.put("apkV", TrackService.apkVersion(c));
        return o;
    }

    private boolean batteryIgnoring() {
        if (Build.VERSION.SDK_INT < 23) return true;
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
    }

    /** Früher: direkte Akku-Ausnahme (eingeschränkte Berechtigung). Jetzt: App-Einstellungen öffnen (Akku → «Uneingeschränkt»). */
    @PluginMethod
    public void requestBatteryExemption(PluginCall call) {
        if (FpDialogs.openAppSettings(getContext())) call.resolve(); else call.reject("Einstellungen konnten nicht geöffnet werden.");
    }

    /** Hersteller/Modell – die Webseite zeigt damit den Hinweis «Nie schlafende Apps» für Samsung/Xiaomi u. a. */
    @PluginMethod
    public void deviceInfo(PluginCall call) {
        JSObject o = new JSObject();
        o.put("manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
        o.put("model", Build.MODEL == null ? "" : Build.MODEL);
        o.put("sdk", Build.VERSION.SDK_INT);
        call.resolve(o);
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Einstellungen konnten nicht geöffnet werden.");
        }
    }

    @PluginMethod
    public void openLocationSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Einstellungen konnten nicht geöffnet werden.");
        }
    }
}
