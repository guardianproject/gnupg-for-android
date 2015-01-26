#!/bin/sh

set -e
set -x

# make sure we're on a signed tag that matches the version name
versionName=`sed -n 's,.*versionName="\([^"]*\)".*,\1,p' AndroidManifest.xml`
describe=`git describe`
if [ $versionName != $describe ]; then
    echo "WARNING: checking out release tag $versionName"
   git checkout $versionName
fi
git tag -v $versionName

if [ -z $ANDROID_HOME ]; then
    if [ -e ~/.android/bashrc ]; then
        . ~/.android/bashrc
    else
        echo "ANDROID_HOME must be set!"
        exit
    fi
fi

if [ -z $ANDROID_NDK ]; then
    if which ndk-build 2>&1 /dev/null; then
        ANDROID_NDK=`which ndk-build |  sed 's,/ndk-build,,'`
    else
        echo "ANDROID_NDK not set and 'ndk-build' not in PATH"
        exit
    fi
fi

projectroot=`pwd`
projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

for f in $projectroot/external/*/.git; do
    dir=`echo $f | sed 's,\.git$,,'`
    cd $dir
    git reset --hard
    git clean -fdx
    cd $projectroot
done

cd $projectroot
git reset --hard
git clean -fdx
git submodule foreach --recursive git reset --hard
git submodule foreach --recursive git clean -fdx
git submodule sync --recursive
git submodule foreach --recursive git submodule sync
git submodule update --init --recursive

make -C external/
$ANDROID_NDK/ndk-build

if [ -e ~/.android/ant.properties ]; then
    cp ~/.android/ant.properties $projectroot/
else
    echo "skipping release ant.properties"
fi

./setup-ant.sh
ant release

apk=$projectroot/bin/$projectname*-release.apk
if [ -e $apk ]; then
    gpg --detach-sign $apk
else
    echo $apk does not exist!
fi
