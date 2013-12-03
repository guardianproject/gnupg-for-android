#/bin/bash

ASTYLE=`which astyle 2> /dev/null`
if [[ $? -ne 0 ]]; then
    echo "astyle code formatter not installed. aborting."
    exit 1
fi

$ASTYLE --help 2>&1 | grep "pad-first-paren-out" > /dev/null
if [[ $? -eq 0 ]]; then
    ASTYLE_ARGS="--style=kr --indent=spaces=4 --convert-tabs --pad-oper --unpad-paren --pad-first-paren-out --pad-header --align-pointer=type --align-reference=type"
else
    ASTYLE_ARGS="--style=kr --indent=spaces=4 --convert-tabs --pad-oper  --pad-header --align-pointer=type --align-reference=type"
fi

$ASTYLE $ASTYLE_ARGS jni/*.h
$ASTYLE $ASTYLE_ARGS jni/*.c

