This test project created to test various rabbitmq configuration options in spring boot.

Before you start _create_ and _up_ the following `docker-compose.yaml`

```rabbit-mq:
  image: rabbitmq:management
  ports:
    - "5672:5672"
    - "15672:15672"
    
 
