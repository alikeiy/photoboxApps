if(NOT TARGET fbjni::fbjni)
add_library(fbjni::fbjni SHARED IMPORTED)
set_target_properties(fbjni::fbjni PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/a18451f415b73841e30da63b3d9805af/transformed/fbjni-0.7.0/prefab/modules/fbjni/libs/android.x86_64/libfbjni.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/a18451f415b73841e30da63b3d9805af/transformed/fbjni-0.7.0/prefab/modules/fbjni/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

