package online.sscbusiness.fahrtenplan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

/**
 * Hinweis-Dialoge, die Google Play verlangt bzw. die ohne Sonderrechte auskommen:
 *  - Standort-Hinweis («prominent disclosure») VOR der Android-Abfrage der Standort-Berechtigung,
 *  - Akku-Hinweis: öffnet die App-Einstellungen, statt die eingeschränkte Berechtigung
 *    REQUEST_IGNORE_BATTERY_OPTIMIZATIONS zu verwenden.
 * Texte in res/values-XX/ (strings.xml bzw. fp_strings.xml) (Englisch als Standard, dazu de, fr, it, pl, ro, hu).
 */
final class FpDialogs {
    private FpDialogs() {}

    interface Answer { void done(boolean accepted); }

    /** Standort-Hinweis. Antwort true = «Zustimmen». */
    static void locationDisclosure(Activity a, Answer answer) {
        if (a == null || a.isFinishing()) { answer.done(false); return; }
        a.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(a)
                        .setTitle(R.string.fp_disclosure_title)
                        .setMessage(R.string.fp_disclosure_text)
                        .setCancelable(false)
                        .setPositiveButton(R.string.fp_disclosure_ok, (d, w) -> answer.done(true))
                        .setNegativeButton(R.string.fp_disclosure_no, (d, w) -> answer.done(false))
                        .show();
            } catch (Exception e) {
                answer.done(false);
            }
        });
    }

    /** Akku-Hinweis mit Knopf zu den App-Einstellungen (dort: Akku → «Uneingeschränkt»). */
    static void batteryHint(Activity a) {
        if (a == null || a.isFinishing()) return;
        a.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(a)
                        .setTitle(R.string.fp_battery_title)
                        .setMessage(R.string.fp_battery_text)
                        .setPositiveButton(R.string.fp_battery_open, (d, w) -> openAppSettings(a))
                        .setNegativeButton(R.string.fp_later, null)
                        .show();
            } catch (Exception ignored) {}
        });
    }

    /** App-Infos öffnen (Berechtigungen, Akku). Fällt auf die Liste «Akku-Optimierung» zurück. */
    static boolean openAppSettings(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            try {
                Intent i2 = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i2);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }
}
