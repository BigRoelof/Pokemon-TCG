#!/usr/bin/env bash

set -e

ADB="$HOME/Android/Sdk/platform-tools/adb"
EMULATOR="$HOME/Android/Sdk/emulator/emulator"
AVD_NAME="Pixel_9"
PACKAGE_NAME="com.example.pokemontcg"
MAIN_ACTIVITY=".MainActivity"

echo "🚀 Checking emulator status..."

# Check if an emulator is already running
if ! $ADB devices | grep -q "emulator"; then
    echo "📱 Starting emulator ($AVD_NAME)..."
    # Auto-detect NVIDIA GPU for PRIME render offloading
    if command -v nvidia-smi &> /dev/null || lspci | grep -qi nvidia; then
        export __NV_PRIME_RENDER_OFFLOAD=1
        export __GLX_VENDOR_LIBRARY_NAME=nvidia
    fi
    $EMULATOR -avd "$AVD_NAME" -gpu host &
    
    echo "⏳ Waiting for device to connect to ADB..."
    $ADB wait-for-device
    
    echo "⏳ Waiting for Android OS to finish booting..."
    while [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "✅ Emulator fully booted!"
else
    echo "✅ Emulator is already running."
fi

echo "📦 Building and installing app..."
./gradlew installDebug

echo "🎉 Launching app..."
$ADB shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"

echo "✨ Done! App should be open on your emulator."
