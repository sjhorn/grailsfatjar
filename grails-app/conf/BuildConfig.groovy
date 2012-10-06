grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.work.dir = "target/work"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.7
grails.project.source.level = 1.7

//grails.war.exploded = true
//grails.project.war.exploded.dir = "target/war"
grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
        excludes "grails-plugin-log4j", "log4j", "slf4j-log4j12", "jul-to-slf4j"
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        compile('javax.servlet:javax.servlet-api:3.0.1')
        compile(
            "org.eclipse.jetty.aggregate:jetty-server:8.1.5.v20120716", 
            "org.eclipse.jetty.aggregate:jetty-servlet:8.1.5.v20120716",
            "org.eclipse.jetty.aggregate:jetty-webapp:8.1.5.v20120716",
        ){
            excludes "javax.servlet", "slf4j-api"
        }
        compile('org.slf4j:slf4j-jdk14:1.6.2')
    }

    plugins {
        runtime ":hibernate:$grailsVersion"
//        runtime ":jquery:1.8.0"
//        runtime ":resources:1.1.6"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.4"

        build(":tomcat:$grailsVersion") {
            //excludes "log4j", "slf4j-log4j12"
        }
//        compile ':cache:1.0.0'
    }
}
