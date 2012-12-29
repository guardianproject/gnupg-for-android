package info.guardianproject.gpg.pinentry;

import info.guardianproject.gpg.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;


public class PINEntry extends Activity {

	ServerSocketThread mServerSocket;
	public static String SOCKET_ADDRESS = "info.guardianproject.gpg.pinentry";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( mServerSocket != null ) {
        	mServerSocket.setStopThread(true);
        	mServerSocket.stop();
        }
        mServerSocket = new ServerSocketThread();
        mServerSocket.start();

        setContentView(R.layout.activity_pinentry);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pinentry, menu);
        return true;
    }
}
