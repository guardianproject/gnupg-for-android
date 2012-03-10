#include <jni.h>
#include <stdio.h>
#include "com_freiheit_gnupg_GnuPGGenkeyResult.h"
#include "gpgmeutils.h"

#include <gpgme.h>

JNIEXPORT jstring JNICALL
Java_com_freiheit_gnupg_GnuPGGenkeyResult_gpgmeGetFpr(JNIEnv * env,
						      jobject self,
						      jlong result)
{
    jstring str = (*env)->NewStringUTF(env, (GENKEYRESULT(result))->fpr);
    return str;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGGenkeyResult_gpgmeGetPrimary(JNIEnv * env,
							  jobject self,
							  jlong result)
{
    return (GENKEYRESULT(result))->primary;
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGGenkeyResult_gpgmeGetSub(JNIEnv * env,
						      jobject self,
						      jlong result)
{
    return (GENKEYRESULT(result))->primary;
}
