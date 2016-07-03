# kalfor
kalfor combines HTTP requests.

## what?
kalfor transforms a single HTTP `POST` request to multiple parallel HTTP `GET` requests to a given HTTP backend
which then are combined and send back to the client in a single JSON response.

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

#### Add the bintray repo
```xml
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-vileda-maven</id>
        <name>bintray</name>
        <url>http://dl.bintray.com/vileda/maven</url>
    </repository>
</repositories>
```

#### Add kalfor dependency
```xml
<dependencies>
    <dependency>
        <groupId>cc.vileda.kalfor</groupId>
        <artifactId>kalfor-library</artifactId>
        <version>1.0.17</version>
    </dependency>
</dependencies>
```

### gradle

#### Add the bintray repo
```groovy
repositories {
    maven {
        url  "http://dl.bintray.com/vileda/maven"
    }
}
```

#### Add kalfor dependency
```groovy
compile 'cc.vileda.kalfor:kalfor:1.0.17'
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
