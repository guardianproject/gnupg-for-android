#include <sys/stat.h>
#include <sys/types.h>

#include <jni.h>

jint Java_info_guardianproject_gpg_Posix_umask(JNIEnv* env, jobject clazz, jint mask)
{
    return umask(mask);
}

static const JNINativeMethod methods[] = {
    {"umask", "(I)I", (void*)Java_info_guardianproject_gpg_Posix_umask},
};