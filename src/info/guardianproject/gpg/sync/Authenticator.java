
package info.guardianproject.gpg.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * We don't do traditional authentication like a web service would.
 * So this class is very bare bones, but still required.
 *
 * Conceivably the "properties" methods could be used to invoke
 * an Activity whereby the user is able to configure his primary
 * PGP identity.
 *
 * Account creation is handled by calling AccountManager.addAccountExplicitly()
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private final static String TAG = Authenticator.class.getSimpleName();

    // Authentication Service context
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        Log.v(TAG, "addAccount()");
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {

        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {

        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {

        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {

        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        return null;
    }

}
