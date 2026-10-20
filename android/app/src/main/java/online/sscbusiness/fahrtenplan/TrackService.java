package online.sscbusiness.fahrtenplan;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Nativer Tracking-Dienst (Vordergrunddienst): holt Positionen von Google Fused Location, legt Spurpunkte in
 * eine Datei-Warteschlange und sendet sie samt Live-Status an track.php – unabhängig von der Webseite in der App.
 * Meldet Probleme (GPS aus, Berechtigung fehlt, abgemeldet) per Benachrichtigung.
 */
public class TrackService extends Service {
    static final String ACTION_STOP = "online.sscbusiness.fahrtenplan.TRACK_STOP";
    private static final String CH_RUN = "fp_track_run";
    private static final String CH_PROBLEM = "fp_track_problem";
    private static final int ID_RUN = 4711, ID_PROBLEM = 4712;

    static volatile boolean running = false;
    // Zustand für die App-Anzeige (status())
    static volatile long lastFixMs = 0, lastSentMs = 0;
    static volatile double lastLat = 0, lastLng = 0;
    static volatile String problem = "";

    private HandlerThread ht;
    private Handler h;
    private FusedLocationProviderClient fused;
    private LocationCallback fusedCb;
    private LocationManager lm;
    private LocationListener lmListener;
    private boolean usingFused = false;

    private Location lastFix;           // letzter brauchbarer Fix
    private long lastFixAt = 0;
    private double qLat = 0, qLng = 0;  // zuletzt in die Warteschlange gelegter Punkt
    private long qAt = 0;
    private boolean dirty = false;
    private long lastLive = 0, lastUpload = 0;
    private boolean uploading = false;
    private String shownProblem = "";

    static void startSafely(Context ctx, boolean fromBackground) {
        try {
            Intent i = new Intent(ctx, TrackService.class);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
        } catch (Exception e) {
            // Android erlaubt den Start aus dem Hintergrund nicht (z. B. Akku-Optimierung aktiv) → Fahrer informieren
            if (fromBackground) notifyProblem(ctx, "Standort-Übertragung pausiert", "Bitte die Fahrtenplan-App einmal öffnen, damit der Standort wieder übertragen wird.");
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        ht = new HandlerThread("fp-track");
        ht.start();
        h = new Handler(ht.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            // Abmeldung: letzte Punkte noch senden, dann alles löschen und beenden
            startForegroundCompat(buildRunNotification("Wird beendet …"));
            h.post(new Runnable() { public void run() {
                try { if (TrackStore.active(TrackService.this)) flush(true); } catch (Exception ignored) {}
                TrackStore.clearLogin(TrackService.this);
                TrackReceiver.cancelWatchdog(TrackService.this);
                cancelProblem(TrackService.this);
                shutdown();
                stopSelf();
            }});
            return START_NOT_STICKY;
        }
        if (!TrackStore.active(this)) {
            startForegroundCompat(buildRunNotification("Nicht angemeldet"));
            shutdown();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundCompat(buildRunNotification("Standort wird übertragen"));
        if (!running) {
            running = true;
            startLocation();
            h.postDelayed(tick, 3000);
        }
        TrackReceiver.scheduleWatchdog(this);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App aus der Übersicht gewischt: Dienst läuft weiter. Falls Android ihn trotzdem beendet, startet ihn die Weckung neu.
        TrackReceiver.scheduleWatchdog(this);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        shutdown();
        if (ht != null) ht.quitSafely();
        super.onDestroy();
    }

    private void shutdown() {
        running = false;
        try { if (h != null) h.removeCallbacksAndMessages(null); } catch (Exception ignored) {}
        try { if (fused != null && fusedCb != null) fused.removeLocationUpdates(fusedCb); } catch (Exception ignored) {}
        try { if (lm != null && lmListener != null) lm.removeUpdates(lmListener); } catch (Exception ignored) {}
        fused = null; fusedCb = null; lmListener = null;
    }

    // ---------- Benachrichtigungen ----------
    private static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel run = new NotificationChannel(CH_RUN, "Standort-Übertragung", NotificationManager.IMPORTANCE_LOW);
        run.setDescription("Dauerhafte Anzeige, solange der Standort für die Disposition übertragen wird.");
        nm.createNotificationChannel(run);
        NotificationChannel pr = new NotificationChannel(CH_PROBLEM, "Probleme bei der Standort-Übertragung", NotificationManager.IMPORTANCE_HIGH);
        pr.setDescription("Warnung, wenn GPS aus ist, die Berechtigung fehlt oder die Anmeldung beendet wurde.");
        nm.createNotificationChannel(pr);
    }

    private static PendingIntent openAppIntent(Context c) {
        Intent li = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (li == null) li = new Intent();
        li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(c, 0, li, fl);
    }

    private Notification buildRunNotification(String text) {
        ensureChannels(this);
        return new NotificationCompat.Builder(this, CH_RUN)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Fahrtenplan")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openAppIntent(this))
                .build();
    }

    private void startForegroundCompat(Notification n) {
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID_RUN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(ID_RUN, n);
        } catch (Exception e) {
            // z. B. fehlende Standort-Berechtigung: Dienst nicht als Vordergrunddienst möglich
            notifyProblem(this, "Standort-Berechtigung fehlt", "Bitte in den App-Einstellungen den Standort auf «Immer zulassen» stellen.");
            stopSelf();
        }
    }

