#!/bin/sh

set -e
set -x

echoheader() {
    echo ""
    echo ========================================================================
}

run_test_script() {
    logfile=`mktemp`
    adb shell $1 > $logfile
    cat $logfile
    if [ "`tail -1 $logfile | cut -b1-7`" != SUCCESS ]; then
        echo $1 FAILED!
        false
    fi
}

echoheader
echo "looking for adb in ANDROID_HOME: $ANDROID_HOME"

if [ -z $ANDROID_HOME ]; then
    . ~/.android/bashrc
else
    export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools"
fi

echoheader
echo "Waiting for device to be attached"
adb wait-for-device
adb devices

echoheader
echo "Checking which user 'adb shell' defaults to:"
adb shell whoami
adb shell 'echo $USER'
adb shell 'echo $HOME'


echoheader
echo "What files got installed?"
adb shell 'ls -l /data/data/info.guardianproject.gpg'
adb shell 'ls -l /data/data/info.guardianproject.gpg/lib'
adb shell 'ls -l /data/data/info.guardianproject.gpg/app_opt'
adb shell 'ls -l /data/data/info.guardianproject.gpg/app_opt/aliases'
adb shell 'ls -l /data/data/info.guardianproject.gpg/app_opt/bin'
adb shell 'ls -l /data/data/info.guardianproject.gpg/app_opt/lib'
adb shell 'ls -l /data/data/info.guardianproject.gpg/app_opt/tests'
adb shell 'ls -l -R /data/data/info.guardianproject.gpg'


echoheader
echo "Printing the environment variables: "
adb shell set
adb shell export


echoheader
echo "Running run-tests.sh on Android via ADB:"
run_test_script /data/data/info.guardianproject.gpg/app_opt/tests/run-tests.sh


echoheader
echo "Print out debug logs:"

adb shell 'ls -l /data/data/info.guardianproject.gpg/app_log/'
adb shell 'cat /data/data/info.guardianproject.gpg/app_log/*'
