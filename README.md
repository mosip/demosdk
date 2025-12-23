# MOSIP Demo SDK

[![Maven Package upon a push](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml/badge.svg?branch=master)](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml)  
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure&project=mosip_demosdk&metric=alert_status)](https://sonarcloud.io/dashboard&id=mosip_demosdk)

## Overview

The **Demo SDK** provides core demographic authentication capabilities used by the **ID Authentication subsystem**.  
It includes:

- Demographic data matching logic
- Name and address normalization utilities
- Support functions required for demographic authentication workflows

This SDK is referenced by **ID-Authentication**, available here:  
 https://github.com/mosip/id-authentication/tree/master/authentication

## Installation

### Local Setup (for Development or Contribution)

1. Clone the repository:

```text
git clone <repo-url>
cd demosdk
```

2. Build the project:

```text
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

This will compile the SDK and install it into your local Maven repository.

## Usage

It's used as a library dependency in the **ID-Authentication** project.
To include Demo SDK in your Maven project:

```xml
<dependency>
    <groupId>io.mosip.demosdk</groupId>
    <artifactId>demosdk</artifactId>
    <version>1.3.0</version><!-- use latest released version -->
</dependency>
```

(Replace **1.3.0** with the appropriate released version.)

For detailed usage examples and integration steps, refer to the **ID-Authentication repository**.

## Documentation

Additional documentation and design references are available in the main MOSIP documentation portal:  
 https://github.com/mosip/documentation/tree/1.2.0/docs

## Contribution & Community

• To learn how you can contribute code to this application, [click here](https://docs.mosip.io/community/code-contributions).

• If you have questions or encounter issues, visit the [MOSIP Community](https://community.mosip.io/) for support.

• For any GitHub issues: [Report here](https://github.com/mosip/demosdk/issues)

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).
