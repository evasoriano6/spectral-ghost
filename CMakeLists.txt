
cmake_minimum_required(VERSION 3.18.1)

project("spectral-ghost")

# Define the shared library
add_library(
        native-lib
        SHARED
        native/NativeBridge.cpp
)

# Search for system libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)
find_library(vulkan-lib vulkan)

# Link libraries
target_link_libraries(
        native-lib
        ${log-lib}
        ${android-lib}
        ${aaudio-lib}
        ${vulkan-lib}
)
