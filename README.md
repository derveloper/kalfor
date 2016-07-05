# kalfor
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cc.vileda/kalfor/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/cc.vileda/kalfor)
[![Build Status](https://travis-ci.org/vileda/kalfor-java.svg?branch=master)](https://travis-ci.org/vileda/kalfor-java)

kalfor combines HTTP requests.

## what?
kalfor transforms a single HTTP `POST` request to multiple parallel HTTP `GET` requests to a given HTTP backend
which then are combined and send back to the client in a single JSON response.

## why
while developing frontends for REST APIs you'll face performance problems because you have to make too many requests.
with kalfor you can combine all your REST Calls into a single `POST` request. kalfor will fetch each endpoint in parallel for you and
and respond all API responses in a single JSON to you.

## demo instance

You can find the demo instance at https://kalfor.herokuapp.com/combine

### try it yourself
```
$ curl -H'Content-Type: application/json' \
--data '[{"proxyBaseUrl":"https://api.github.com", "proxyRequests":[{"path":"/", "key":"github"}]},'\
'{"proxyBaseUrl":"https://api.spotify.com", "proxyRequests":[{"path":"/v1", "key":"spotify"}]}]' \
'https://kalfor.herokuapp.com/combine'
```

## installation

### maven

#### Add kalfor dependency
```xml
<dependencies>
    <dependency>
        <groupId>cc.vileda.kalfor</groupId>
        <artifactId>kalfor-library</artifactId>
        <version>1.2.1</version>
    </dependency>
</dependencies>
```

### gradle

#### Add kalfor dependency
```groovy
compile 'cc.vileda.kalfor:kalfor:1.2.1'
```

## Use it

### create your main method

```java
public class MyKalfor
{
	public static void main(String[] args)
	{
		new Kalfor().listen(8080);
	}
}
```

now run the main method in your IDE or build yourself a fat-jar.

### combine
```
$ curl -H'Content-Type: application/json' \
--data '[{"proxyBaseUrl":"https://api.github.com", "proxyRequests":[{"path":"/", "key":"github"}]},'\
'{"proxyBaseUrl":"https://api.sipgate.com", "proxyRequests":[{"path":"/v1", "key":"sipgate"}]}]' \
'http://localhost:8080/combine'
{
    "github": {
        "current_user_url" : ...,
        ...
    },
    "sipgate" : {
        "authorizationOauthClientsSecretUrl" : ...,
        ...
    }
}
```

#### headers
kalfor is able to send specific headers to each backend. Just provide a list of headers in your request JSON

```
$ curl -H'Content-Type: application/json' \
--data '[{"proxyBaseUrl":"https://api.github.com", "proxyRequests":[{"path":"/", "key":"github"}]},'\
'{"proxyBaseUrl":"https://api.spotify.com", "headers":[{"name":"Authorization", "value": "Bearer <YOUR_SPOTIFY_API_KEY>"}], '\
'"proxyRequests":[{"path":"/v1/me", "key":"spotify"}]}]' \
'http://localhost:8080/combine'
```

### existing vert.x application
if you already have a vert.x application, just grab the
`kalfor-library` or `kalfor-combine-handler` maven module as a dependency.

## license
```
Copyright 2016 Tristan Leo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
