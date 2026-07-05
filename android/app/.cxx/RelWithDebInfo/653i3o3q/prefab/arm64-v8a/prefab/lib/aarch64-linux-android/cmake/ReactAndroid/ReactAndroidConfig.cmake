if(NOT TARGET ReactAndroid::hermestooling)
add_library(ReactAndroid::hermestooling SHARED IMPORTED)
set_target_properties(ReactAndroid::hermestooling PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/hermestooling/libs/android.arm64-v8a/libhermestooling.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/hermestooling/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

if(NOT TARGET ReactAndroid::jsi)
add_library(ReactAndroid::jsi SHARED IMPORTED)
set_target_properties(ReactAndroid::jsi PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/jsi/libs/android.arm64-v8a/libjsi.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/jsi/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

if(NOT TARGET ReactAndroid::reactnative)
add_library(ReactAndroid::reactnative SHARED IMPORTED)
set_target_properties(ReactAndroid::reactnative PROPERTIES
    IMPORTED_LOCATION "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/reactnative/libs/android.arm64-v8a/libreactnative.so"
    INTERFACE_INCLUDE_DIRECTORIES "/private/var/folders/xv/j56l4qrj6fvd59fqp82nkj5w0000gn/T/cursor-sandbox-cache/d55fe7474559b7c1e974602ebf17264f/gradle/caches/8.14.3/transforms/6e6f07ea71e2ad1e3794b7d024435d07/transformed/jetified-react-android-0.81.0-release/prefab/modules/reactnative/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

