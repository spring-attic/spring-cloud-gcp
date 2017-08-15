# Spring Cloud Config Client integration for Google Cloud Runtime Configurator API

[Runtime Configuration API](https://cloud.google.com/deployment-manager/runtime-configurator/) on Google Cloud Platform
allows dynamic configuration of your services.

[Spring Cloud Config Client](https://cloud.spring.io/spring-cloud-config/) provides developers the ability to 
bootstrap their application configuration with property sources.

This library [integrates](http://projects.spring.io/spring-cloud/spring-cloud.html#customizing-bootstrap-property-sources)
the [Runtime Configurator REST SDK](https://cloud.google.com/deployment-manager/runtime-configurator/reference/rest/)
as a Spring Cloud Config property source.

[Here](../spring-cloud-gcp-examples/spring-cloud-gcp-config-example) is a sample application that uses this integration.
