#!/bin/bash
# PORTMASTER: zelda3.zip, Zelda 3 (fork, 2 screens).sh
# Zelda3 on the top panel, the fork's second screen (map, inventory) on the bottom one: one window over both panels
# (BananaOS SDL: SDL_BANANA_PANELS=side), touches on the bottom half.

XDG_DATA_HOME=${XDG_DATA_HOME:-$HOME/.local/share}
controlfolder="/roms/ports/PortMaster"
[ -d "$XDG_DATA_HOME/PortMaster/" ] && controlfolder="$XDG_DATA_HOME/PortMaster"

source $controlfolder/control.txt
[ -f "${controlfolder}/mod_${CFW_NAME}.txt" ] && source "${controlfolder}/mod_${CFW_NAME}.txt"
get_controls

GAMEDIR="/$directory/ports/zelda-fork2"
cd $GAMEDIR

# run-port switched the bottom panel's backlight off for a one-panel port: this one uses both
. /usr/libexec/banana/one-panel
one_panel_dark top
one_panel_restore

export SDL_GAMECONTROLLERCONFIG="$sdl_controllerconfig"
export SDL_VIDEODRIVER=evdev SDL_BANANA_PANELS=side SDL_BANANA_TOP=${BANANA_TOP:-DSI-2}
export ZELDA3_SECOND_SCREEN=side

$GPTOKEYB "zelda3.fork2" &
./zelda3.fork2 2>&1 | tee ./log.txt
kill -9 $(pidof gptokeyb) 2>/dev/null
printf "\033c" >> /dev/tty1
