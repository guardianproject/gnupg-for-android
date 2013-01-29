
#include <stdlib.h>
#include <jni.h>

#include "pinentry.h"

static pinentry_t pinentry;
static int passphrase_ok;
typedef enum { CONFIRM_CANCEL, CONFIRM_OK, CONFIRM_NOTOK } confirm_value_t;
static confirm_value_t confirm_value;

static int
android_cmd_handler (pinentry_t pe)
{
  int want_pass = !!pe->pin;

  pinentry = pe;
  confirm_value = CONFIRM_CANCEL;
  passphrase_ok = 0;

  if (confirm_value == CONFIRM_CANCEL)
    pe->canceled = 1;

  pinentry = NULL;
  if (want_pass)
    {
        LOGD("android_cmd_handler: i think they want a pin..\n");

        const char *pin = "1234";
        int len = strlen (pin);
        pinentry_setbufferlen (pe, len + 1);
        strcpy (pe->pin, pin);
        return len;
    }
  else
    return (confirm_value == CONFIRM_OK) ? 1 : 0;
}

pinentry_cmd_handler_t pinentry_cmd_handler = android_cmd_handler;

JNIEXPORT void JNICALL
startPinentryLoop(JNIEnv * env, jobject self)
{
    pinentry_init("pinentry-android");

    char* debug[1];
    debug[0] = "--debug";
    /* Consumes all arguments.  */
    if (pinentry_parse_opts(1, debug))
        exit(EXIT_SUCCESS);
    
    pinentry_loop(); // this only exits when done
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    // TODO set locale from the JavaVM's config
    //setlocale(LC_ALL, "");

    return JNI_VERSION_1_6;
}
