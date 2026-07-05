if(NOT TARGET hermes-engine::libhermes)
add_library(hermes-engine::libhermes SHARED IMPORTED)
set_target_properties(hermes-engine::libhermes PROPERTIES
    IMPORTED_LOCATION "/Users/alikapermatasari/.gradle/caches/8.14.3/transforms/5d0a1bdcc2c5d492e0409f1745c7c8f4/transformed/hermes-android-0.81.0-debug/prefab/modules/libhermes/libs/android.arm64-v8a/libhermes.so"
    INTERFACE_INCLUDE_DIRECTORIES "/Users/alikapermatasari/.gradle/caches/8.14.3/transforms/5d0a1bdcc2c5d492e0409f1745c7c8f4/transformed/hermes-android-0.81.0-debug/prefab/modules/libhermes/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

