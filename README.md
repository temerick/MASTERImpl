# Bullseye

Initial development for the Open Source Entity Resolution Architecture Framework ([OSERAF](https://groups.google.com/forum/#!forum/oseraf)) project.

## Requirements

We assume [node](http://nodejs.org/download/) is installed (v0.10.33 is known to work).
We also require [ant](http://ant.apache.org/) and [maven](https://maven.apache.org/).

Once node and npm are available, you need to obtain a jar file created by Ikanow:

1. `git clone https://github.com/IKANOW/Infinit.e.git`
2. `cd Infinit.e/core/infinit.e.data_model`
3. `ant`

The jar file will now be in ./Infinit.e/core/infinit.e.data_model/dist.
To install this jar into your local .m2 repository:

1. `cd ./Infinit.e/core/infinit.e.data_model/dist`
2. `mvn install:install-file -Dfile=infinit.e.data_model.jar -DgroupId=com.ikanow.infinit.e.data_model -DartifactId=infinite -Dversion=1 -Dpackaging=jar`

## Build

Assuming the requirements above, `mvn clean install` should build Bullseye.


## Running

Bullseye is currently built as a runnable jar. You should be able to run it with:
> `java -Dconfig.file=webapp/target/classes/example/bullseye.conf -jar webapp/target/bullseye.war`

By default, this will launch the bullseye on port 8081, and it can be accessed in a browser at [http://localhost:8081/bullseye](http://localhost:8081/bullseye).
If you wish to run on a different port, the port can be specified as an additional argument at the end of the command above
(for example, `java -jar bullseye.war 8080`).

You can also deploy the war into your server of choice. We have tested with [tomcat](http://tomcat.apache.org/) versions 7(.0.59) and 8(.0.17).
Additional documentation for such deployments, along with additional configuration documentation, is now available on the [wiki](https://github.com/OSERAF/MASTERImpl/wiki).
