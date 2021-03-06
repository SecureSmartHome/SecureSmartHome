#!/usr/bin/env zsh

# Author: Wolfgang Popp

MASTER='192.168.0.100:5555'
SLAVE='192.168.0.101:5555'
APPS=( "${(@f)$(adb devices -l | grep usb | cut -f1 -d ' ')}" )

DO_DEPLOY=true
DO_CLEAR=false

if [[ $1 == "--clear" || $1 == "-c" ]]; then
    DO_DEPLOY=true
    DO_CLEAR=true
elif [[ $1 == "--clear-only" || $1 == "-o" ]]; then
    DO_DEPLOY=false
    DO_CLEAR=true
elif [[ $1 != "" || $1 == "-h" || $1 == "--help" ]]; then
    echo "This Script builds and deploys the Secure Smart Home System on Odroids and all"
    echo "Android smartphones that are connected via usb debugging."
    echo
    echo "Master will be deployed on: $MASTER"
    echo "Slave will be deployed on: $SLAVE"
    echo "Apps will be deployed on: $APPS"
    echo
    echo "Usage: $0  [ OPTION ]"
    echo
    echo "OPTION:"
    echo "  --help, -h         Print this help and exit"
    echo "  --clear, -c        Clear data cache on all devices, build and redeploy"
    echo "  --clear-only, -o   Only clear data cache on all devices and exit"

    exit
fi

adb connect $MASTER
adb connect $SLAVE

if $DO_CLEAR; then
    echo Clearing data on master: $MASTER
    adb -s $MASTER shell "pm clear de.unipassau.isl.evs.ssh.master"

    echo Clearing data on slave: $SLAVE
    adb -s $SLAVE shell "pm clear de.unipassau.isl.evs.ssh.slave"

    for app in "${APPS[@]}"; do
        echo Clearing data on app: $app
        adb -s $app shell "pm clear de.unipassau.isl.evs.ssh.app"
    done
fi

if $DO_DEPLOY; then

    ./gradlew master:assembleDebug slave:assembleDebug app:assembleDebug

    echo
    echo "deploying master on: $MASTER"
    adb -s $MASTER install -r ./master/build/outputs/apk/master-debug.apk
    adb -s $MASTER shell 'am start -n "de.unipassau.isl.evs.ssh.master/de.unipassau.isl.evs.ssh.master.activity.MasterMainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'

    for app in "${APPS[@]}"; do
        echo
        echo "deploying app on: $app"
        adb -s $app install -r ./app/build/outputs/apk/app-debug.apk
        adb -s $app shell 'am start -n "de.unipassau.isl.evs.ssh.app/de.unipassau.isl.evs.ssh.app.activity.AppMainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'
    done

    echo
    echo "deploying slave on: $SLAVE"
    adb -s $SLAVE install -r ./slave/build/outputs/apk/slave-debug.apk
    adb -s $SLAVE shell 'am start -n "de.unipassau.isl.evs.ssh.slave/de.unipassau.isl.evs.ssh.slave.activity.SlaveMainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'
fi
