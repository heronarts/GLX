#!/bin/sh
for f in led1 led2 led3 led4 led5 sparkle1 sparkle2 sparkle3 sparkle4 sparkle5
do
  echo "Compiling $f"
  ./texturec -f "$f.png" -o "../../src/main/resources/textures/$f.ktx"
done
echo "Done."
