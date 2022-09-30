module ${serviceModule}

go 1.17

replace (
	gitlab.meetwhale.com/whale/whale-framework => $!{libraryRelativePath}
	google.golang.org/grpc => google.golang.org/grpc v1.26.0
	$!{whgo} => $!{whgoRelativePath}
)

require (
	gitlab.meetwhale.com/whale/whale-framework v0.0.1
	google.golang.org/grpc v1.49.0
	$!{whgo} v0.0.0
)
