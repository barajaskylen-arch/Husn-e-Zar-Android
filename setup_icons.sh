#!/bin/bash

LOGO_URL="https://cdn.phototourl.com/free/2026-08-17-587b1323-9091-4774-b398-949b8ca87b98.png"
wget -O logo.png "$LOGO_URL"

RES_DIR="app/src/main/res"

mkdir -p "$RES_DIR/mipmap-mdpi"
mkdir -p "$RES_DIR/mipmap-hdpi"
mkdir -p "$RES_DIR/mipmap-xhdpi"
mkdir -p "$RES_DIR/mipmap-xxhdpi"
mkdir -p "$RES_DIR/mipmap-xxxhdpi"
mkdir -p "$RES_DIR/mipmap-anydpi-v26"

convert logo.png -resize 48x48 "$RES_DIR/mipmap-mdpi/ic_launcher.png"
convert logo.png -resize 48x48 "$RES_DIR/mipmap-mdpi/ic_launcher_round.png"

convert logo.png -resize 72x72 "$RES_DIR/mipmap-hdpi/ic_launcher.png"
convert logo.png -resize 72x72 "$RES_DIR/mipmap-hdpi/ic_launcher_round.png"

convert logo.png -resize 96x96 "$RES_DIR/mipmap-xhdpi/ic_launcher.png"
convert logo.png -resize 96x96 "$RES_DIR/mipmap-xhdpi/ic_launcher_round.png"

convert logo.png -resize 144x144 "$RES_DIR/mipmap-xxhdpi/ic_launcher.png"
convert logo.png -resize 144x144 "$RES_DIR/mipmap-xxhdpi/ic_launcher_round.png"

convert logo.png -resize 192x192 "$RES_DIR/mipmap-xxxhdpi/ic_launcher.png"
convert logo.png -resize 192x192 "$RES_DIR/mipmap-xxxhdpi/ic_launcher_round.png"
convert logo.png -resize 192x192 "$RES_DIR/mipmap-xxxhdpi/ic_launcher_foreground.png"

# Write adaptive icons
cat << 'XML' > "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
XML

cp "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml"

rm -f "$RES_DIR/drawable/ic_launcher_foreground.xml"
rm -f "$RES_DIR/drawable-anydpi-v26/ic_launcher_foreground.xml"

echo "Images generated successfully."
