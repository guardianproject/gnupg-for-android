
package info.guardianproject.gpg;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class PrivateFilesProvider extends ContentProvider {
    public static final String TAG = "PrivateFilesProvider";
    public static final Uri FILES_URI = Uri
            .parse("content://info.guardianproject.gpg.PrivateFilesProvider/");
    private MimeTypeMap mimeTypeMap;

    @Override
    public boolean onCreate() {
        mimeTypeMap = MimeTypeMap.getSingleton();
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        List<String> pathSegments = uri.getPathSegments();
        File filesDir = getContext().getFilesDir();
        File privateFile = new File(filesDir, pathSegments.get(pathSegments.size() - 1));
        return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        return mimeTypeMap.getMimeTypeFromExtension(fileExtension);
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        Log.v(TAG, String.format("delete(%s, %s, %s)",
                arg0.toString(), arg1, arrayToString(arg2)));
        return 0;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        Log.v(TAG, String.format("insert(%s, %s)", arg0.toString(), arg1.toString()));
        return null;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        Log.v(TAG, String.format("query(%s, %s, %s, %s, %s)",
                arg0.toString(), arrayToString(arg1), arg2, arrayToString(arg3), arg4));
        return null;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        Log.v(TAG, String.format("update(%s, %s, %s, %s, %s)",
                arg0.toString(), arg1.toString(), arg2, arrayToString(arg3)));
        return 0;
    }

    private String arrayToString(String[] a) {
        String separator = ", ";
        StringBuffer result = new StringBuffer("{ ");
        if (a == null)
            return "{ null }";
        else if (a.length == 0)
            return "";
        else {
            result.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                result.append(separator);
                result.append(a[i]);
            }
        }
        result.append(" }");
        return result.toString();
    }

}
