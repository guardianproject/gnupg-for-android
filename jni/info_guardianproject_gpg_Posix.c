#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include <jni.h>

jint Java_info_guardianproject_gpg_Posix_umask(JNIEnv* env, jobject clazz, jint mask)
{
    return umask(mask);
}

JNIEXPORT jint JNICALL Java_info_guardianproject_gpg_Posix_setenv
  (JNIEnv* env, jclass clazz, jstring key, jstring value, jboolean overwrite)
{
    char* k = (char *) (*env)->GetStringUTFChars(env, key, NULL);
    char* v = (char *) (*env)->GetStringUTFChars(env, value, NULL);
    int err = setenv(k, v, overwrite);
    (*env)->ReleaseStringUTFChars(env, key, k);
    (*env)->ReleaseStringUTFChars(env, value, v);
    return err;
}

jint Java_info_guardianproject_gpg_Posix_symlink(JNIEnv* env, jobject clazz,
                                                 jstring oldpath, jstring newpath)
{
    char* old = (char *) (*env)->GetStringUTFChars(env, oldpath, NULL);
    char* new = (char *) (*env)->GetStringUTFChars(env, newpath, NULL);
    int err = symlink(old, new);
    (*env)->ReleaseStringUTFChars(env, oldpath, old);
    (*env)->ReleaseStringUTFChars(env, newpath, new);
    return err;
}
