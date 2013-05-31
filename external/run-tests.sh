#!/system/bin/sh
#
# this script runs the tests on Android

# stop on any error
set -e

app=/data/data/info.guardianproject.gpg
app_opt=$app/app_opt
export HOME=$app/app_home
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$app_opt/lib
export USER=`findusername`

findusername_helper() {
    echo $2
}

findusername() {
    echo `findusername_helper $(ls -ld $HOME)`
}

runtest() {
    echo "Running $1/$2 $3 $4 $5 $6 $7 $8 $9"
    cd $app_opt/tests/$1
    ./$2 $3 $4 $5 $6 $7 $8 $9 && echo DONE.
}

echo "------------------------------"
echo "environment:"
export

echo "------------------------------"
echo "gpgme tests:"
cd $app_opt/tests/
runtest gpgme t-version
runtest gpgme t-engine-info
runtest gpgme t-data
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose pubdemo.asc
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose seckey-1.asc
runtest gpgme run-import --verbose secdemo.asc
runtest gpgme run-keylist --verbose

