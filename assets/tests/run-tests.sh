#!/system/bin/sh
#
# this script runs tests that don't require a password on Android

# stop on any error
set -e

. /data/data/info.guardianproject.gpg/app_opt/tests/common


echo "------------------------------"
echo "gpgme tests:"
export USER=`findusername`
cd $app_opt/tests/
runtest gpgme t-version
runtest gpgme t-engine-info
runtest gpgme t-data
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose pubdemo.asc
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-keylist --verbose

echo SUCCESS
