#!/bin/sh
# Builds Mirage.app into <project>/dist using jpackage. macOS only; needs JDK 25+ and Maven.
#
#   packaging/macos/build-app.sh
#   SKIP_TESTS=1 packaging/macos/build-app.sh
#
# The app is built for the architecture of the JDK used to run this script.
set -eu

APP_NAME="Mirage"
APP_VERSION="${APP_VERSION:-1.0.0}"

here=$(cd "$(dirname "$0")" && pwd -P)
root=$(cd "$here/../.." && pwd -P)
cd "$root"

if [ "$(uname -s)" != "Darwin" ]; then
  echo "build-app.sh builds the macOS app and must be run on a Mac." >&2
  exit 1
fi
for tool in java jpackage mvn; do
  command -v "$tool" >/dev/null 2>&1 || { echo "Missing '$tool'. Install JDK 25+ and Apache Maven first." >&2; exit 1; }
done

jdk_major=$(java -version 2>&1 | sed -n 's/^[^"]*"\([0-9][0-9]*\).*/\1/p' | head -n 1)
if [ "${jdk_major:-0}" -lt 25 ]; then
  echo "Mirage needs JDK 25 or newer to build; found JDK ${jdk_major:-unknown}." >&2
  exit 1
fi

if [ "${SKIP_TESTS:-0}" = "1" ]; then
  mvn -q clean package -DskipTests
else
  mvn -q clean package
fi

version=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
jar="mirage-$version.jar"

rm -rf target/dist target/fx dist
mkdir -p target/dist target/fx dist
mvn -q dependency:copy-dependencies -DoutputDirectory=target/dist -DincludeScope=runtime
cp "target/original-$jar" target/dist/

# JavaFX needs to be loaded as named modules. Keep the platform jars on the module path and
# the app plus its other runtime dependencies on the class path.
for platform_jar in target/dist/javafx-*-mac*.jar; do
  [ -e "$platform_jar" ] || continue
  mv "$platform_jar" target/fx/
done
if [ -z "$(find target/fx -name 'javafx-*.jar' -print -quit)" ]; then
  echo "Could not find macOS JavaFX modules in the Maven runtime dependencies." >&2
  exit 1
fi
rm -f target/dist/javafx-*.jar

# Generate native icon files from the same built-in artwork used by the app window.
java -Djava.awt.headless=true -cp target/classes com.mirage.icon.IconExporter packaging/icons

MODULES="javafx.controls,java.se,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.zipfs"
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input target/dist \
  --main-jar "original-$jar" \
  --main-class com.mirage.Launcher \
  --icon packaging/icons/icon.icns \
  --module-path target/fx \
  --add-modules "$MODULES" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

echo
echo "Built: $root/dist/$APP_NAME.app"
echo "Install it, and the Mirage terminal command, with: packaging/macos/install.sh"
