CAS Overlay Template [![Build Status](https://travis-ci.org/apereo/cas-overlay-template.svg?branch=master)](https://travis-ci.org/apereo/cas-overlay-template)
=======================

Generic CAS WAR overlay to exercise the latest versions of CAS. This overlay could be freely used as a starting template for local CAS war overlays.

# Versions

- CAS `6.2.x`
- JDK `11`

# Overview

To build the project, use:

```bash
# Use --refresh-dependencies to force-update SNAPSHOT versions
./gradlew[.bat] clean build
```

To see what commands are available to the build script, run:

```bash
./gradlew[.bat] tasks
```

To launch into the CAS command-line shell:

```bash
./gradlew[.bat] downloadShell runShell
```

To fetch and overlay a CAS resource or view, use:

```bash
./gradlew[.bat] getResource -PresourceName=[resource-name]
```

To list all available CAS views and templates:

```bash
./gradlew[.bat] listTemplateViews
```

To unzip and explode the CAS web application file and the internal resources jar:

```bash
./gradlew[.bat] explodeWar
```

# Configuration

- The `etc` directory contains the configuration files and directories that need to be copied to `/etc/cas/config`.

```bash
./gradlew[.bat] copyCasConfiguration
```

- The specifics of the build are controlled using the `gradle.properties` file.

## Adding Modules

CAS modules may be specified under the `dependencies` block of the [Gradle build script](build.gradle):

```gradle
dependencies {
    compile "org.apereo.cas:cas-server-some-module:${project.casVersion}"
    ...
}
```

To collect the list of all project modules and dependencies:

```bash
./gradlew[.bat] allDependencies
```

### Clear Gradle Cache

If you need to, on Linux/Unix systems, you can delete all the existing artifacts (artifacts and metadata) Gradle has downloaded using:

```bash
# Only do this when absolutely necessary
rm -rf $HOME/.gradle/caches/
```

Same strategy applies to Windows too, provided you switch `$HOME` to its equivalent in the above command.

# Deployment

- Create a keystore file `thekeystore` under `/etc/cas`. Use the password `changeit` for both the keystore and the key/certificate entries. This can either be done using the JDK's `keytool` utility or via the following command:

```bash
./gradlew[.bat] createKeystore
```

- Ensure the keystore is loaded up with keys and certificates of the server.

On a successful deployment via the following methods, CAS will be available at:

* `https://cas.server.name:8443/cas`

## Executable WAR

Run the CAS web application as an executable WAR:

```bash
./gradlew[.bat] run
```

Debug the CAS web application as an executable WAR:

```bash
./gradlew[.bat] debug
```

Run the CAS web application as a *standalone* executable WAR:

```bash
./gradlew[.bat] clean executable
```

## External

Deploy the binary web application file `cas.war` after a successful build to a servlet container of choice.  
NOTE: The cas.properties in the `/etc/cas/config`,must should be configured correctly
 and run the command `./gradlew[.bat] copyCasConfiguration`,
then the property `cas.server.name` will take effect when deploy to external servlet container.

## Docker

The following strategies outline how to build and deploy CAS Docker images.

### Jib

The overlay embraces the [Jib Gradle Plugin](https://github.com/GoogleContainerTools/jib) to provide easy-to-use out-of-the-box tooling for building CAS docker images. Jib is an open-source Java containerizer from Google that lets Java developers build containers using the tools they know. It is a container image builder that handles all the steps of packaging your application into a container image. It does not require you to write a Dockerfile or have Docker installed, and it is directly integrated into the overlay.

```bash
./gradlew build jibDockerBuild
```

### Dockerfile

You can also use the native Docker tooling and the provided `Dockerfile` to build and run CAS.

```bash
chmod +x *.sh
./docker-build.sh
./docker-run.sh
```
### Support OAuth2
implementation "org.apereo.cas:cas-server-support-oauth-webflow:${casServerVersion}"
```bash
cas.authn.oauth.grants.resource-owner.require-service-header=false
cas.authn.oauth.user-profile-view-type=NESTED
cas.authn.oauth.refresh-token.time-to-kill-in-seconds=2592000
cas.authn.oauth.code.time-to-kill-in-seconds=30
cas.authn.oauth.code.number-of-uses=1
cas.authn.oauth.access-token.time-to-kill-in-seconds=7200
cas.authn.oauth.access-token.max-time-to-live-in-seconds=28800
```
### Ldap  Authentication
implementation "org.apereo.cas:cas-server-support-ldap:${casServerVersion}"
```bash
#ldap connection
cas.authn.ldap[0].ldap-url=ldap://127.0.0.1:389
cas.authn.ldap[0].bind-dn=cn=manager,dc=maxcrc,dc=com
cas.authn.ldap[0].bind-credential=secret
#ldap search
cas.authn.ldap[0].principal-attribute-list=sn,cn
cas.authn.ldap[0].principal-attribute-password=userPassword
cas.authn.ldap[0].type=AUTHENTICATED
cas.authn.ldap[0].base-dn=ou=user,dc=maxcrc,dc=com
cas.authn.ldap[0].search-filter=cn={user}
cas.authn.ldap[0].subtree-search=true
cas.authn.ldap[0].dn-format=cn=%s,ou=user,dc=maxcrc,dc=com
cas.authn.ldap[0].enhance-with-entry-resolver=true

cas.authn.ldap[0].password-encoder.type=NONE
```
You can use Apache Directory Studio to browser ldap .  
NOTE: 'password-encoder' must be set to 'NONE',and the stored password of entry in ldap database must
be the format '{SHA}base64TheShaHash'，because the cas default handler(CompareAuthenticationHandler) compare 
the value by first getting the password hash byte[] with SHA algorithm,and then Base64 encode the hash。  
eg:  password plain text=jack  , then it is should stored in
    ldap database  format={SHA}WWcnyKDqTbO6LOzu3Mus09ezcbg=     

CompareAuthenticationHandler.java source code:
```bash
 /** Default password scheme. Value is {@value}. */
  protected static final String DEFAULT_SCHEME = "SHA:SHA";

  /** Default password attribute. Value is {@value}. */
  protected static final String DEFAULT_ATTRIBUTE = "userPassword";

  /** Password scheme. */
  private Scheme passwordScheme = new Scheme(DEFAULT_SCHEME);

  /** Password attribute. */
  private String passwordAttribute = DEFAULT_ATTRIBUTE;
@Override
  protected AuthenticationHandlerResponse authenticateInternal(
    final Connection c,
    final AuthenticationCriteria criteria)
    throws LdapException
  {
    final byte[] hash = digestCredential(criteria.getCredential(), passwordScheme.getAlgorithm());
    final CompareResponse compareResponse = c.operation(
      CompareRequest.builder()
        .controls(processRequestControls(criteria))
        .dn(criteria.getDn())
        .name(passwordAttribute)
        .value(String.format("{%s}%s", passwordScheme.getLabel(), LdapUtils.base64Encode(hash))).build()).execute();
    return
      new AuthenticationHandlerResponse(
        compareResponse,
        compareResponse.isTrue() ?
          AuthenticationResultCode.AUTHENTICATION_HANDLER_SUCCESS :
          AuthenticationResultCode.AUTHENTICATION_HANDLER_FAILURE,
        c);
  }
```





