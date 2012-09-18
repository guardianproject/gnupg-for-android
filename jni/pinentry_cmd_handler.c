
#include <stdlib.h>
#include <jni.h>

#include "pinentry.h"

static pinentry_t pinentry;
static int grab_failed;
static int passphrase_ok;
typedef enum { CONFIRM_CANCEL, CONFIRM_OK, CONFIRM_NOTOK } confirm_value_t;
static confirm_value_t confirm_value;

static int
android_cmd_handler (pinentry_t pe)
{
//  GtkWidget *w;
  int want_pass = !!pe->pin;

  pinentry = pe;
  confirm_value = CONFIRM_CANCEL;
  passphrase_ok = 0;
//  w = create_window (want_pass ? 0 : 1);
//  gtk_main ();
//  gtk_widget_destroy (w);
//  while (gtk_events_pending ())
//    gtk_main_iteration ();

  if (confirm_value == CONFIRM_CANCEL || grab_failed)
    pe->canceled = 1;

  pinentry = NULL;
  if (want_pass)
    {
      if (passphrase_ok && pe->pin)
	return strlen (pe->pin);
      else
	return -1;
    }
  else
    return (confirm_value == CONFIRM_OK) ? 1 : 0;
}

pinentry_cmd_handler_t pinentry_cmd_handler = android_cmd_handler;


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    // TODO set locale from the JavaVM's config
    //setlocale(LC_ALL, "");
    
    pinentry_init("pinentry-android");

    /* Consumes all arguments.  */
    if (pinentry_parse_opts(0, NULL))
        exit(EXIT_SUCCESS);
    
    if (pinentry_loop())
        return 1;
    
    return JNI_VERSION_1_6;
}
