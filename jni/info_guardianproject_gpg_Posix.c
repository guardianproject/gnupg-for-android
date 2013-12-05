#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include <jni.h>

jint Java_info_guardianproject_gpg_Posix_umask(JNIEnv* env, jobject clazz, jint mask)
{
    return umask(mask);
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

static const JNINativeMethod methods[] = {
    {"umask", "(I)I", (void*)Java_info_guardianproject_gpg_Posix_umask},
};
