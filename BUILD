load("@rules_proto//proto:defs.bzl", "proto_library")

java_binary(
    name = "DMTool",
    deps = [":dmtool_java_proto"],
    srcs = glob(["dmtool/*.java"]),
    main_class = "dmtool.Main",
)

proto_library(
    name = "dmtool_proto",
    srcs = ["dmtool/dmtool.proto"],
)

java_proto_library(
		name = "dmtool_java_proto",
    deps = [":dmtool_proto"],
)
