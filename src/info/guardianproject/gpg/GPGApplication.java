package info.guardianproject.gpg;

import android.app.Application;
import android.content.Context;

public class GPGApplication extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        GPGApplication.context = getApplicationContext();
    }

    public static Context getGlobalApplicationContext() {
        return GPGApplication.context;
    }

}
