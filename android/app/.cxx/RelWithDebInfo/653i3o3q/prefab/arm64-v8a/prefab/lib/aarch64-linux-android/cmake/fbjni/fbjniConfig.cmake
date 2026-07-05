if(NOT TARGET fbjni::fbjni)
add_library(fbjni::fbjni SHARED IMPORTED)
set_target_properties(fbjni::fbjni PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/5177f883cfc04d7a3c97e530870da5d8/transformed/jetified-fbjni-0.7.0/prefab/modules/fbjni/libs/android.arm64-v8a/libfbjni.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/5177f883cfc04d7a3c97e530870da5d8/transformed/jetified-fbjni-0.7.0/prefab/modules/fbjni/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

