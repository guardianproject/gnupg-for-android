#!/bin/sh

set -e

if [ -e ~/.android/bashrc ]; then
    . ~/.android/bashrc
else
    echo "No ~/.android/bashrc found, 'android' and 'ndk-build' must be in PATH"
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
ndk-build

if [ -e ~/.android/ant.properties ]; then
    cp ~/.android/ant.properties $projectroot/
else
    echo "skipping release ant.properties"
fi

./setup-ant.sh
ant release

apk=$projectroot/bin/$projectname-release.apk
if [ -e $apk ]; then
    gpg --detach-sign $apk
else
    echo $apk does not exist!
fi
