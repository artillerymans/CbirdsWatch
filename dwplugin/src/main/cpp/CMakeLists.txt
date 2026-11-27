cmake_minimum_required(VERSION 3.22)
project(cbirds_dw LANGUAGES C)

set(CBIRDS_SOURCE "${CMAKE_CURRENT_LIST_DIR}/../../../../app/src/main/cpp")
add_library(cbirds_dw SHARED
    plugin_bridge.c
    ${CBIRDS_SOURCE}/cbirds/cells.c
    ${CBIRDS_SOURCE}/cbirds/font.c
    ${CBIRDS_SOURCE}/cbirds/gif.c
    ${CBIRDS_SOURCE}/cbirds/kitty_graphics.c
    ${CBIRDS_SOURCE}/cbirds/letters.c
    ${CBIRDS_SOURCE}/cbirds/link.c
    ${CBIRDS_SOURCE}/cbirds/options.c
    ${CBIRDS_SOURCE}/cbirds/picture.c
    ${CBIRDS_SOURCE}/cbirds/png.c
    ${CBIRDS_SOURCE}/cbirds/sign.c
    ${CBIRDS_SOURCE}/cbirds/sky3d.c
    ${CBIRDS_SOURCE}/cbirds/spatial_grid.c
    ${CBIRDS_SOURCE}/cbirds/vt.c
)
target_include_directories(cbirds_dw PRIVATE ${CBIRDS_SOURCE})
set_target_properties(cbirds_dw PROPERTIES
    C_STANDARD 99 C_STANDARD_REQUIRED YES C_EXTENSIONS NO
    C_VISIBILITY_PRESET hidden)
target_compile_options(cbirds_dw PRIVATE -O2 -Wall -Wextra)
target_link_options(cbirds_dw PRIVATE -Wl,--no-undefined)
target_link_libraries(cbirds_dw PRIVATE jnigraphics m)
