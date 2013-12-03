#ifndef GPGME_JAVA_UTILS
#define GPGME_JAVA_UTILS

#include <jni.h>
#include <gpg-error.h>
#include <gpgme.h>

jboolean UTILS_setStringMember(JNIEnv* env, jobject self, jclass selfclass,
                               const char* fieldname, const char* fieldvalue);

jboolean UTILS_setIntMember(JNIEnv* env, jobject self, jclass selfclass,
                            const char* fieldname, int fieldvalue);

void UTILS_setBooleanMember(JNIEnv* env, jobject self, jclass selfclass,
                            const char* fieldname, unsigned int fieldvalue);

jboolean UTILS_onErrorThrowException(JNIEnv* env, gpgme_error_t err);

int UTILS_copyRecipientsFromJvm(JNIEnv* env, jlongArray recipients, gpgme_key_t keys[]);

/* use this macro to convert a jlong variable to a pointer to a gpgme context in a safe and portable way */
#define CONTEXT(c) ((gpgme_ctx_t)_ptrFromJLong(c))
/* use this macro to convert a jlong variable to a pointer to a gpgme data buffer in a safe and portable way */
#define DATA(c) ((gpgme_data_t)_ptrFromJLong(c))
/* use this macro to convert a jlong variable to a pointer to a gpgme genkey result in a safe and portable way */
#define GENKEYRESULT(c) ((gpgme_genkey_result_t)_ptrFromJLong(c))
/* use this macro to convert a jlong variable to a pointer to a gpgme key signature in a safe and portable way */
#define KEYSIG(c) ((gpgme_key_sig_t)_ptrFromJLong(c))
/* use this macro to convert a jlong variable to a pointer to a gpgme key in a safe and portable way */
#define KEY(c) ((gpgme_key_t)_ptrFromJLong(c))

/* use this macro to convert a pointer variable back to a jlong in a safe and portable way */
#define LNG(a) (_jlongFromPtr(a))

// don't use this directly, it is wrapped by the PTR() macro
inline static void* _ptrFromJLong(jlong l)
{
    return (void*) (unsigned long) l;
}

// don't use this directly, it is wrapped by the PTR() macro
inline static jlong _jlongFromPtr(void* p)
{
    return (jlong) (unsigned long) p;
}
#endif
