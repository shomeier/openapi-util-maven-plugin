openapi-util-maven-plugin
============================

A Maven plugin to merge OpenAPI paths from multiple yaml files into a single one.
This is useful when some utilities can not handle multiple yaml files and you need to provide
a single 'consolidated' or 'merged' one.

Example see below.

Usage
============================

Add to your `build->plugins` section (default phase is `initialize` phase)
```xml
<plugin>
    <groupId>io.github.shomeier</groupId>
    <artifactId>openapi-util-maven-plugin</artifactId>
    <version>1.2.2</version>
    <executions>
        <execution>
            <goals>
                <goal>merge</goal>
            </goals>
            <configuration>
                <headerFile>${project.basedir}/src/main/resources/header.yaml<headerFile>
                <!-- resources element is similar to maven-resources plugin
                    which means you can use <includes>, <excludes> here -->
                <resources>
                  <resource>
                    <directory>${project.basedir}/src/main/resources/paths</directory>
                  </resource>
                </resources>
                <outputFile>${project.build.directory}/merged.yaml</outputFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Followed by:

```
mvn clean compile
```
Example
============================
For the configuration above let's imagine you have two yaml files in src/main/resources/paths:

## pets.yaml
```yaml
openapi: "3.0.0"
info:
  version: 1.0.0
  title: Swagger Petstore - Pets API
  license:
    name: MIT
servers:
  - url: http://petstore.swagger.io/v1
paths:
  /pets:
    ...
  /pets/{petId}:
    ...
components:
  ...
```

## stores.yaml
```yaml
openapi: "3.0.0"
info:
  version: 1.0.0
  title: Swagger Petstore - Stores API
  license:
    name: MIT
servers:
  - url: http://petstore.swagger.io/v1
paths:
  /stores:
    ...
  /stores/{storeId}:
    ...
components:
  ...
```

And the header.yaml looks like this:

## header.yaml
```yaml
openapi: "3.0.0"
info:
  version: 1.0.0
  title: Swagger Petstore API
  license:
    name: MIT
servers:
  - url: http://petstore.swagger.io/v1
```

This would result in the following merged yaml:

## merged.yaml
```yaml
openapi: 3.0.0
info:
  title: Swagger Petstore API
  license:
    name: MIT
  version: 1.0.0
servers:
- url: http://petstore.swagger.io/v1
paths:
  /pets:
    $ref: ./paths/pets.yaml#/paths/~1pets
  /pets/{petId}:
    $ref: "./paths/pets.yaml#/paths/~1pets~1{petId}"
  /stores:
    $ref: ./paths/stores.yaml#/paths/~1stores
  /stores/{storeId}:
    $ref: "./paths/stores.yaml#/paths/~1stores~1{storeId}"
```
### Expand/Resolve Paths

You also have the option to resolve the path references, either partial or fully:
#### ```<resolve>true<resolve>```
This will resolve the path references while for example schemas are stored in the local components section in the output file. This feature is described in detail [here](https://github.com/swagger-api/swagger-parser#1-resolve).

#### ```<resolveFully>true<resolveFully>```
This will fully resolve the path and all other references.
Full resolving in this case means all $refs are dereferenced (no external or internal reference to #components left).
The feature is described in detail [here](https://github.com/swagger-api/swagger-parser#2-resolvefully).

### Exclude Operations

You can exclude operations which have a marker in form of an OpenAPI extension set:
```yaml
...
/pets/{petId}:
    get:
      x-amazon-internal:
      summary: Info for a specific pet
      operationId: showPetById
    ...
    post:
      x-google-internal:
      summary: Create a pet
      operationId: createPets
```
Both operations will be excluded from the ouptut file via the following regex:
```<exclude>x-\w*-internal</exclude>```

### Transform Operations

You can provide your own transformation of an OpenAPI document via a lambda string which must implement the Consumer<OpenAPI> interface:
```xml
...
  <plugin>
    <groupId>io.github.shomeier</groupId>
    <artifactId>openapi-util-maven-plugin</artifactId>
    <version>1.2.2</version>
    <dependencies>
        <dependency>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-models</artifactId>
            <version>2.1.2</version>
        </dependency>
    </dependencies>
      <executions>
        <execution>
          <id>merge-all-api</id>
          <goals>
              <goal>merge</goal>
          </goals>
          <configuration>
              <headerFile>${project.basedir}/src/main/resources/rest-api-header.yaml
              </headerFile>
              <resources>
                  <resource>
                      <directory>${project.basedir}/src/main/resources</directory>
                      <include>**/paths/*.yaml</include>
                  </resource>
              </resources>
              <outputFile>
                  ${project.basedir}/src/main/resources/rest-api.yaml
              </outputFile>
              <transformers>
                <transformer>
                    <imports>
                    java.util.regex.Pattern,java.util.regex.Matcher,java.util.Collections,java.util.Optional
                    </imports>
                    <lambda>
                      openApi -> {
                            Optional.ofNullable(openApi.getPaths()).ifPresent(paths -> {
                                paths.entrySet().forEach(e -> {
                                    e.getValue().readOperations().forEach(o -> {
                                        Matcher m = Pattern.compile("^/([a-zA-Z]+)/.*$+").matcher(e.getKey());
                                        if (m.matches()) {
                                            o.setTags(Collections.singletonList(m.group(1)));
                                        }
                                    });
                                });
                            });
                          }
                    </lambda>
                </transformer>
              </transformers>
              <resolve>true</resolve>
          </configuration>
        </execution>
      </executions>
  </plugin>
...
```
This example will take the first path element from every "paths" item and set it as tag ("petsapi"):
```xml
...
paths:
  /petsapi/pets:
    get:
      summary: List all pets
      operationId: listPets
      tags:
        - petsapi
...
```
Note that the jars set as plugin dependencies are set on the compilation classpath for lambda compilation.
In this case swagger-models.
## Using in conjunction with openapi-generator-maven-plugin

You can then process the output further by for example passing it as \<inputSpec\> for the [openapi-generator-maven-plugin](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-maven-plugin/README.md).
This plugin is bound to phase 'initialize' which comes before 'generate-sources' to which the openapi-generator plugin is bound to:

```xml
<plugin>
    <groupId>io.github.shomeier</groupId>
    <artifactId>openapi-util-maven-plugin</artifactId>
    <version>1.2.2</version>
    <executions>
        <execution>
            <goals>
                <goal>merge</goal>
            </goals>
            <configuration>
                <headerFile>${project.basedir}/src/main/resources/header.yaml<headerFile>
                <resources>
                  <resource>
                    <directory>${project.basedir}/src/main/resources/paths</directory>
                  </resource>
                </resources>
                <outputFile>${project.build.directory}/merged.yaml</outputFile>
                <resolve>true</resolve>
            </configuration>
        </execution>
    </executions>
</plugin>

<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>5.2.1</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.build.directory}/merged.yaml</inputSpec>
                <generatorName>java</generatorName>
                <configOptions>
                   <sourceFolder>src/gen/java/main</sourceFolder>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```