#include <jni.h>
#include <stdio.h>
#include "com_freiheit_gnupg_GnuPGSignature.h"
#include "gpgmeutils.h"

#include <gpgme.h>

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetRevoked(JNIEnv * env,
						       jobject self, jlong sig)
{
    return (jboolean) (KEYSIG(sig))->revoked;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetExpired(JNIEnv * env,
						       jobject self, jlong sig)
{
    return (jboolean) (KEYSIG(sig))->expired;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetInvalid(JNIEnv * env,
						       jobject self, jlong sig)
{
    return (jboolean) (KEYSIG(sig))->invalid;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetExportable(JNIEnv * env,
							  jobject self,
							  jlong sig)
{
    return (jboolean) (KEYSIG(sig))->exportable;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetKeyID(JNIEnv * env, jobject self,
						     jlong sig)
{
    jstring str = (*env)->NewStringUTF(env, (KEYSIG(sig))->keyid);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetUserID(JNIEnv * env,
						      jobject self, jlong sig)
{
    jstring str = (*env)->NewStringUTF(env, (KEYSIG(sig))->uid);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetName(JNIEnv * env, jobject self,
						    jlong sig)
{
    jstring str = (*env)->NewStringUTF(env, (KEYSIG(sig))->name);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetEmail(JNIEnv * env, jobject self,
						     jlong sig)
{
    jstring str = (*env)->NewStringUTF(env, (KEYSIG(sig))->email);
    return str;
}

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetComment(JNIEnv * env,
						       jobject self, jlong sig)
{
    jstring str = (*env)->NewStringUTF(env, (KEYSIG(sig))->comment);
    return str;
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGSignature_gpgmeGetNextSignature(JNIEnv * env,
							     jobject self,
							     jlong sig)
{
    return LNG((KEYSIG(sig))->next);
}
