# BoringdroidSettings

The settings app for Boringdroid.

## Spotless

This project uses [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) to
format source code, and you can use the below command to check and format source code before
you push changes to the repository for reviewing:

```shell
./gradlew spotlessCheck
./gradlew spotlessApply
``` 

If you encounter an error when use `./gradlew spotlessApply`, you should fix format errors 
manually, because the Spotless based formatter can't fix all errors.  