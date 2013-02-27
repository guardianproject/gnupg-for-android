
#include <stdlib.h>
#include <jni.h>

#include <errno.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <sys/un.h>
#include <locale.h>

#include <android/log.h>

#include "pinentry.h"

#define SOCKET_NAME "info.guardianproject.gpg.pinentry"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , "PINENTRY", __VA_ARGS__)
#define MSG_CMSG_CLOEXEC NULL

#define TO_JAVA_STRING(NAME, EXP) \
        jstring NAME = (*env)->NewStringUTF(env,EXP); \
        if (NAME == 0) { LOGD("failed to create str NAME with EXP\n"); return; }

#define SET_STR(NAME) \
     TO_JAVA_STRING(NAME, pe->NAME); \
     fid = (*env)->GetFieldID(env,cls,#NAME,"Ljava/lang/String;"); \
     (*env)->SetObjectField(env,obj,fid,NAME);

JavaVM* _jvm = 0;
jobject _pinentryActivity = 0;
 int recv_fd(int socket)
 {
  int sent_fd, available_ancillary_element_buffer_space;
  struct msghdr socket_message;
  struct iovec io_vector[1];
  struct cmsghdr *control_message = NULL;
  char message_buffer[1];
  char ancillary_element_buffer[CMSG_SPACE(sizeof(int))];

  /* start clean */
  memset(&socket_message, 0, sizeof(struct msghdr));
  memset(ancillary_element_buffer, 0, CMSG_SPACE(sizeof(int)));

  /* setup a place to fill in message contents */
  io_vector[0].iov_base = message_buffer;
  io_vector[0].iov_len = 1;
  socket_message.msg_iov = io_vector;
  socket_message.msg_iovlen = 1;

  /* provide space for the ancillary data */
  socket_message.msg_control = ancillary_element_buffer;
  socket_message.msg_controllen = CMSG_SPACE(sizeof(int));

  // TODO: the NULL below used ot be msg_cmsg_cloexec
  // but it broke compilation on android, make sure this isn't killing puppies
  if(recvmsg(socket, &socket_message, MSG_CMSG_CLOEXEC) < 0)
   return -1;

  if(message_buffer[0] != 'F')
  {
   /* this did not originate from the above function */
   return -1;
  }

  if((socket_message.msg_flags & MSG_CTRUNC) == MSG_CTRUNC)
  {
   /* we did not provide enough space for the ancillary element array */
   return -1;
  }

  /* iterate ancillary elements */
   for(control_message = CMSG_FIRSTHDR(&socket_message);
       control_message != NULL;
       control_message = CMSG_NXTHDR(&socket_message, control_message))
  {
   if( (control_message->cmsg_level == SOL_SOCKET) &&
       (control_message->cmsg_type == SCM_RIGHTS) )
   {
    sent_fd = *((int *) CMSG_DATA(control_message));
    return sent_fd;
   }
  }

  return -1;
 }
 
static pinentry_t pinentry;
static int passphrase_ok;
typedef enum { CONFIRM_CANCEL, CONFIRM_OK, CONFIRM_NOTOK } confirm_value_t;
static confirm_value_t confirm_value;

/*
 * takes a pinentry_t from the command handler
 * 1. instantiates the corresponding java PinentryStruct object
 * 2. passes this new object to PinEntryActivity.setPinentryStruct()
 * 3. accepts return of a NEW PinentryStruct from the method
 * 4. parses out the user supplied pin (if there is one!)
 * 5. returns
 * TODO: would be nice to simplify this function, but each of those
 * taskes requires lots of shared state, should create a state obj to pass around.
 */
int contact_javaland(pinentry_t pe) {
    JNIEnv* env;
    jfieldID fid;
    jclass cls = 0;
    jobject result = 0, obj = 0;

    LOGD("fill_struct %s\n", pe->title);

    if(_jvm == 0) {
        LOGD("fill_struct: JVM is null\n");
        return -1;
    }
    if(_pinentryActivity == 0) {
        LOGD("fill_struct: _pinentryActivity is null\n");
        return -1;
    }

    // attach to JVM and instantiate a PinentryStruct object
    (*_jvm)->AttachCurrentThread(_jvm, &env, 0);
    LOGD("attached thread\n");
     cls = (*env)->FindClass(env, "info/guardianproject/gpg/pinentry/PinentryStruct");
     if( !cls ) { 
         LOGD("failed to retrieve PinentryStruct\n");
         return -1;
     }
     LOGD("got cls\n");

     jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");
     LOGD("got ctor\n");
     obj = (*env)->NewObject(env, cls, constructor);
     if (!obj)  {
         LOGD("NewObject failed\n");
         return -1;
    }
    LOGD("got obj\n");

    // assign all values
     LOGD("title %s\n", pe->title);
     if(pe->title) {
        SET_STR(title);
     }
     LOGD("description %s\n", pe->description);
     if( pe->description ) {
        SET_STR(description);
     }
     LOGD("prompt %s\n", pe->prompt);
     //SET_STR(error);

     jclass cls2 = (*env)->FindClass(env, "info/guardianproject/gpg/pinentry/PinEntryActivity");
     if( !cls2 ) { LOGD("failed to retrieve PinentryActivity\n"); return -1; }
     jmethodID myUsefulJavaFunction = (*env)->GetMethodID(env, cls2, "setPinentryStruct", "(Linfo/guardianproject/gpg/pinentry/PinentryStruct;)Linfo/guardianproject/gpg/pinentry/PinentryStruct;");
     if( !myUsefulJavaFunction ) { LOGD("failed to retrieve myUsefulJavaFunction\n"); return -1; }
     result = (*env)->CallObjectMethod(env, _pinentryActivity, myUsefulJavaFunction, obj);
     if( !result ) { LOGD("result is null!!\n"); return -1; }
     fid = (*env)->GetFieldID(env,cls,"pin","Ljava/lang/String;");
     if( !fid ) { LOGD("failed to get pin field id\n"); return -1; }
     jstring jpin = (*env)->GetObjectField(env,result,fid);
     if( !jpin ) { LOGD("jpin is null!!\n"); return -1; }
     jbyte *pin = (*env)->GetStringUTFChars(env, jpin, NULL);

    int len = strlen (pin);
    if (len >= 0) {
        pinentry_setbufferlen (pe, len + 1);
        if (pe->pin)
        {
            strcpy (pe->pin, pin);
            LOGD("set pin to %s\n", pe->pin);
            return len;
        }
    }
    LOGD("something went wrong returning -1\n");
     return -1;
}

static int
android_cmd_handler (pinentry_t pe)
{
  LOGD("android_cmd_handler\n");
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

        return contact_javaland(pe);
    }
  else
    return (confirm_value == CONFIRM_OK) ? 1 : 0;
}

