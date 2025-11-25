# Healthcare Data Lake & FHIR Pipeline

![Java](https://img.shields.io/badge/Language-Java_17+-ED8B00?style=for-the-badge&logo=java) ![FHIR](https://img.shields.io/badge/Standard-HL7_FHIR_R4-fire?style=for-the-badge&logo=hl7&logoColor=white) ![Database](https://img.shields.io/badge/Auth_DB-MongoDB-47A248?style=for-the-badge&logo=mongodb)

### Overview
A robust Command Line Interface (CLI) application designed for clinical environments. This system separates **Authentication/User Management** (stored in MongoDB) from **Clinical Data** (stored in a FHIR Server), simulating a real-world secure hospital architecture.

### Key Features
* **Role-Based Access Control (RBAC):** Distinct interfaces for Practitioners (Doctors) and Patients.
* **Dual-Database Architecture:**
    * **MongoDB:** Handles user credentials, session tokens, and application state.
    * **FHIR Server (HAPI):** Stores standardized patient resources (Patients, Observations, Encounters).
* **Robust Error Handling:** Graceful connection failure management and user session loops.

---


## Getting Started

This example shows how to interact with a **HAPI FHIR server** using Java. You will learn how to query, create, update, and delete FHIR resources.

## Testing the example

### Clone this repository

```bash
git clone https://github.com/cm2027/lab3-java-fhir.git
cd lab3-java-fhir
# code . # opens the repo in vscode.
```

### Installing pre-requisites

You can install the pre-requisites in two ways:

1. **Use the provided [devcontainer](.devcontainer/devcontainer.json)**
2. **Manually install them**

---

### Option 1: Using the devcontainer in this repo

#### Prerequisites

- docker
- vs-code
- [vs-code remote development extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.vscode-remote-extensionpack)

Then, open the repository in **VS Code**, and press (`ctrl` + `shift` + `p`) or (`cmd`+ `shift`+ `p` on mac) and select `Dev Containers: Rebuild and Reopen in Container`.

---

### Option 2: Manual install of pre-requiesites

#### Dependencies

You will need:

- Java 17+
- Maven or Gradle
- HAPI FHIR R4 structures library
- HAPI FHIR Client library

**Maven example:**
(already in the [pom.xml](pom.xml))

```xml
    <!-- https://mvnrepository.com/artifact/ca.uhn.hapi.fhir/hapi-fhir-structures-r4 -->
	<dependency>
		<groupId>ca.uhn.hapi.fhir</groupId>
		<artifactId>hapi-fhir-structures-r4</artifactId>
		<version>7.4.3</version>
	</dependency>

    <!-- https://mvnrepository.com/artifact/ca.uhn.hapi.fhir/hapi-fhir-client -->
    <dependency>
      <groupId>ca.uhn.hapi.fhir</groupId>
      <artifactId>hapi-fhir-client</artifactId>
      <version>7.4.3</version>
    </dependency>
```

**Gradle example:**
(this example uses maven)

```gradle
implementation 'ca.uhn.hapi.fhir:hapi-fhir-structures-r4:7.4.3'
implementation 'ca.uhn.hapi.fhir:hapi-fhir-client:7.4.3'
```

---

### Running the example

Open [`Example.java`](./src/main/java/com/github/cm2027/lab3/Example.java) and run it with your IDE.

Or you can run it with maven using the follwing command:

```bash
mvn compile exec:java -Dexec.mainClass="com.github.cm2027.lab3.Example"
```

---

## How to Use the FHIR Server

### Testing endpoints with the `Swagger` interface

The HAPI server exposes a Swagger interface that can be used to test the APIs endpoints, this is available at: [`https://hapi-fhir.app.cloud.cbh.kth.se/fhir/`](https://hapi-fhir.app.cloud.cbh.kth.se/fhir/), when you access it through a browser.

### Setting up the client

```java
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

FhirContext ctx = FhirContext.forR4();
// public fhir r4 endpoint with lots of data
// there is a fhir server hosted on kthcloud at
// https://hapi-fhir.app.cloud.cbh.kth.se/fhir
String serverBase = "http://hapi.fhir.org/baseR4";
IGenericClient client = ctx.newRestfulGenericClient(serverBase);
```

---

## Getting Data

### Queries

**1. Get a resource by ID**

```java
Patient patient = client.read()
        .resource(Patient.class)
        .withId("123")
        .execute();
```

**2. Search for resources**

```java
Bundle bundle = client.search()
        .forResource(Patient.class)
        .where(Patient.FAMILY.matches().value("Smith"))
        .returnBundle(Bundle.class)
        .execute();

List<Patient> patients = bundle.getEntry().stream()
        .map(e -> (Patient) e.getResource())
        .toList();
```

**3. Handle multiple pages**

- FHIR servers usually return a limited number of results per page (default 20).
- Use pagination to fetch all results:

```java
Bundle bundle = client.search()
        .forResource(Patient.class)
        .count(50) // the amount of resources per "page"
        .returnBundle(Bundle.class)
        .execute();

// will iterate and request all pages and add them to the bundle
while (bundle.getLink(Bundle.LINK_NEXT) != null) {
    bundle = client.loadPage().next(bundle).execute();
}
```

Another way to get all resources is to first fetch the count (how many results do we get with our query), and then set that count as the requested resources count

```java
Bundle countBundle = client
            .search()
            .forResource(Patient.class)
            .summaryMode(SummaryEnum.COUNT) // tells the FHIR server to only count the results
            .returnBundle(Bundle.class)
            .execute();

int totalElements = countBundle.getTotal(); // the total field is only populated when summaryMode is used

// Then we can fetch the entire bundle by specifying the totalElements as the count we want to fetch.
Bundle bundle = client
            .search()
            .forResource(Patient.class)
            .returnBundle(Bundle.class)
            .count(totalElements)
            .execute();

```

---

## Creating Data

```java
Patient patient = new Patient();
patient.addName().setFamily("Smith").addGiven("John");
patient.setGender(Enumerations.AdministrativeGender.MALE);

MethodOutcome outcome = client.create()
        .resource(patient)
        .execute();

// TODO: check if this id is the id of the operation or of the created Patient
System.out.println("Created patient ID: " + outcome.getId());
```

---

## Updating Data

```java
Patient patient = client.read()
        .resource(Patient.class)
        .withId("123")
        .execute();

patient.setBirthDate(new Date());
client.update()
        .resource(patient)
        .execute();
```

---

## Deleting Data

```java
client.delete()
        // on the IdType we need to specify what resource we have, and then the ID
        .resourceById(new IdType("Patient", "123"))
        .execute();
```

---

## Where can i read more?

- [HAPI FHIR client examples](https://hapifhir.io/hapi-fhir/docs/v/7.4.0/client/examples.html)
- [FHIR R4 Specification](https://www.hl7.org/fhir/R4/)
