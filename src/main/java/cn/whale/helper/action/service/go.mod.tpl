module ${serviceModule}

go 1.19

replace (
	gitlab.meetwhale.com/whale/whale-framework => $!{whgoRelativePath}library
	google.golang.org/grpc => google.golang.org/grpc v1.26.0
	meetwhale.com/infra/project-validation/errors => $!{whgoRelative}infra/satelites/modules/project-validation/errors
	meetwhale.com/snapshot => $!{whgoRelative}infra/satelites/kubernetes/snapshot
	whgo => $!{whgoRelativePath}
)

require (
	gitlab.meetwhale.com/whale/whale-framework v0.0.1
	google.golang.org/grpc v1.49.0
	$!{whgo} v0.0.0
)
