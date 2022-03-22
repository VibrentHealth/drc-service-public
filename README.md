# About drc-service
DRC service will be used to sync participant data with external DRC systems for AoURP program

# Responsibilities of DRC service:
* Sync participant data with external DRC systems
* Fetch participants genomics statuses
* Expose notification endpoints that would be consumed by external DRC systems for realtime communications

# Dependencies
* Java 11
* Lombok

## Setup and Running
- [Installing Lombok for intelliJ](#-installing-lombok-for-intellij)
- [Setup Configuration on IntelliJ](#-setup-configuration-on-intellij)
- [Environment variables](#-environment-variables)

### Setup
This section describes how to add Lombok into the IDEA and describes all the environment variables and their default values.
 
#### Installing Lombok for intelliJ

If you are using intelliJ, need to activate annotations processor:
    Settings -> Compiler -> Annotation Processors

Now install lombok plugin:

    Preferences -> Plugins
    Click Browse repositories...
    Search for "Lombok Plugin"
    Install
    Restart IntelliJ
    
#### Setup Configuration on IntelliJ
    
    Main class:
    
    com.vibrent.drc.DrcServiceApplication
    
    Use classpath of modules: navigate to drc-service
        
#### Environment variables:
| Variable                          | Description                                                                                           | Default Value                                 |
| ------------------------------    |-------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `DB_HOST`                    | Database host name                                                                                    | `localhost`                                   |
| `DB_PORT`                    | Database port                                                                                         | `3306`                                        |
| `DB_USERNAME` | Database username                                                                                     | `root`                                        |
| `DB_PASSWORD` | Database password                                                                                     | `password`                                    |
| `KAFKA_HOST` | URL of the Kafka server                                                                               | `_localhost:9092`                             |
| `KAFKA_ENABLED` | Boolean variable to check if kafka server is enabled                                                  | `true`                                        |
| `KEYCLOAK_BASEURL` | Base URL to connect to Keycloak used to interact with Keycloak                                        | `https://keycloak-dev.vibrenthealth.com/auth` |
| `KEYCLOAK_RESOURCEID` | KeyCloak Resource ID                                                                                  | `participant`                                 |
| `KEYCLOAK_PARTICIPANT_REALM` | KeyCloak participant realm name                                                                       | `default_participant_realm`                   |
| `KEYCLOAK_ENTERPRISE_REALM` | KeyCloak enterprise realm name                                                                        | `default_realm`                               |
| `KEYCLOAK_PARTICIPANT_ENABLED` | Participant portal keycloak enabled flag used to turn on/off participant realm keycloak functionality | `true`                                        |
| `KEYCLOAK_API_CLIENT_ID` | Client id to authenticate against participant realm                                                   | `subscriber-server-api`                       |
| `CLIENT_CREDENTIAL_API_SECRET` | Client Secret key to authenticate against participant realm client                                    | `a2f3caa4-13d9-446d-87d6-89f26a7cc7ff`        |
| `DRC_BASE_URL` | Base URL to connect to DRC                                                                            | `https://pmi-drc-api-test.appspot.com/rdr/v1` |
| `DRC_ENABLED` | Is DRC turned on for this environment?                                                                | `true`                                        |
| `DRC_TIMEOUT` | Timeout when making DRC calls                                                                         | `90000`                                        |
| `DRC_CERTIFICATE_FILE_PATH` | DRC Certificate file path                                                                             | `/opt/data/drc/cert.json`                                        |
| `DRC_GENOMIC_BATCH_PROCESSING_SIZE` | Genomics report ready batch processing size                                                           | `100`                                        |
| `DRC_GENOMIC_PARTICIPANT_STATUS` | CRON Expression used to retrieve the Genomic report ready status                                      | `0 0 0/6 ? * * *`                                        |
| `DRC_GENOMIC_PARTICIPANT_BATCH_PROCESSING` | CRON Expression used to process the Genomic report ready status batch                                 | `0 0/15 * ? * * *`                                        |
| `SUPPLY_STATUS_ENABLED` | Flag to enable supply status requests from DRC service                                                | `false`                                        |
| `ACCOUNT_INFO_UPDATES_ENABLED` | Flag to enable Account Info updates from DRC service                                                  | `false`                                        |
| `BASICS_FORM_ID` | Basics form ID                                                                                        | `284`                                        |
| `BASICS_FORM_NAME` | Basics form name                                                                                      | `TheBasics`                                        |
| `API_SERVER` | API Service URL                                                                                       | `https://sub-default.qak8s.vibrenthealth.com`                                        |
| `CAFFEINE_CACHE_SPEC` | Caffeine cache spec configuration                                                                     | `maximumSize=150000`                                        |

 
# Code Coverage
The application quality checks are enforced via code coverage rules in the build file [pom.xml](pom.xml)
To customize the code coverage exclusions refer to the [COVERAGE.md](COVERAGE.md) file

# Application Monitoring
Application generates logs and they are pushed to a common ELK stack. Please read the [monitoring guide](MONITORING.md) for more details.
    
---
