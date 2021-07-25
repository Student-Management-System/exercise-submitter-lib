## Integration tests

Integration tests use docker containers to set up a student-management-system instance.

### Prerequisites

* `docker-compose` installed and available in the PATH
* A local copy of [StuMgmtDocker](https://github.com/Student-Management-System/StuMgmtDocker). Pass the location to the maven process via either:
    * Setting `-Dnet.ssehub.studentmgmt.docker.rootPath=/path/to/StuMgmtDocker` in the maven invocation
    * Creating a file `stu-mgmt-docker-rootPath.txt` in the root folder of this project with the path to StuMgmtDocker in the first line

### Skipping Integration Test

If you don't have docker installed or don't want to run the integration tests, pass `-DskipITs` to maven
