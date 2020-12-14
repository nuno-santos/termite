#!/bin/bash

echo "Name: $1"
echo "Api: $2"

echo "Creating avd $1"
echo "$ANDROID_SDK_PATH/tools/bin/./avdmanager -v create avd -n $1 -k 'system-images;android-$2;default;x86' -c 512M -f"
echo "no" | $ANDROID_SDK_PATH/tools/bin/./avdmanager -v create avd -n $1 -k "system-images;android-$2;default;x86" -c 512M -f
echo "no"
echo "Avd $1 created."

echo "Script finished."