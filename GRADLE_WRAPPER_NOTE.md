# Gradle Wrapper Note

The repository's GitHub workflow installs Gradle 8.11.1 explicitly.
A generated `gradlew` wrapper is not included in this package because this
build environment does not have a standalone Gradle distribution available
from which to generate a valid wrapper JAR.

Do not substitute a guessed wrapper JAR. Generate the wrapper in a Gradle
environment with `gradle wrapper --gradle-version 8.11.1` when desired.
