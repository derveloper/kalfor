# kalfor
service for combining http requests

# installation

## maven
Add the bintray repo
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

Add kalfor dependency
```xml
<dependencies>
    <dependency>
        <groupId>cc.vileda.kalfor</groupId>
        <artifactId>kalfor-library</artifactId>
        <version>1.0.8</version>
    </dependency>
</dependencies>
```

Use it
```java
public class MyKalfor
{
	public static void main(String[] args) throws MalformedURLException
	{
		new Kalfor("https://some.api.example.com").listen(8080);
	}
}
```

# license
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
