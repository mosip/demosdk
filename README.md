[![Maven Package upon a push](https://github.com/mosip/demosdk/actions/workflows/push_trigger.yml/badge.svg?branch=release-1.3.0)](https://github.com/mosip/demosdk/actions/workflows/push_trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.0&project=mosip_biometrics-util&id=mosip_demosdk&metric=alert_status)](https://sonarcloud.io/dashboard?id=mosip_demosdk)

# Demo SDK

## Overview
This library is used for demographic authentication in [ID-Authentication](https://github.com/mosip/id-authentication/tree/master/authentication). This SDK have implementations for demographic data match along with the name and address normalizations.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Setting Up Locally](#setting-up-locally)
- [License](#license)

## Prerequisites

Ensure you have the following installed before proceeding:

1. **Java**: Version 21.0.3
2. **Maven**: For building the project 3.9.6
3. **Git**: To clone the repository

---

## Setting Up Locally

### Follow these steps to set up the project on your local environment:

1. **Clone the repository**

Clone the repository from GitHub to your local machine:

```bash
	git clone https://github.com/mosip/demosdk.git
	cd demosdk
```

1. **Build the project**

Use Maven to build the project and resolve dependencies.

```bash
	mvn clean install -Dgpg.skip=true
```

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).

---
