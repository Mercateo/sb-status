# sb-status
Ops tool to retrieve Health Status of a running Boot Application as well as its maven version

sb-status uses some reflection/attach/agent magic to conveniently get the maven version of a running Spring Boot Application identified by PID. The maven version will be sent to STDOUT. It also reads the actuator's healthEndpoint and returns non-zero, if the overall status is not 'UP'.

We use the nasty attach/agent method here in order to *not force any non-standard dependency* on the target Spring Boot Application. 
sb-status reads the maven version from the target's META-INF/MANIFEST.MF file.

We use this during deployment so that we can make sure a boot app is running and operational before we deploy another instance in order to provide an uninterrupted service.

### Usage

sb-status [-vbotah] pid 

  Sequential arguments:
    pid   
        Process ID of the Boot application you want to grab the
        Status/Version from

  Optional arguments:

    --verbose                         -v 
        Create verbose output for debugging

    --block                           -b 
        Use blocking mode

    --timeout=val                     -o 
        Timeout when using blocking mode  value is an integer.

    --toolsJar=val                    -t 
        Optional path to tools.jar – if unset, sb-status tries to find it
    
    --alternativeVersionAttribute=val -a 
        Name of the alternative Manifest-Attribute to use as Version, if
        it is set and non null - Defaults to 'Implementation-Version'
        
    --help                            -h This help

### TODO 

* Test on MacOSX

