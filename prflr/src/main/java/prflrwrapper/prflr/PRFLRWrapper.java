package prflrwrapper.prflr;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class PRFLRWrapper extends Application{
    private PRFLRWrapper() {
    }

    public static void init(Context c) throws Exception {
        if(PRFLR.init)
            return;
        Log.d("PRFLR", "Init");
        PackageInfo pInfo;
        String version;
        try {
            pInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("PRFLR", e.toString());
            version = "?";
        }
        ApplicationInfo inf = c.getPackageManager().getApplicationInfo(c.getPackageName(),PackageManager.GET_META_DATA);
        String apiKey = inf.metaData.getString("apiKey");
        String source = version;
        PRFLR.init(source, apiKey);
    }
    public static void setOveflowCounter(int value) {
        PRFLR.overflowCount = value;
    }
    public static void begin(String timerName) throws Exception {
        PRFLR.begin(timerName);
    }

    public static void end(String timerName, String info) throws Exception {
        PRFLR.end(timerName, info);
    }
    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String version = Build.VERSION.RELEASE;
        if (model.startsWith(manufacturer)) {
            return model + " " + version;
        } else {
            return manufacturer + " " + model + " " + version;
        }
    }
}