pinentry_cmd_handler_t pinentry_cmd_handler = android_cmd_handler;


int  open_socket() {
      /* 2. connect to activity's socket */
  struct sockaddr_un addr;
  int fd;

  if ( (fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
    perror("socket error");
    exit(-1);
  }

  memset(&addr, 0, sizeof(addr));
  addr.sun_family = AF_UNIX;
  addr.sun_path[0] = '\0';
  strncpy( &addr.sun_path[1], SOCKET_NAME, sizeof(addr.sun_path)-1 );
  /* calculate the length of our addrlen, for some reason this isn't simply
   * sizeof(addr), TODO: learn why, i suspect it has something to do with sun_path
   * being a char[108]
   */
  int len = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(&addr.sun_path[1]);

    if (connect(fd, (struct sockaddr*)&addr, len) < 0) {
    perror("connect error");
    exit(-1);
  }
  return fd;
}
JNIEXPORT void JNICALL
Java_info_guardianproject_gpg_pinentry_PinEntryActivity_connectToGpgAgent(JNIEnv * env, jobject self)
{
    _pinentryActivity = self;
	LOGD("connectToGpgAgent called!\n");
    int in, out, sock;
    sock = open_socket();
    LOGD("connected to pinentry helper\n");

    in = recv_fd(sock);
    if( in == -1 ) {
        LOGD("STDIN receiving failed!\n");
    }
    out = recv_fd(sock);
    if( out == -1 ) {
        LOGD("STDOUT receiving failed!\n");
    }

    pinentry_init("pinentry-android");

    char* debug[1];
    debug[0] = "--debug";
    /* Consumes all arguments.  */
    if (pinentry_parse_opts(1, debug))
        write(sock, EXIT_SUCCESS, 1);
    LOGD("good to go, starting pinentry loop\n");
    pinentry_loop2(in, out); // this only exits when done
    LOGD("pinentry_loop2 returned\n");
    // close pinentry helper
    int buf[1] = { EXIT_SUCCESS };
    int r = write(sock, buf, 1);
    LOGD("wrote %d to helper\n", r);
    if( r < 0 ) 
        perror("closing pinentry helper failed:");
}

static JNINativeMethod sMethods[] = {
		{"connectToGpgAgent", "()V", (void *)Java_info_guardianproject_gpg_pinentry_PinEntryActivity_connectToGpgAgent}
};

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
	LOGD("JNI_OnLoad called\n");
#ifdef __ANDROID__
    // TODO get the actual gpgAppOpt path from Java
    // we need to set LD_LIBRARY_PATH because gpgme calls the cmd line utils
    const char *ldLibraryPath = getenv("LD_LIBRARY_PATH");
    const char *gpgAppOpt = "/data/data/info.guardianproject.gpg/app_opt/lib";
    size_t newPathLen = strlen(ldLibraryPath) + strlen(gpgAppOpt) + 2;
    char newPath[newPathLen];
    snprintf(newPath, newPathLen, "%s:%s", ldLibraryPath, gpgAppOpt);
    setenv("LD_LIBRARY_PATH", newPath, 1);
#endif /* __ANDROID */
    // TODO set locale from the JavaVM's config
//    setlocale(LC_ALL, "");

    _jvm = vm;

    return JNI_VERSION_1_6;
}
