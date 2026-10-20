package online.sscbusiness.fahrtenplan;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Eigene Plugins müssen VOR super.onCreate registriert werden.
        registerPlugin(DocOpenerPlugin.class);
        registerPlugin(NativeTrackPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
