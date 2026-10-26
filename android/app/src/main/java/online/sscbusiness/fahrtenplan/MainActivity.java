package online.sscbusiness.fahrtenplan;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Eigene Plugins müssen VOR super.onCreate registriert werden.
        registerPlugin(DocOpenerPlugin.class);
        registerPlugin(NativeTrackPlugin.class);
        registerPlugin(FpTenantPlugin.class);
        // Gewählte Firma (Mandant) als Startadresse – ohne Firma: Startseite mit Firmen-Code (www/index.html)
        this.config = FpTenant.buildConfig(this);
        super.onCreate(savedInstanceState);
    }
}
