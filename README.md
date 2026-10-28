FUA OpenMRS Module
==================

Description
-----------
This module supports Peru SIS Formato Unico de Atencion (FUA) workflows in OpenMRS.

Maven Coordinates
-----------------
Published releases are available from Maven Central under:

    io.github.proyecto-santaclotilde:fua-api
    io.github.proyecto-santaclotilde:fua-omod

Building from Source
--------------------
Use Java 8 or Java 21 and Maven 3.9+. Run `mvn clean verify` to test and
package the module.  The .omod file will be in the omod/target folder.

Alternatively you can add the snippet provided in the [Creating Modules](https://wiki.openmrs.org/x/cAEr) page to your 
omod/pom.xml and use the mvn command:

    mvn package -P deploy-web -D deploy.path="../../openmrs-1.8.x/webapp/src/main/webapp"

It will allow you to deploy any changes to your web 
resources such as jsp or js files without re-installing the module. The deploy path says 
where OpenMRS is deployed.

Running Spotless
----------------
This project uses Spotless for code formatting. Spotless is embedded in the build process, so when you run `mvn clean package`, Spotless will automatically format your code according to the project's style guidelines.

If you want to run Spotless separately, you can use the following Maven commands:

To apply the formatting:

    mvn spotless:apply

This will automatically format your code according to the project's style guidelines. It's recommended to run this command before committing your changes.

To check if your code adheres to the style guidelines without making any changes, you can run:

    mvn spotless:check

If this command reports any violations, you can then run `mvn spotless:apply` to fix them.

Remember, in most cases, you don't need to run these commands separately as Spotless will run automatically during the build process with `mvn clean package`.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.

Configuration and access for 1.0.90
----------------------------------

Related distribution issue: https://github.com/sihsalus/sihsalus/issues/244.
The distribution already uses 1.0.89; this candidate completes the remaining
configuration and access work without replacing a published artifact.

All generator calls use the current `fua.generator.headerName` and
`fua.generator.headerValue` global properties through one header builder.
There is no fallback token. Missing, blank, malformed or transport-reserved
headers stop the request before contacting the generator. Configure the secret
through the environment's approved secret-management procedure; never put it in
source, screenshots or request logs. Existing global-property values are retained
on upgrade and must be reviewed before rollout. Removing the packaged default
does not rotate a value already stored in a database.

The module reuses the privilege names already provisioned in `config.xml`:

| Operation | Required FUA privilege |
| --- | --- |
| Lists, individual records, state catalog, render and PDF | `Read Fua` |
| Record generation, record save, format upload and state creation | `Manage Fua` |
| Change a record's state | `Update Fua` |
| Purge a record or state through the legacy form | `Delete Fua` |

Generation and state changes also read existing FUA records/catalog metadata and
require `Read Fua`. Other OpenMRS patient/visit privileges still apply to clinical
generation. No role assignments or duplicate suffixed privileges are created.
Anonymous requests receive 401 and authenticated callers without the operation's
privilege receive 403 before controller work. Service authorization remains in
place for non-HTTP consumers.

State changes use the existing authorized `updateEstadoFua` service. The prior
version, revision increment and state update share its transaction; a persistence
failure rolls back all of them. Generator failures return generic errors without
upstream response bodies or exception details.

Validation and rollout
----------------------

Run the complete reactor with `mvn --batch-mode --no-transfer-progress clean verify`.
Tests exercise OpenMRS authorization advice, every mapped controller's denial
path, actual header construction in seven generator request flows, changed
configuration, invalid inputs, and SQL commit/rollback in an ephemeral H2 database.
The HTTP transport and all clinical fixtures are synthetic. These tests do not
establish MariaDB, browser, generator-service or deployed clinical acceptance.

PR CI also builds the distribution through its own Dockerfile, retaining its
source/release checksum pins and packaging checks. An ephemeral checkout installs
the candidate FUA archive into the same Maven cache used by the distro build.
The check compares the final image's FUA bytes with the candidate and verifies
module requirements against its packaged Core. It neither starts OpenMRS nor
publishes the image; deployed acceptance remains separate.

Before promoting to QLTY, verify current configured authentication without
disclosing its values, the immutable module/backend images, a recoverable backup,
and a non-administrator FUA test role. Verify list, generate, render, PDF and state
update against a journaled synthetic visit. Confirm both allowed and denied
operations, historical-version preservation and absence of duplicate records.
Publish 1.0.90 only after required review/CI, then change the distribution pin in a
separate reviewed change. This branch neither publishes nor deploys the module.

Rollback preserves existing FUA records, versions and global properties. Restoring
the previous module also restores its previous access/configuration behavior; it
does not undo committed data changes or rotate credentials.

Made with love
