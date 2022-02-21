/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe;

import com.github.dockerjava.api.model.*;
import io.camunda.zeebe.client.*;
import io.camunda.zeebe.client.api.*;
import io.camunda.zeebe.client.api.response.*;
import io.zeebe.containers.cluster.*;
import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Testcontainers
public class ProcessTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTest.class);

    @Container
    private final ZeebeCluster cluster =
            ZeebeCluster.builder()
                    .withEmbeddedGateway(false)
                    .withGatewaysCount(1)
                    .withBrokersCount(3)
                    .withPartitionsCount(3)
                    .withGatewayConfig(zeebeGatewayNode -> zeebeGatewayNode
                            .withEnv("ZEEBE_GATEWAY_LONGPOLLING_ENABLED", "true")
                            .self()
                            .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd
                                    .getHostConfig()
                                    .withCpuCount(1L)
                                    .withMemory(512L * 1024 * 1024)))
                    .withBrokerConfig(zeebeBrokerNode -> zeebeBrokerNode
                            .withEnv("ZEEBE_BROKER_BACKPRESSURE_ENABLED", "false")
                            .self()
                            .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd
                                    .getHostConfig()
                                    .withCpuCount(1L)
                                    .withMemory(1024L * 1024 * 1024)))
                    .withNodeConfig(zeebeNode -> zeebeNode
                            .self()
                            .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd
                                    .withExposedPorts(ExposedPort.tcp(9600))))
                    .build();

    @Test
    public void shouldCompleteProcessInstance() throws InterruptedException {
        try (final ZeebeClient client = cluster.newClientBuilder().build()) {
            client.newDeployCommand().addResourceFromClasspath("process.bpmn").send().join();
            client.newDeployCommand().addResourceFromClasspath("process_1.bpmn").send().join();
            client.newDeployCommand().addResourceFromClasspath("process_2.bpmn").send().join();
            for (int i = 0; i <= 19; i++) {
                final String taskId = String.format("task-%s", i);
                client
                        .newWorker()
                        .jobType(taskId)
                        .handler((c, t) -> c.newCompleteCommand(t.getKey()).variables(Map.of("taskId", taskId)).send().join())
                        .open();
            }

            final long durationOfTestInMillis = Duration.ofHours(2).toMillis();
            final long testStartMillis = System.currentTimeMillis();

            long attempt = 0L;

            while (System.currentTimeMillis() < testStartMillis + durationOfTestInMillis) {
                attempt++;
                LOGGER.info("Starting with an attempt: {}", attempt);

                final LocalDateTime timeBeforeRequest = LocalDateTime.now();

                final ZeebeFuture<ProcessInstanceResult> processFuture = client.newCreateInstanceCommand()
                        .bpmnProcessId("process")
                        .latestVersion()
                        .withResult()
                        .requestTimeout(Duration.ofSeconds(10))
                        .send();

                final ZeebeFuture<ProcessInstanceResult> process1Future = client.newCreateInstanceCommand()
                        .bpmnProcessId("process_1")
                        .latestVersion()
                        .withResult()
                        .requestTimeout(Duration.ofSeconds(10))
                        .send();

                TimeUnit.SECONDS.sleep(1);

                final ZeebeFuture<PublishMessageResponse> process1MessageFuture = client.newPublishMessageCommand()
                        .messageName("process_1_message")
                        .correlationKey("task-11")
                        .requestTimeout(Duration.ofSeconds(10))
                        .send();

                final ZeebeFuture<ProcessInstanceResult> process2Future = client.newCreateInstanceCommand()
                        .bpmnProcessId("process_2")
                        .latestVersion()
                        .withResult()
                        .requestTimeout(Duration.ofSeconds(10))
                        .send();

                processFuture.join();
                process1MessageFuture.join();
                process1Future.join();
                process2Future.join();

                final LocalDateTime timeAfterRequest = LocalDateTime.now();

                LOGGER.info("Processes {} took about: {}", attempt, Duration.between(timeBeforeRequest, timeAfterRequest));

                TimeUnit.SECONDS.sleep(30);
            }
        }
    }
}
