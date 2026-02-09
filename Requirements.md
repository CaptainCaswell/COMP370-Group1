### Problem Statement
The system consists of a fault-tolerant server cluster where a primary server processes client requests while secondary servers remain synchronized and automatically take over when failures occur.

### Stakeholders
* Developers / System Designers 

    * Build and maintain the server, monitor, and client components
    * Design failover logic, heartbeat system, and split-brain prevention
    * Need clear requirements and logging

* System Administrators   

    * Deploy and run the servers and monitor
    * Monitor health, failures, and performance
    * Need reliable logs and monitoring info

* End Users

    * Send requests to the primary server
    * Expect uninterrupted processing even during failures
    * Care about availability and correct results

### Functional Requirements

1. FR1: Servers must send periodic heartbeats to the monitor.

2. FR2: Servers must be able to be promoted to PRIMARY by the monitor.

3. FR3: Servers must be able to demote to SECONDARY if needed.

4. FR4: Only the PRIMARY server processes client requests.

5. FR5: Monitor must track active servers and their roles.

6. FR6: Monitor must ensure only one PRIMARY exists at a time.

7. FR7: Monitor must respond to heartbeats with the server role and latest system sum.

8. FR8: Monitor must maintain the system sum for failover recovery.

9. FR9: Server must update and maintain a running sum.

10. FR10: System must promote a secondary if primary fails.

11. FR11: Servers must restore sum when promoted.

12. FR12: System must allow simulated heartbeat delay.

13. FR13: System must support multiple server instances.

### Non-Functional Requirements

1. NFR1: The system must detect primary failure within 5 heartbeats.

2. NFR2: The system must resume serving requests within 5 seconds after failover.

3. NFR3: The system must process client requests with an average response time of less than 5 seconds under normal operation.

4. NFR4: The monitor must process heartbeat messages from all servers within 3 seconds of receiving it.

5. NFR5: The system must remain available to process client requests 99% of the time during runtime testing.

6. NFR6: The system must log all server promotions, demotions, failures, and recoveries within 2 seconds of occurrence.
