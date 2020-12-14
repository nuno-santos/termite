#!/bin/bash

$ANDROID_SDK_PATH/platform-tools/./adb -s emulator-$1 shell getprop init.svc.bootanim