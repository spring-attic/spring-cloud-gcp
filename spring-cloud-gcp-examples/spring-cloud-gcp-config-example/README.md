# Spring Cloud Config Client for Google Cloud Runtime Configurator API

This sample application demonstrates using the `Spring Cloud Config Client for GCP` in your code.

## Setup
1. Install the [Google Cloud SDK](https://cloud.google.com/sdk/). You
  can do so with the following command:
```
  curl https://sdk.cloud.google.com | bash
```
1. Create, enable billing on a project on the [Google Cloud Console](https://console.cloud.google.com).
1. Enable the [Runtime Config API](https://console.cloud.google.com/flows/enableapi?apiid=runtimeconfig.googleapis.com)
1. [Set up authentication](https://cloud.google.com/authentication/docs) using a service account.

## Testing

1. Create a configuration using the [Google Cloud SDK](https://cloud.google.com/sdk/). The configuration name
should be in the format `config`_`profile`, for example : `myapp_prod`
```
    gcloud beta runtime-config configs create myapp_prod
```
Then set the variables you wish to load.
```
  gcloud beta runtime-config configs variables set \
    queue_size 25 \
    --config-name myapp_prod
  gcloud beta runtime-config configs variables set \
    feature_x_enabled true \
    --config-name myapp_prod
```

1. In your Spring Boot application directory ,
add the following line to [src/main/resources/META-INF/spring.factories](src/main/resources/META-INF/spring.factories) :
```
  org.springframework.cloud.bootstrap.BootstrapConfiguration=org.springframework.cloud.gcp.config.GoogleConfigPropertySourceLocator
```

1. Add the following dependency to your [pom.xml](pom.xml):
```
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-gcp-config</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
  </dependency>
```

1. Update
[src/main/resources/application.properties](src/main/resources/application.properties):
```
    spring.cloud.config.name = "myapp" // (optional, "spring.application.name" is used, if not provided) 
    spring.cloud.config.profile = "prod"
```

1. Add Spring style configuration variables, see [SampleConfig.java](src/main/java/com/example/SampleConfig.java).
```
  @Value("${queue_size}")
  private int queueSize;

  @Value("${feature_x_enabled}")
  private boolean isFeatureXEnabled;
```

1. (Optional) [Spring Boot Actuator](http://cloud.spring.io/spring-cloud-static/docs/1.0.x/spring-cloud.html#_endpoints)
 provides support to have configuration parameters be reloadable with the POST `/refresh` endpoint.
    
    1. Add the following dependency to your `pom.xml`:
        ```
          <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
        ```
    1. Add `@RefreshScope` to your configuration class to have parameters be reloadable at runtime.
    1. Update a property with `gcloud`:
        ```
           gcloud beta runtime-config configs variables set \
             queue_size 200 \
             --config-name myapp_prod
        ```
    1. Send a POST request to the `/refresh` endpoint:
       ```
           curl -XPOST http://myapp.host.com/refresh
       ```