# kalfor
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cc.vileda.kalfor/kalfor-library/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cc.vileda.kalfor/kalfor-library)
[![Build Status](https://travis-ci.org/derveloper/kalfor.svg?branch=master)](https://travis-ci.org/derveloper/kalfor)
[![](https://tokei.rs/b1/github/derveloper/kalfor)](https://github.com/Aaronepower/tokei)
[![codecov](https://codecov.io/gh/derveloper/kalfor/branch/master/graph/badge.svg)](https://codecov.io/gh/derveloper/kalfor)
[![Dependency Status](https://www.versioneye.com/user/projects/591f41b98dcc41003af21ec7/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/591f41b98dcc41003af21ec7)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/vert-x3/vertx-awesome#microservices)

kalfor is a HTTP multiplexer microservice

## what?
kalfor transforms a single HTTP `POST` request to multiple parallel HTTP `GET` requests to a given HTTP backend
which then are combined and send back to the client in a single JSON response.

## why
while developing frontends for REST APIs you'll face performance problems because you have to make too many requests.
with kalfor you can combine all your REST Calls into a single `POST` request.
kalfor will fetch each endpoint in parallel for you and and respond all API responses in a single JSON to you.

## demo instance

You can find the demo instance at https://kalfor.app.vileda.cc/combine

### try it yourself
```
$ curl -H'Content-Type: application/json' \
--data '[{"proxyBaseUrl":"https://api.github.com", "proxyRequests":[{"path":"/", "key":"github"}]},'\
'{"proxyBaseUrl":"https://api.spotify.com", "proxyRequests":[{"path":"/v1", "key":"spotify"}]}]' \
'https://kalfor.app.vileda.cc/combine'
```

## api

### request schema

The schema for a kalfor request
```javascript
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "proxyBaseUrl": {
        "type": "string"
      },
      "headers": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "value": {
              "type": "string"
            }
          },
          "required": [
            "name",
            "value"
          ]
        }
      },
      "proxyRequests": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "path": {
              "type": "string"
            },
            "key": {
              "type": "string"
            }
          },
          "required": [
            "path",
            "key"
          ]
        }
      }
    },
    "required": [
      "proxyBaseUrl",
      "proxyRequests"
    ]
  }
}
```

A request with all current features
```javascript
[
  {
    "proxyBaseUrl": "https://api.github.com",
    "proxyRequests": [
      {
        "path": "/",
        "key": "github"
      }
    ]
  },
  {
    "proxyBaseUrl": "https://api.spotify.com",
    "headers": [
      {
        "name": "Authorization",
        "value": "Bearer <YOUR_SPOTIFY_API_KEY>"
      }
    ],
    "proxyRequests": [
      {
        "path": "/v1/me",
        "key": "spotify"
      }
    ]
  }
]
```

## installation

### maven

#### Add kalfor dependency
```xml
<dependencies>
    <dependency>
        <groupId>cc.vileda.kalfor</groupId>
        <artifactId>kalfor-library</artifactId>
        <version>4.0.0</version>
    </dependency>
</dependencies>
```

### gradle

#### Add kalfor dependency
```groovy
compile 'cc.vileda.kalfor:kalfor-library:4.0.0'
```

## Use it

### create your http server

```kotlin
fun main(args: Array<String>) {
    print("starting kalfor server...")
    embeddedServer(Netty, 8080) {
        routing {
            post("/combine") {
                val json = call.request.receive(String::class)
                val resp = validateSchema(json)
                        .fold({
                            Gson().toJson(kalforPost(Gson()
                                    .fromJson<List<KalforRequest>>(json)))
                        }, {
                            it.printStackTrace()
                            it.message!!
                        })
                call.respondText(resp)
            }
        }
    }.start(wait = true)
}
```

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
