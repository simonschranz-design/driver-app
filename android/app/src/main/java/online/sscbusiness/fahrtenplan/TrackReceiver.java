package online.sscbusiness.fahrtenplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Startet den Tracking-Dienst automatisch wieder:
 *  - nach Handy-Neustart (BOOT_COMPLETED) und nach einem App-Update (MY_PACKAGE_REPLACED),
 *  - durch eine Kontroll-Weckung alle ~10 Min. (falls Android den Dienst beendet hat).
 * Nur wenn ein Fahrer angemeldet ist (TrackStore.active). Ohne Anmeldung passiert nichts.
 */
public class TrackReceiver extends BroadcastReceiver {
    static final String ACTION_WATCHDOG = "online.sscbusiness.fahrtenplan.TRACK_WATCHDOG";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !TrackStore.active(ctx)) return;
        TrackService.startSafely(ctx, true);
        scheduleWatchdog(ctx);
    }

    static void scheduleWatchdog(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent i = new Intent(ctx, TrackReceiver.class).setAction(ACTION_WATCHDOG);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 7, i, fl);
            long at = System.currentTimeMillis() + 10 * 60 * 1000L;
            if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.set(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception ignored) {}
    }

    static void cancelWatchdog(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(ctx, TrackReceiver.class).setAction(ACTION_WATCHDOG);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            if (am != null) am.cancel(PendingIntent.getBroadcast(ctx, 7, i, fl));
        } catch (Exception ignored) {}
    }
}
