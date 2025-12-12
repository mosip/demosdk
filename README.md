# MOSIP Demo SDK

[![Maven Package upon a push](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml/badge.svg?branch=release-1.3.x)](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml)  
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.x&project=mosip_demosdk&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.3.x&id=mosip_demosdk)

## Overview

The **Demo SDK** provides core demographic authentication capabilities used by the **ID Authentication subsystem**.  
It includes:

- Demographic data matching logic
- Name and address normalization utilities
- Support functions required for demographic authentication workflows

This SDK is referenced by **ID-Authentication**, available here:  
 https://github.com/mosip/id-authentication/tree/master/authentication

## Local Setup

The project can be set up in two ways:

1. [Local Setup (for Development or Contribution)](#local-setup-for-development-or-contribution)

### Prerequisites

Before you begin, ensure you have the following installed:

- **JDK**: 21.0.3
- **Maven**: 3.9.6

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

## Setting Up Locally

Follow the steps below to set up and build the Demo SDK.

### 1. Clone the repository

```bash
git clone https://github.com/mosip/demosdk.git
cd demosdk
```

### 2. Build the project

```bash
mvn clean install -Dgpg.skip=true
```

This will compile the SDK and install it into your local Maven repository.


## Usage

To include Demo SDK in your Maven project:

```xml
<dependency>
    <groupId>io.mosip</groupId>
    <artifactId>demosdk</artifactId>
    <version>1.3.x</version>
</dependency>
```

(Replace **1.3.x** with the appropriate released version.)

For detailed usage examples and integration steps, refer to the **ID-Authentication repository**.

---

## Documentation

Additional documentation and design references are available in the main MOSIP documentation portal:  
 https://github.com/mosip/documentation/tree/1.2.0/docs

---

## License

This project is licensed under the **Mozilla Public License 2.0**.  
See the [LICENSE](LICENSE) file for more details.
