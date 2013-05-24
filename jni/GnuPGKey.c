#include <jni.h>
#include <stdio.h>

#include "gpgmeutils.h"

#include <gpgme.h>


JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetKey(JNIEnv * env, jobject self,
                                             jlong context, jstring fingerprint,
                                             jboolean secret)
{
    gpgme_key_t key;

    const char *fpr = (*env)->GetStringUTFChars(env, fingerprint, NULL);
    gpgme_error_t err = gpgme_get_key(CONTEXT(context), fpr, &key, secret);
    if (UTILS_onErrorThrowException(env, err)) {
	(*env)->ReleaseStringUTFChars(env, fingerprint, fpr);
	return LNG(NULL);
    }
    (*env)->ReleaseStringUTFChars(env, fingerprint, fpr);

    //setMembers(env, self, key);

    return LNG(key);
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeKeyUnref(JNIEnv * env, jobject self,
					       jlong key)
{
    gpgme_key_unref(KEY(key));
    return 0l;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetName(JNIEnv * env, jobject self,
					      jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->uids->name);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetEmail(JNIEnv * env, jobject self,
					       jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->uids->email);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetKeyID(JNIEnv * env, jobject self,
					       jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->subkeys->keyid);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetFingerprint(JNIEnv * env, jobject self,
						     jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->subkeys->fpr);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetComment(JNIEnv * env, jobject self,
						 jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->uids->comment);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetUserID(JNIEnv * env, jobject self,
						jlong key)
{
    jstring str = (*env)->NewStringUTF(env, (KEY(key))->uids->uid);
    return str;
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeGetSignature(JNIEnv * env, jobject self,
						   jlong key)
{
    return LNG((KEY(key))->uids->signatures);
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeCanEncrypt(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->can_encrypt;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeCanSign(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->can_sign;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeCanCerify(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->can_certify;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeCanAuthenticate(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->can_authenticate;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsRevoked(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->revoked;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsExpired(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->expired;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsDisabled(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->disabled;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsInvalid(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->invalid;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsQualified(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->is_qualified;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeIsSecret(JNIEnv * env, jobject self,
						   jlong key)
{
    return (KEY(key))->secret;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGKey_gpgmeHasSecretKey(JNIEnv * env, jobject self,
						   jlong key)
{
    jboolean hasSecretKey = 0;
    gpgme_subkey_t subkey;
    for(subkey = KEY(key)->subkeys; subkey; subkey = subkey->next)
        if (subkey->secret)
            return 1;
    return 0;
}

/* void */
/* setMembers(JNIEnv *env, jobject self, gpgme_key_t key){ */
/*   jclass cls = (*env)->GetObjectClass(env, self); */

/*   jfieldID uid_fld = (*env)->GetFieldID(env, cls, "_uid", "Ljava/lang/String;"); */
/*   if(uid_fld == NULL) return; */
/*   jfieldID name_fld = (*env)->GetFieldID(env, cls, "_name", "Ljava/lang/String;"); */
/*   if(name_fld == NULL) return; */
/*   jfieldID email_fld = (*env)->GetFieldID(env, cls, "_email", "Ljava/lang/String;"); */
/*   if(email_fld == NULL) return; */
/*   jfieldID keyid_fld = (*env)->GetFieldID(env, cls, "_keyid", "Ljava/lang/String;"); */
/*   if(keyid_fld == NULL) return; */
/*   jfieldID fpr_fld = (*env)->GetFieldID(env, cls, "_fpr", "Ljava/lang/String;"); */
/*   if(fpr_fld == NULL) return; */

/*   jstring uid_jstr = (*env)->NewStringUTF(env, key->uids->uid); */
/*   if(uid_jstr == NULL) return; */
/*   (*env)->SetObjectField(env, self, uid_fld, uid_jstr); */

/*   jstring name_jstr = (*env)->NewStringUTF(env, key->uids->name); */
/*   if(name_jstr == NULL) return; */
/*   (*env)->SetObjectField(env, self, name_fld, name_jstr); */

/*   jstring email_jstr = (*env)->NewStringUTF(env, key->uids->email); */
/*   if(email_jstr == NULL) return; */
/*   (*env)->SetObjectField(env, self, email_fld, email_jstr); */

/*   jstring keyid_jstr = (*env)->NewStringUTF(env, key->subkeys->keyid); */
/*   if(keyid_jstr == NULL) return; */
/*   (*env)->SetObjectField(env, self, keyid_fld, keyid_jstr); */

/*   jstring fpr_jstr = (*env)->NewStringUTF(env, key->subkeys->fpr); */
/*   if(fpr_jstr == NULL) return; */
/*   (*env)->SetObjectField(env, self, fpr_fld, fpr_jstr); */

/* } */
