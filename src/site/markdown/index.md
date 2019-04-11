# AMW Maven Plugin

Plugin that allows to deploy applications through AMW.

To be able to send deployment requests to AMW a client certificate is required to authenticate.

To test or execute this plugin locally you need to configure the client certificate:

    -Djavax.net.ssl.keyStore=./path/PRIVATE-KEY.pfx
    -Djavax.net.ssl.keyStorePassword=PRIVATE-KEY-PASSWORD
    -Djavax.net.ssl.keyStoreType=PKCS12


The goals in this plugin do not require a project.

## Examples

__Deploy Build Step in TeamCity using Multi-Line applicationProperties__

(Note the "" around amwApplicationProperties=...)

    mvn ch.mobi.maven:amw-maven-plugin:deploy-multi \
    -DamwServerName=%amw.server.name%
    -D"amwApplicationProperties=%amw.application.properties%"
    -DamwRelease=%amw.release.name%
    -DamwSourceEnv=%amw.source.env%
    -DamwTargetEnv=%amw.target.env%

where amw.application.properties would look like:

    application_name_1=
    application_name_2=

## Links

 * [Liima](http://www.liima.org/)
