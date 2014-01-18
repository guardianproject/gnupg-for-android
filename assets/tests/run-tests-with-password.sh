#!/system/bin/sh
#
# this script runs tests that don't require a password on Android

# stop on any error
set -e

. /data/data/info.guardianproject.gpg/app_opt/tests/common


echo "------------------------------"
echo "gpgme tests:"
cd $app_opt/tests/
./run-tests.sh
runtest gpgme run-import --verbose seckey-1.asc
runtest gpgme run-import --verbose secdemo.asc
runtest gpgme run-keylist --verbose

echo SUCCESS
