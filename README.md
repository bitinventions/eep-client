# eep-client
HEPv3/EEP java client (https://github.com/sipcapture/HEP/blob/master/docs/HEP3_rev12.pdf).

## installation and use
Compile with maven and include the binary in your classpath.
```
mvn clean package
```

Or compile and install in your local maven repo:
```
mvn clean install
```
Add to your pom.xml dependencies:
```
<dependency>
	<groupId>eep</groupId>
	<artifactId>eep-client</artifactId>
	<version>1.0.0</version>
</dependency>
```



