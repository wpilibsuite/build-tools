# Jenkins Jobs Generator

These scripts generate the jobs on Jenkins. Each job is stored in a separate script in the `jobs` subdirectory. They are maintained by a seed job, that has been manually set up in Jenkins. The parameters for the seed job are:

* Clone this repo → Check for SCM Changes: `H/5 * * * *`
* Invoke Gradle script → Use Gradle Wrapper: `true`, Subdirectory: `jenkins-jobs`
* Invoke Gradle script → Tasks: `clean test`
* Process Job DSLs → DSL Scripts: `jenkins-jobs/jobs/**/*Jobs.groovy`
* Process Job DSLs → Additional classpath: `src/main/groovy`
* Publish JUnit test result report → Test report XMLs: `build/test-results/**/*.xml`

Documentation for the Jobs DSL is available [here](https://github.com/jenkinsci/job-dsl-plugin/wiki).