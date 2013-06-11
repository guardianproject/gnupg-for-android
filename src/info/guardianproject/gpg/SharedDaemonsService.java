
package info.guardianproject.gpg;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SharedDaemonsService extends Service {

    public static final String TAG = "PinentryService";
    private static final int SERVICE_FOREGROUND_ID = 8473;

    private DirmngrThread dirmngrThread;

    private void startDaemons() {
        Log.i(TAG, "start daemons in " + NativeHelper.app_opt.getAbsolutePath());
        synchronized (this) {

            dirmngrThread = new DirmngrThread();
            dirmngrThread.start();
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        // since this service is a separate process, it has its own instance of
        // NativeHelper
        NativeHelper.setup(this);
        goForeground();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    public class LocalBinder extends Binder {
        public SharedDaemonsService getService() {
            return SharedDaemonsService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        startDaemons();
        return START_STICKY;
    }

    private void goForeground() {
        Log.d(TAG, "goForeground()");

        startForeground(SERVICE_FOREGROUND_ID, buildNotification());
    }

    private Notification buildNotification() {

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.drawable.icon);
        b.setContentTitle(getText(R.string.pinentry_service_label));
        b.setContentText(getText(R.string.pinentry_service_started));
        b.setTicker(getText(R.string.pinentry_service_started));
        b.setDefaults(Notification.DEFAULT_VIBRATE);
        b.setWhen(System.currentTimeMillis());
        b.setOngoing(true);
        b.setContentIntent(PendingIntent.getService(getApplicationContext(), 0,
                new Intent(), 0));

        return b.getNotification();
    }

    class DirmngrThread extends Thread {

        @Override
        public void run() {
            NativeHelper.kill9(NativeHelper.dirmngr);
            String dirmngrCmd = NativeHelper.dirmngr
                    + " --daemon " + "--debug-level guru --log-file "
                    + NativeHelper.app_log + "/dirmngr.log";
            String chmodCmd = "chmod 777 " + NativeHelper.app_opt + "/var/run/gnupg/S.dirmngr";
            try {
                Runtime runtime = Runtime.getRuntime();
                Log.i(TAG, dirmngrCmd);
                runtime.exec(dirmngrCmd, NativeHelper.envp, NativeHelper.app_home)
                        .waitFor();
                Log.i(TAG, chmodCmd);
                runtime.exec(chmodCmd, NativeHelper.envp, NativeHelper.app_home)
                        .waitFor();
            } catch (Exception e) {
                Log.e(TAG, "Could not start dirmngr", e);
            } finally {
                stopSelf();
                synchronized (SharedDaemonsService.this) {
                    dirmngrThread = null;
                }
            }
        }
    }
}
