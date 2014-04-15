#!/bin/bash
#
# we need bash for the better file globbing syntax (i.e. the ^ NOT)

set -e
set -x

# reset version code/name to current date
versionCodeDate=`date +%s`
versionNameDate=`date +%Y-%m-%d_%H.%M.%S`

sed -i \
    -e "s,android:versionCode=\"[0-9][0-9]*\",android:versionCode=\"$versionCodeDate\"," \
    -e "s,android:versionName=\"\([^\"][^\"]*\)\",android:versionName=\"\1.$versionNameDate\"," \
    AndroidManifest.xml


. ~/.android/bashrc
make -C external/ distclean clean-assets gitclean
make -C external/
make -C external/ assets-tests
ndk-build
./setup-ant.sh
# run on all GnuPG projects (skip curl and openldap)
cppcheck $WORKSPACE/jni $WORKSPACE/external/[^co]* --max-configs=50 --enable=all --xml 2> cppcheck-result.xml
