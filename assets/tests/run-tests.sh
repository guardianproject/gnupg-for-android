#!/system/bin/sh
#
# this script runs tests that don't require a password on Android

# stop on any error
set -e

. /data/data/info.guardianproject.gpg/app_opt/tests/common

export USER=`findusername`
cd $app_opt/tests/

echo "------------------------------"
echo "libgpg-error tests:"
runtest libgpg-error t-strerror --verbose
runtest libgpg-error t-syserror --verbose
runtest libgpg-error t-version --verbose

echo "------------------------------"
echo "npth tests:"
runtest npth t-mutex --verbose
runtest npth t-thread --verbose

echo "------------------------------"
echo "libassuan tests:"
runtest libassuan version --verbose
runtest libassuan pipeconnect --verbose
# TODO libassuan/fdpassing needs to be reimplimented to run on Android
#runtest libassuan fdpassing --verbose

echo "------------------------------"
echo "libksba tests:"
runtest libksba cert-basic --verbose
runtest libksba t-crl-parser
runtest libksba t-dnparser

echo "------------------------------"
echo "libgcrypt tests:"
runtest libgcrypt version --verbose
runtest libgcrypt mpitests --verbose
runtest libgcrypt tsexp --verbose
runtest libgcrypt t-convert --verbose
runtest libgcrypt t-mpi-bit --verbose
runtest libgcrypt t-mpi-point --verbose
runtest libgcrypt curves --verbose
runtest libgcrypt prime --verbose
runtest libgcrypt basic --verbose
runtest libgcrypt keygen --verbose --progress
runtest libgcrypt pubkey --verbose
runtest libgcrypt hmac --verbose
runtest libgcrypt hashtest --verbose
runtest libgcrypt t-kdf --verbose
runtest libgcrypt keygrip --verbose
runtest libgcrypt fips186-dsa --verbose
runtest libgcrypt aeswrap --verbose
runtest libgcrypt pkcs1v2 --verbose
# TODO enable --prefer-fips-rng, its temporarily disabled because it hangs for hours
runtest libgcrypt random --verbose --progress
runtest libgcrypt dsa-rfc6979 --verbose
runtest libgcrypt t-ed25519 --verbose
runtest libgcrypt benchmark --verbose
runtest libgcrypt bench-slope --verbose

echo "------------------------------"
echo "gpgme tests:"
runtest gpgme t-version
runtest gpgme t-engine-info
runtest gpgme t-data
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-import --verbose pubdemo.asc
runtest gpgme run-import --verbose pubkey-1.asc
runtest gpgme run-keylist --verbose
runtest gpgme run-import --verbose seckey-1.asc
runtest gpgme run-import --verbose secdemo.asc
runtest gpgme run-keylist --verbose

echo $SUCCESS
