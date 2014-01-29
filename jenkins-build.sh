#!/bin/bash
#
# we need bash for the better file globbing syntax (i.e. the ^ NOT)

set -e
set -x

. ~/.android/bashrc
make -C external/ distclean clean-assets gitclean
make -C external/
make -C external/ assets-tests
ndk-build
./setup-ant.sh
# run on all GnuPG projects (skip curl and openldap)
cppcheck $WORKSPACE/jni $WORKSPACE/external/[^co]* --enable=all --xml 2> cppcheck-result.xml
