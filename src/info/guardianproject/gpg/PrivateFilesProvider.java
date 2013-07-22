
package info.guardianproject.gpg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

public class PrivateFilesProvider extends ContentProvider {
    public static final String TAG = "PrivateFilesProvider";
    public static final Uri FILES_URI = Uri.parse("content://info.guardianproject.gpg.PrivateFilesProvider/");
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
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        throw new RuntimeException("Operation not supported");
    }

}
