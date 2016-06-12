# sb-status
Ops tool to retrieve Health Status of a running Boot Application as well as its maven version

sb-status uses some reflection/attach/agent to conveniently get the maven version of a running Spring Boot Application identified by PID. It also reads the healthEndpoint and return non-0, if the overall status is not 'UP'.
The maven version will be sent to STDOUT.

We use the nasty attach/agent method here in order to *not force any non-standard dependency* on the target Spring Boot Application. 
sb-status reads the maven version from the target's META-INF/maven/<GROUPID>/<ARTIFACTID>/pom.properties file.

We use this during deployment so that we can make sure a boot app is running and operational before we deploy another instance in order to provide an uninterrupted service.

### TODO 

* Test on MacOSX
* Add retry logic with maximum timeout
