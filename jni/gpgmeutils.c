#include "gpgmeutils.h"
#include <stdio.h>

#define BUF_LEN 1024

jboolean
UTILS_setStringMember(JNIEnv* env, jobject self, jclass selfclass,
                      const char* fieldname, const char* fieldvalue)
{
    //get the field from the class
    jfieldID fld =
        (*env)->GetFieldID(env, selfclass, fieldname, "Ljava/lang/String;");
    if (fld == NULL) {
        return JNI_FALSE;
    }

    jstring jval = (*env)->NewStringUTF(env, fieldvalue);
    if (jval == NULL) {
        return JNI_FALSE;
    }
    (*env)->SetObjectField(env, self, fld, jval);
    return JNI_TRUE;
}

jboolean
UTILS_setIntMember(JNIEnv* env, jobject self, jclass selfclass,
                   const char* fieldname, int fieldvalue)
{
    //get the field from the class

    jlong jval = (jlong) fieldvalue;

    jfieldID fld = (*env)->GetFieldID(env, selfclass, fieldname, "I");
    if (fld == NULL) {
        return JNI_FALSE;
    }

    if (jval == 0) {
        return JNI_FALSE;
    }
    (*env)->SetIntField(env, self, fld, jval);
    return JNI_TRUE;
}

void
UTILS_setBooleanMember(JNIEnv* env, jobject self, jclass selfclass,
                       const char* fieldname, unsigned int fieldvalue)
{
    //get the field from the class

    jboolean jval = (jboolean) fieldvalue;

    jfieldID fld = (*env)->GetFieldID(env, selfclass, fieldname, "Z");
    if (fld == NULL) {
        return;
    }

    if (jval == 0) {
        return;
    }
    (*env)->SetBooleanField(env, self, fld, jval);
}

jboolean UTILS_onErrorThrowException(JNIEnv* env, gpgme_error_t err)
{
    if (err) {
        char exceptionString[BUF_LEN];  /* this is enough */
        int spaceUsed;
        jclass exception =
            (*env)->FindClass(env, "com/freiheit/gnupg/GnuPGException");

        if (exception == NULL) {
            return JNI_TRUE;
        }
        spaceUsed = snprintf(exceptionString, BUF_LEN, "%u: ", err);
        gpgme_strerror_r(err, exceptionString + spaceUsed, BUF_LEN - spaceUsed);
        (*env)->ThrowNew(env, exception, exceptionString);
        (*env)->DeleteLocalRef(env, exception);
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

int
UTILS_copyRecipientsFromJvm(JNIEnv* env, jlongArray recipients,
                            gpgme_key_t keys[])
{
    //how many keys from recipients did we receive?
    jsize len = (*env)->GetArrayLength(env, recipients);
    if (len < 1) {
        return len;
    }

    //allocate native memory from recipient keys
    jlong* carr = (*env)->GetLongArrayElements(env, recipients, NULL);
    if (carr == NULL) {
        return -1;
    }
    //copy recipient keys to new array...
    int i;
    for (i = 0; i < len; i++) {
        keys[i] = KEY(carr[i]);
    }
    //and mark the end of the array with a NULL.
    keys[len] = NULL;

    //release the memory allocated from the recipients key list
    (*env)->ReleaseLongArrayElements(env, recipients, carr, 0);

    return len;
}
