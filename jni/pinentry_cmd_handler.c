
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
        if (NAME == NULL) return;

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
//TO_JAVA_STRING(NAME, pe->NAME);
#define SET_STR(NAME, OBJ) \
     TO_JAVA_STRING(NAME, "NAME"); \
     fid = (*env)->GetFieldID(env,OBJ,"NAME","Ljava/lang/String;"); \
     (*env)->SetObjectField(env,OBJ,fid,NAME); \

void fill_struct(pinentry_t pe) {
    if(_jvm == 0) {
        LOGD("WTF! JVM is null\n");
        return;
    }
    if(_pinentryActivity == 0) {
        LOGD("WTF! _pinentryActivity is null\n");
        return;
    }
    JNIEnv* env;
    jfieldID fid;
    (*_jvm)->AttachCurrentThread(_jvm, &env, 0);
     jclass cls = (*env)->FindClass(env, "info/guardianproject/gpg/pinentry/PinentryStruct");
     if( !cls ) { LOGD("failed to retrieve PinentryStruct\n"); return; }
     LOGD("got class.. loading ctor\n");
     jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");
     LOGD("got ctor making obj\n");
     jobject obj = (*env)->NewObject(env, cls, constructor);
     if (!obj)  { LOGD("NewObject failed\n"); return; }
     LOGD("got obj setting title.\n");

     jstring title = (*env)->NewStringUTF(env,"ZOMG");
    if (title == 0) return;
        LOGD("got str.\n");
     fid = (*env)->GetFieldID(env,cls,"title","Ljava/lang/String;");
    LOGD("got fid.\n");
     (*env)->SetObjectField(env,obj,fid,title);
     LOGD("set fid.\n");
     //SET_STR(title, obj);
     jclass cls2 = (*env)->FindClass(env, "info/guardianproject/gpg/PinEntryActivity");
     if( !cls2 ) { LOGD("failed to retrieve PinentryActivity\n"); return; }
      jmethodID myUsefulJavaFunction = (*env)->GetStaticMethodID(env, cls2, "setPinentryStruct", "(Linfo/guardianproject/gpg/pinentry/PinentryStruct;)V");
      if( !myUsefulJavaFunction ) { LOGD("failed to retrieve myUsefulJavaFunction\n"); return; }
      (*env)->CallStaticVoidMethod(env, cls2, myUsefulJavaFunction, obj);
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
Java_info_guardianproject_gpg_PinEntryActivity_connectToGpgAgent(JNIEnv * env, jobject self)
{
    _pinentryActivity = self;
    fill_struct(0);
    LOGD("sent struct quitting!\n");
    return;
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
    
    LOGD("good to go, starting pinentry loop\n");

    pinentry_init("pinentry-android");

    char* debug[1];
    debug[0] = "--debug";
    /* Consumes all arguments.  */
    if (pinentry_parse_opts(1, debug))
        exit(EXIT_SUCCESS);

    pinentry_loop2(in, out); // this only exits when done
}

static JNINativeMethod sMethods[] = {
		{"connectToGpgAgent", "()V", (void *)Java_info_guardianproject_gpg_PinEntryActivity_connectToGpgAgent}
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
