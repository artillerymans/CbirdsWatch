cmake_minimum_required(VERSION 3.22)
project(cbirds_wallpaper LANGUAGES C)

add_library(cbirds SHARED
    wallpaper_bridge.c
    cbirds/cells.c
    cbirds/font.c
    cbirds/gif.c
    cbirds/kitty_graphics.c
    cbirds/letters.c
    cbirds/link.c
    cbirds/options.c
    cbirds/picture.c
    cbirds/png.c
    cbirds/sign.c
    cbirds/sky3d.c
    cbirds/spatial_grid.c
    cbirds/vt.c
)

set_target_properties(cbirds PROPERTIES
    C_STANDARD 99
    C_STANDARD_REQUIRED YES
    C_EXTENSIONS NO
    C_VISIBILITY_PRESET hidden
)
target_compile_options(cbirds PRIVATE -O2 -Wall -Wextra)
target_link_options(cbirds PRIVATE -Wl,--no-undefined)
target_link_libraries(cbirds PRIVATE jnigraphics m)