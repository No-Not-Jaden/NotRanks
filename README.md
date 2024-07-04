# API
Replace VERSION with the most recent [release](https://github.com/No-Not-Jaden/NotRanks/releases)
## Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.No-Not-Jaden</groupId>
    <artifactId>NotRanks</artifactId>
    <version>VERSION</version>
</dependency>
```
## Gradle
```gradle
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
        implementation 'com.github.No-Not-Jaden:NotRanks:Tag'
}
```
