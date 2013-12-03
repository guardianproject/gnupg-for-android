#include <jni.h>
#include <stdio.h>


#include "gpgmeutils.h"

#include <gpgme.h>
#include <data.h>

#define BUFSIZE 1024


JNIEXPORT jsize JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeSize(JNIEnv* env, jobject self, jlong data)
{
    return (jsize) (DATA(data))->data.mem.size;
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataNewFromMem(JNIEnv* env,
        jobject self,
        jbyteArray plain)
{
    gpgme_error_t err;

    jbyte* plain_ptr = (*env)->GetByteArrayElements(env, plain, NULL);  //GETMEM(0)
    if (plain_ptr == NULL) {
        fprintf(stderr, "could not allocate memory.\n");
        return 0;
    }

    gpgme_data_t data;
    jlong len = (*env)->GetArrayLength(env, plain);

    //make private copy of data
    err = gpgme_data_new_from_mem(&data, (const char*) plain_ptr,
                                  (size_t) len, 1);
    if (UTILS_onErrorThrowException(env, err)) {
        return LNG(NULL);
    }

    (*env)->ReleaseByteArrayElements(env, plain, plain_ptr, 0); //RELMEM(0)
    jlong result = LNG(data);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataNewFromFilename(JNIEnv* env,
        jobject self,
        jstring filename,
        jstring mode)
{
    const char* mode_str = (*env)->GetStringUTFChars(env, mode, NULL);
    const char* filename_str = (*env)->GetStringUTFChars(env, filename, NULL);
    if (filename_str == NULL) {
        fprintf(stderr, "could not allocate memory.\n");
        return 0;
    }

    gpgme_data_t data;
    FILE* stream = fopen(filename_str, mode_str);
    gpgme_error_t err = gpgme_data_new_from_stream(&data, stream);
    if (UTILS_onErrorThrowException(env, err)) {
        return LNG(NULL);
    }

    (*env)->ReleaseStringUTFChars(env, filename, filename_str);
    (*env)->ReleaseStringUTFChars(env, mode, mode_str);

    jlong result = LNG(data);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataNew(JNIEnv* env, jobject self)
{
    gpgme_data_t data;
    gpgme_error_t err = gpgme_data_new(&data);
    if (UTILS_onErrorThrowException(env, err)) {
        return LNG(NULL);
    }

    return LNG(data);
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataWrite(JNIEnv* env, jobject self,
        jlong data, jobject out)
{
    gpgme_error_t err;

    jbyte buf[BUFSIZE];
    size_t nread;

    jclass outputStream = (*env)->GetObjectClass(env, out);
    if (outputStream == NULL) {
        fprintf(stderr, "output stream NULL! abort.\n");
        return;
    }

    jmethodID writeMethod =
        (*env)->GetMethodID(env, outputStream, "write", "([BII)V");
    if (writeMethod == NULL) {
        fprintf(stderr, "write method NULL! abort.\n");
        return;
    }

    jbyteArray jbuf;

    err = (gpgme_data_seek ( DATA(data), (off_t)0, SEEK_SET ) < 0);
    if (UTILS_onErrorThrowException(env, err)) {
        fprintf(stderr, "error throw exception! abort.\n");
        return;
    }

    int size = 0;
    while ((nread = gpgme_data_read(DATA(data), buf, BUFSIZE)) != 0) {
        size += nread;
        jbuf = (*env)->NewByteArray(env, nread);
        if (jbuf == NULL) {
            fprintf(stderr, "jbuf is NULL! abort.\n");
            return;
        }
        (*env)->SetByteArrayRegion(env, jbuf, 0, nread, buf);
        (*env)->CallVoidMethod(env, out, writeMethod, jbuf, (jint)0,
                               (jint)nread);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, jbuf);
            return;
        }
        (*env)->DeleteLocalRef(env, jbuf);
    }

}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataRelease(JNIEnv* env, jobject self,
        jlong data)
{
    gpgme_data_t dh = DATA(data);
    if (dh->data.stream != NULL)
        fclose(dh->data.stream);
    gpgme_data_release(dh);
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGData_gpgmeDataRead(JNIEnv* env, jobject self,
        jlong data, jobject in)
{
    gpgme_error_t err;

    jclass inputStream = (*env)->GetObjectClass(env, in);
    if (inputStream == NULL) {
        fprintf(stderr, "input stream NULL! abort.\n");
        return;
    }

    jmethodID readMethod = (*env)->GetMethodID(env, inputStream,
                           "read", "([BII)I");
    if (readMethod == NULL) {
        fprintf(stderr, "read method NULL! abort.\n");
        return;
    }

    jbyteArray jbuf = (*env)->NewByteArray(env, BUFSIZE);   //GETMEM(0)
    jlong nread;

    err = (gpgme_data_seek (DATA(data), (off_t)0, SEEK_SET) < 0);
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    ssize_t written;
    while ((nread = (*env)->CallIntMethod(env, in, readMethod,
                                          jbuf, (jint)0, BUFSIZE)) != -1) {

        jbyte* buf = (*env)->GetByteArrayElements(env, jbuf, NULL);
        if (buf == NULL) {
            return;
        }

        written = gpgme_data_write(DATA(data), buf, nread);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, jbuf);
            return;
        }

    }
    (*env)->DeleteLocalRef(env, jbuf);  //RELMEM(0)

}
