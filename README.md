# Bullseye

Initial development for the Open Source Entity Resolution Architecture Framework ([OSERAF](https://groups.google.com/forum/#!forum/oseraf)) project.

## Requirements

We assume [node](http://nodejs.org/download/) is installed.

Once node and npm are available, you need to obtain a jar file created by Ikanow:

1. `git clone https://github.com/IKANOW/Infinit.e.git`
2. `cd Infinit.e/core/infinit.e.data_model`
3. `ant`

The jar file will now be in ./Infinit.e/core/infinit.e.data_model/dist.
To install this jar into your local .m2 repository:

1. `cd ./Infinit.e/core/infinit.e.data_model/dist`
2. `mvn install:install-file -Dfile=infinit.e.data_model.jar -DgroupId=com.ikanow.infinit.e.data_model -DartifactId=infinite -Dversion=1 -Dpackaging=jar`


`mvn clean install` should now work just fine to build Bullseye.


## Running

Bullseye is currently built as a runnable jar. You should be able to run it with:
> `java -Dconfig.file=path/to/conf.file -jar path/to/bullseye-webapp-${VERSION}.war 8081`

The current version is 0.0.1, in the above, and the default war location is in webapp/target. The config file
should be similar to that in `webapp/src/main/resources-filter/application.conf`, which will also be available as
`application.conf` at the root level of the compiled war (should you wish to extract it).


## Configuration

The primary source of configurability at the moment is in the backing OSERAF store. Two implementations currently exist:

1. A [Blueprints](http://blueprints.tinkerpop.com/) graph store
2. An [Ikanow](http://www.ikanow.com/) store which pulls search results into a Blueprints store for processing.

### Blueprints

For a Blueprints store, the argument to pass to the store configuration must be called factoryArgs, and it must be a
list of strings. These strings will be passed to a Blueprints
[GraphFactory](https://github.com/tinkerpop/blueprints/wiki/Code-Examples#use-graphfactory). A functional example is:
```
store: {
 clazz: "org.oseraf.bullseye.service.DataService.BlueprintsBullseyeEntityStore"
 args: {
   factoryArgs: [
 	 "blueprints.graph=com.tinkerpop.blueprints.impls.tg.TinkerGraph",
 	 "blueprints.tg.directory=/path/to/directory", // tinkergraph.json lives in the directory
 	 "blueprints.tg.file-type=GRAPHSON"
 ]
}
```

### Ikanow

For the Ikanow store, you must configure the Ikanow connection, as well as the backing Blueprints store.
An example is:
```
service: {
  store: {
   clazz: "org.oseraf.bullseye.ikanow.IkanowFallBackStore"
   args: {
     graph: {
       factoryArgs: ["blueprints.graph=com.tinkerpop.blueprints.impls.tg.TinkerGraph"]
     }
     ikanow: {
   	  url: "https://some-installed.ikanow.com/api/"
   	  user: "MyIkanowUsername"
   	  password: "secretpassword"
   	  communities: "abc123,456fed" // comma separated list of community ids
     }
   }
 }
}
```
In this example, an in-memory, initially empty graph will be used as the Blueprints store.
