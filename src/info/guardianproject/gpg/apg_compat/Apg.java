/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.gpg.apg_compat;

import java.util.regex.Pattern;

public class Apg {

    public static class Intent {
        public static final String DECRYPT = "org.thialfihar.android.apg.intent.DECRYPT";
        public static final String ENCRYPT = "org.thialfihar.android.apg.intent.ENCRYPT";
        public static final String DECRYPT_FILE = "org.thialfihar.android.apg.intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = "org.thialfihar.android.apg.intent.ENCRYPT_FILE";
        public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
        public static final String ENCRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.ENCRYPT_AND_RETURN";
        public static final String SELECT_PUBLIC_KEYS = "org.thialfihar.android.apg.intent.SELECT_PUBLIC_KEYS";
        public static final String SELECT_SECRET_KEY = "org.thialfihar.android.apg.intent.SELECT_SECRET_KEY";
        public static final String IMPORT = "org.thialfihar.android.apg.intent.IMPORT";
        public static final String LOOK_UP_KEY_ID = "org.thialfihar.android.apg.intent.LOOK_UP_KEY_ID";
        public static final String LOOK_UP_KEY_ID_AND_RETURN = "org.thialfihar.android.apg.intent.LOOK_UP_KEY_ID_AND_RETURN";
        public static final String GENERATE_SIGNATURE = "org.thialfihar.android.apg.intent.GENERATE_SIGNATURE";
    }

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_DECRYPTED_DATA = "decryptedData";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String EXTRA_ENCRYPTED_DATA = "encryptedData";
    public static final String EXTRA_RESULT_URI = "resultUri";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_SIGNATURE_DATA = "signatureData";
    public static final String EXTRA_SIGNATURE_TEXT = "signatureText";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER_IDS = "userIds";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_REPLY_TO = "replyTo";
    public static final String EXTRA_SEND_TO = "sendTo";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_ASCII_ARMOUR = "asciiArmour";
    public static final String EXTRA_BINARY = "binary";
    public static final String EXTRA_KEY_SERVERS = "keyServers";

    private static String VERSION = null;

    public static Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                    Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static Pattern PGP_PUBLIC_KEY =
            Pattern.compile(
                    ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
                    Pattern.DOTALL);

    /**
     * Convert the GPG user ID format to the APG userID format.
     * 
     * @param userId as String[3] of Name, Email, Comment
     * @return userId as single String
     */
    public static String userId(String[] userId) {
        return userId[0] + " " + userId[1] + " " + userId[2];
    }
}
