#!/system/bin/sh
#
# this script runs the tests on Android

# stop on any error
set -e

findusername_helper() {
    echo $2
}

findusername() {
    echo `findusername_helper $(ls -ld /data/data/info.guardianproject.gpg/app_home)`
}

runtest() {
    echo "Running $1/$2 $3 $4 $5 $6 $7 $8 $9"
    cd /data/data/info.guardianproject.gpg/app_opt/tests/$1
    ./$2 $3 $4 $5 $6 $7 $8 $9 && echo DONE.
}

export HOME=/data/data/info.guardianproject.gpg/app_home
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/data/data/info.guardianproject.gpg/app_opt/lib
export USER=`findusername`

echo "------------------------------"
echo "environment:"
export

echo "------------------------------"
echo "gpgme tests:"
cd /data/data/info.guardianproject.gpg/app_opt/tests/
runtest gpgme t-version
runtest gpgme t-engine-info
runtest gpgme t-data
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose pubdemo.asc
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose seckey-1.asc
runtest gpgme run-import --verbose secdemo.asc
runtest gpgme run-keylist --verbose

