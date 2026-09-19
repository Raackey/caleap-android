# CaLeap V25 Build Cleanup

The failed GitHub run exposed stale `com/word2prompt` sources in the repository.
Those files are unrelated to CaLeap and are now outside the CaLeap application namespace.

Before the next build, delete this repository folder if it exists:

`app/src/main/java/com/word2prompt/`

The workflow also removes that stale folder automatically as a defensive CI step.

CaLeap source should compile only from:

`app/src/main/java/com/maisor/caleap/`
