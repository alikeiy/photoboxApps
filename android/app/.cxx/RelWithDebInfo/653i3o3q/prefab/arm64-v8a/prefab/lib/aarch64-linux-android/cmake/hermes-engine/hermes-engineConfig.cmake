if(NOT TARGET hermes-engine::libhermes)
add_library(hermes-engine::libhermes SHARED IMPORTED)
set_target_properties(hermes-engine::libhermes PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/5140b06fca7b22505b23fcffaa7a166d/transformed/jetified-hermes-android-0.81.0-release/prefab/modules/libhermes/libs/android.arm64-v8a/libhermes.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/5140b06fca7b22505b23fcffaa7a166d/transformed/jetified-hermes-android-0.81.0-release/prefab/modules/libhermes/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