    private void updateRunNotification(String text) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(ID_RUN, buildRunNotification(text));
        } catch (Exception ignored) {}
    }

    static void notifyProblem(Context c, String title, String text) {
        try {
            ensureChannels(c);
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Notification n = new NotificationCompat.Builder(c, CH_PROBLEM)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setContentIntent(openAppIntent(c))
                    .build();
            nm.notify(ID_PROBLEM, n);
        } catch (Exception ignored) {}
    }

    static void cancelProblem(Context c) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(ID_PROBLEM);
        } catch (Exception ignored) {}
        problem = "";
    }

    // ---------- Standort ----------
    static boolean hasFine(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    static boolean hasBackground(Context c) {
        if (Build.VERSION.SDK_INT < 29) return hasFine(c);
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    static boolean gpsOff(Context c) {
        try {
            LocationManager m = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            if (m == null) return false;
            return !(m.isProviderEnabled(LocationManager.GPS_PROVIDER) || m.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        } catch (Exception e) { return false; }
    }

    private void startLocation() {
        if (!hasFine(this)) return; // tick() meldet das Problem; Start wird beim nächsten Takt erneut versucht
        boolean play = false;
        try { play = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS; } catch (Throwable ignored) {}
        try {
            if (play) {
                fused = LocationServices.getFusedLocationProviderClient(this);
                LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
                        .setMinUpdateIntervalMillis(8000)
                        .setMinUpdateDistanceMeters(15f)
                        .setWaitForAccurateLocation(false)
                        .build();
                fusedCb = new LocationCallback() {
                    @Override public void onLocationResult(LocationResult r) {
                        if (r == null) return;
                        for (Location l : r.getLocations()) onFix(l);
                    }
                };
                fused.requestLocationUpdates(req, fusedCb, ht.getLooper());
                usingFused = true;
            } else {
                startPlainLocation();
            }
        } catch (SecurityException se) {
            fused = null; fusedCb = null;
        } catch (Throwable t) {
            try { startPlainLocation(); } catch (Throwable ignored) {}
        }
    }

    private void startPlainLocation() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lmListener = new LocationListener() {
            @Override public void onLocationChanged(Location l) { onFix(l); }
            @Override public void onStatusChanged(String p, int s, Bundle e) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 15f, lmListener, ht.getLooper());
        } catch (SecurityException ignored) {}
    }

    private static double distM(double aLat, double aLng, double bLat, double bLng) {
        float[] r = new float[1];
        Location.distanceBetween(aLat, aLng, bLat, bLng, r);
        return r[0];
    }

    private static String iso(long ms) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(ms));
    }
    private static String dateKey(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms)); // lokale Zeit wie in der Webseite
    }
    private static double r5(double x) { return Math.round(x * 1e5) / 1e5; }

    private void onFix(Location l) {
        if (l == null || !TrackStore.active(this)) return;
        long now = System.currentTimeMillis();
        float acc = l.hasAccuracy() ? l.getAccuracy() : 0;
        // sehr ungenaue Fixe verwerfen, solange ein brauchbarer jünger als 5 Min. ist
        if (acc > 150 && lastFix != null && now - lastFixAt < 300000) return;
        lastFix = l; lastFixAt = now; dirty = true;
        lastFixMs = now; lastLat = l.getLatitude(); lastLng = l.getLongitude();
        double moved = qAt == 0 ? 1e9 : distM(qLat, qLng, l.getLatitude(), l.getLongitude());
        long dt = qAt == 0 ? Long.MAX_VALUE / 2 : now - qAt;
        if (qAt == 0 || (moved >= 40 && dt >= 30000) || dt >= 300000) queuePoint(l.getLatitude(), l.getLongitude(),
                l.hasSpeed() ? l.getSpeed() : Float.NaN, now);
    }

    private void queuePoint(double lat, double lng, float speed, long now) {
        try {
            String empId = TrackStore.get(this, "employeeId");
            String vid = TrackStore.get(this, "vehicleId");
            String vl = TrackStore.get(this, "vehicleLabel");
            if (vid.isEmpty()) { vid = "fahrer-" + empId; vl = "Fahrer " + TrackStore.get(this, "employeeName"); }
            JSONObject p = new JSONObject();
            p.put("lat", r5(lat)); p.put("lng", r5(lng)); p.put("t", iso(now));
            if (!Float.isNaN(speed)) p.put("speed", Math.round(speed * 10) / 10.0);
            JSONObject q = new JSONObject();
            q.put("v", vid); q.put("vl", vl); q.put("d", dateKey(now)); q.put("p", p);
            TrackStore.queueAdd(this, q);
            qLat = lat; qLng = lng; qAt = now;
        } catch (Exception ignored) {}
    }

    // ---------- Takt: Lebenszeichen, Versand, Problemerkennung ----------
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try { doTick(); } catch (Throwable ignored) {}
            if (running) h.postDelayed(this, 15000);
        }
    };

    private void doTick() {
        if (!TrackStore.active(this)) { shutdown(); stopSelf(); return; }
        long now = System.currentTimeMillis();
        checkProblems();
        if (fused == null && lm == null && hasFine(this)) startLocation(); // Berechtigung wurde inzwischen erteilt
        // im Stand: alle 5 Min. ein Spurpunkt an der letzten bekannten Position (Fix höchstens 15 Min. alt)
        if (lastFix != null && now - lastFixAt < 900000 && (qAt == 0 || now - qAt >= 300000))
            queuePoint(lastFix.getLatitude(), lastFix.getLongitude(), 0f, now);
        boolean haveQ = TrackStore.queueSize(this) > 0;
        boolean due = (haveQ && now - lastUpload >= 60000) || (dirty && now - lastLive >= 30000) || (now - lastLive >= 120000);
        if (due && !uploading) flush(false);
        TrackReceiver.scheduleWatchdog(this);
    }

    private String lastNotifText = "";
    private void checkProblems() {
        String prob = "", title = "", text = "";
        if (!hasFine(this)) {
            prob = "perm"; title = "Standort-Berechtigung fehlt";
            text = "Die Fahrtenplan-App darf den Standort nicht nutzen. Bitte in den App-Einstellungen «Standort → Immer zulassen» wählen.";
        } else if (gpsOff(this)) {
            prob = "gps"; title = "GPS ausgeschaltet";
            text = "Der Standort ist am Handy ausgeschaltet. Bitte GPS (Standort) einschalten, sonst sieht die Disposition dich nicht.";
        }
        problem = prob;
        if (!prob.equals(shownProblem)) {
            shownProblem = prob;
            if (prob.isEmpty()) cancelProblem(this); else notifyProblem(this, title, text);
        }
    }

    /** Versand: Warteschlange (max. 200 Punkte) + Live-Status. Auf dem Tick-Thread bzw. beim Beenden synchron. */
    private void flush(boolean finalFlush) {
        if (uploading && !finalFlush) return;
        uploading = true;
        try {
            long now = System.currentTimeMillis();
            JSONArray pts = TrackStore.queuePeek(this, 200);
            JSONObject body = new JSONObject();
            body.put("points", pts);
            body.put("status", buildStatus());
            int code = TrackStore.post(this, body);
            if (code == 200) {
                TrackStore.queueDrop(this, pts.length());
                lastLive = now; lastUpload = now; lastSentMs = now; dirty = false;
                TrackStore.sp(this).edit().putLong("lastSentMs", now).apply();
                updateRunNotification("Standort übertragen · " + new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date(now)));
                // weitere Punkte (grosse Rückstände nach Funkloch) gleich nachschieben
                if (!finalFlush && TrackStore.queueSize(this) > 0) { uploading = false; h.postDelayed(new Runnable() { public void run() { if (running) flush(false); } }, 1500); return; }
            } else if (code == 401 || code == 403) {
                onLoggedOut();
            } else {
                lastUpload = now; // Netz weg / Serverfehler: nächster Versuch beim nächsten Takt nach 60 s
                updateRunNotification("Wartet auf Verbindung · " + TrackStore.queueSize(this) + " Punkte gespeichert");
            }
        } catch (Exception ignored) {
        } finally {
            uploading = false;
        }
    }

    private void onLoggedOut() {
        notifyProblem(this, "Abgemeldet – bitte anmelden", "Die Fahrtenplan-App wurde abgemeldet. Bitte öffnen und neu anmelden, sonst wird der Standort nicht mehr übertragen.");
        TrackStore.sp(this).edit().putBoolean("active", false).putBoolean("lost", true).remove("token").apply();
        TrackReceiver.cancelWatchdog(this);
        shutdown();
        stopForeground(true);
        stopSelf();
    }

    private JSONObject buildStatus() throws Exception {
        JSONObject s = new JSONObject();
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            int lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), sc = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (lvl >= 0 && sc > 0) s.put("battery", Math.round(lvl * 100f / sc));
            int st = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            s.put("charging", st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL);
        }
        s.put("queued", TrackStore.queueSize(this));
        s.put("appV", TrackStore.get(this, "webV"));
        s.put("apkV", apkVersion(this));
        boolean ign = true;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            ign = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        s.put("battIgnoring", ign);
        s.put("perm", hasFine(this) ? "ok" : "denied");
        s.put("permBg", hasBackground(this));
        s.put("gpsOff", gpsOff(this));
        String vid = TrackStore.get(this, "vehicleId");
        s.put("vehicleId", vid.isEmpty() ? JSONObject.NULL : vid);
        s.put("vehicleLabel", TrackStore.get(this, "vehicleLabel"));
        s.put("vehicleSrc", TrackStore.get(this, "vehicleSrc"));
        if (lastFix != null) {
            s.put("lat", r5(lastFix.getLatitude())); s.put("lng", r5(lastFix.getLongitude()));
            if (lastFix.hasAccuracy()) s.put("accuracy", Math.round(lastFix.getAccuracy()));
            if (lastFix.hasSpeed()) s.put("speed", Math.round(lastFix.getSpeed() * 10) / 10.0);
            if (lastFix.hasBearing()) s.put("heading", Math.round(lastFix.getBearing()));
            s.put("fixAt", iso(lastFixAt));
        }
        return s;
    }

    static String apkVersion(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; }
        catch (Exception e) { return ""; }
    }
}
