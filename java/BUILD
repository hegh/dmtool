java_binary(
    name = "DMTool",
    deps = [":dmtool_java_proto"],
    srcs = glob(["net/jonp/dmtool/*.java"]),
    main_class = "net.jonp.dmtool.Main",
)

java_proto_library(
    name = "dmtool_java_proto",
    deps = ["//proto:dmtool_proto"],
)