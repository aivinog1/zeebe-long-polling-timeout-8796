# Zeebe test template with Java client

This repository contains a simple example of how to write a test for Zeebe. You can use it for reporting bugs or asking questions in the forum.

## Usage

1) Clone this repository, or use it as template to create a new one.

2) Modify the test and/or workflow to your needs.

    * JUnit test: `src/test/java/io/zeebe/WorkflowTest.java`
    * BPMN workflow: `src/test/resources/process.bpmn`

3) Check the results by running the test `WorkflowTest` locally in your IDE.

    Or, using the Maven command: `mvn clean test`.

4) Share your repository in the [forum](https://forum.zeebe.io/), or as part of an [issue](https://github.com/zeebe-io/zeebe/issues).

P.S. The test is failing at my machine (MBP 2019 16-inch, 6 Cores I7 2.6 GHz, 16 GB RAM, Docker: 6 CPUs, 6 GB RAM, SWAP 1GB) after approximately 10 minutes. When I change `ZEEBE_GATEWAY_LONGPOLLING_ENABLED` to true the test isn't fall after 2 hours.

## Code of Conduct

This project adheres to the Contributor Covenant [Code of
Conduct](/CODE_OF_CONDUCT.md). By participating, you are expected to uphold
this code. Please report unacceptable behavior to code-of-conduct@zeebe.io.
