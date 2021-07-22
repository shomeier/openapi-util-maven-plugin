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
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>merge</goal>
            </goals>
            <configuration>
                <headerFile>${project.basedir}/src/main/resources/header.yaml<headerFile>
                <!-- resources element is similar to maven-resources plugin
                    which means you can use <includes> <excludes> here -->
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
## Using in conjunction with openapi-generator-maven-plugin

You can then process the output further by for example passing it as <inputSpec> for the [openapi-generator-maven-plugin](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-maven-plugin/README.md).
This plugin is bound to phase 'initialize' which comes before 'generate-sources' to which the openapi-generator plugin is bound to:

```xml
<plugin>
    <groupId>io.github.shomeier</groupId>
    <artifactId>openapi-util-maven-plugin</artifactId>
    <version>1.0.0</version>
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
            </configuration>
        </execution>
    </executions>
</plugin>

<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>5.1.0</version>
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