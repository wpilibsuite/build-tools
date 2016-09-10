# Jenkins Jobs Generator

These scripts generate the jobs on Jenkins. Each job is stored in a separate script in the `jobs` subdirectory. They are maintained by a seed job, that has been manually set up in Jenkins. The parameters for the seed job are:

* Clone this repo → Check for SCM Changes: `H/5 * * * *`
* Invoke Gradle script → Use Gradle Wrapper: `true`, Subdirectory: `jenkins-jobs`
* Invoke Gradle script → Tasks: `clean test`
* Process Job DSLs → DSL Scripts: `jenkins-jobs/jobs/**/*Jobs.groovy`
* Process Job DSLs → Additional classpath: `src/main/groovy`
* Publish JUnit test result report → Test report XMLs: `build/test-results/**/*.xml`

The following plugins are needed to use the Jobs DSL:

* Job DSL
* Post Build Script

Documentation for the Jobs DSL is available [here](https://github.com/jenkinsci/job-dsl-plugin/wiki).

This license for this repository can be found [here](license.txt)

# Attribution

The test setup and framework for testing was taken from https://github.com/sheehan/job-dsl-gradle-example, licensed under the Apache 2.0 license. You can find a copy of the license file from that repository under job-dsl-gradle-example_license