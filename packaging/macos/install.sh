#!/bin/sh
# Installs Mirage.app and the Mirage terminal command.
#
#   packaging/macos/install.sh
#   packaging/macos/install.sh --uninstall
#
# The app goes to /Applications (or ~/Applications if that is not writable). The command goes to
# /usr/local/bin if writable, otherwise to ~/.local/bin.
set -eu

APP_NAME="Mirage"
here=$(cd "$(dirname "$0")" && pwd -P)
root=$(cd "$here/../.." && pwd -P)

apps_dir() {
  if [ -n "${MIRAGE_APPS_DIR-}" ]; then mkdir -p "$MIRAGE_APPS_DIR"; echo "$MIRAGE_APPS_DIR"; return; fi
  if [ -w /Applications ]; then echo /Applications; else mkdir -p "$HOME/Applications"; echo "$HOME/Applications"; fi
}

bin_dir() {
  if [ -n "${MIRAGE_BIN_DIR-}" ]; then mkdir -p "$MIRAGE_BIN_DIR"; echo "$MIRAGE_BIN_DIR"; return; fi
  if [ -d /usr/local/bin ] && [ -w /usr/local/bin ]; then echo /usr/local/bin; else mkdir -p "$HOME/.local/bin"; echo "$HOME/.local/bin"; fi
}

if [ "${1-}" = "--uninstall" ]; then
  if [ -n "${MIRAGE_APPS_DIR-}" ]; then set -- "$MIRAGE_APPS_DIR"; else set -- /Applications "$HOME/Applications"; fi
  for apps in "$@"; do rm -rf "$apps/$APP_NAME.app"; done
  if [ -n "${MIRAGE_BIN_DIR-}" ]; then set -- "$MIRAGE_BIN_DIR"; else set -- /usr/local/bin "$HOME/.local/bin"; fi
  for bin in "$@"; do rm -f "$bin/Mirage"; done
  echo "Removed $APP_NAME and the Mirage command."
  exit 0
fi

built="$root/dist/$APP_NAME.app"
if [ ! -d "$built" ]; then
  echo "Nothing to install: $built does not exist. Run packaging/macos/build-app.sh first." >&2
  exit 1
fi

apps=$(apps_dir)
rm -rf "$apps/$APP_NAME.app"
cp -R "$built" "$apps/"
echo "Installed $apps/$APP_NAME.app"

bin=$(bin_dir)
cp "$here/Mirage" "$bin/Mirage"
chmod 755 "$bin/Mirage"
echo "Installed $bin/Mirage"

case ":$PATH:" in
  *":$bin:"*) ;;
  *) echo "Note: $bin is not on your PATH. Add this line to ~/.zshrc:  export PATH=\"$bin:\$PATH\"" ;;
esac
echo "Try it: Mirage"
