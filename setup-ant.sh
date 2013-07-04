#!/bin/bash

if ! type -P android &> /dev/null; then
    echo "Error: 'android' utility is not in your path."
    echo "  Did you forget to setup the SDK?"
    exit 1
fi

android update project --path . --name GnuPrivacyGuard --subprojects
android update lib-project --path external/ActionBarSherlock/actionbarsherlock
