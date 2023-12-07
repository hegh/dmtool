load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "com_google_protobuf",
    sha256 = "9bd87b8280ef720d3240514f884e56a712f2218f0d693b48050c836028940a42",
    strip_prefix = "protobuf-25.1",
    urls = [
        "https://github.com/protocolbuffers/protobuf/archive/refs/tags/v25.1.tar.gz",
    ],
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

http_archive(
    name = "rules_proto",
    sha256 = "904a8097fae42a690c8e08d805210e40cccb069f5f9a0f6727cf4faa7bed2c9c",
    strip_prefix = "rules_proto-6.0.0-rc1",
    url = "https://github.com/bazelbuild/rules_proto/releases/download/6.0.0-rc1/rules_proto-6.0.0-rc1.tar.gz",
)
load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()
