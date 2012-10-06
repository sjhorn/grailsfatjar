import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import org.codehaus.groovy.runtime.StackTraceUtils

includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsWar")
includeTargets << grailsScript("_GrailsPackage")


services = [:]
entries = [:]
BUFFER = new byte[4 * 1024 * 1024]
i18nPatch = true

target(fatjar: "Create and run fatjar") {
    String appName = "${metadata['app.name']}-${metadata['app.version'] ?: '0.1-SNAPSHOT'}"
    String jarName = "${appName}.jar"
    String warName = "${appName}.war"
    
    buildConfig.grails.war.exploded = false
    buildConfig.grails.project.war.file = "target/${warName}"
    
    if(argsMap['nopatch']) {
        i18nPatch = false
        argsMap.remove('nopatch')
    }
    
    depends(parseArguments, checkVersion, war)
    
    grailsConsole.updateStatus "Writing to target/$jarName"
    try {
        new File("target/${jarName}").withOutputStream { OutputStream os ->
            
            Manifest manifest = new Manifest()
            def mainAttributes = manifest.mainAttributes
            [
                'Manifest-Version' : '1.0',
                'Main-Class' : 'example.JettyServer',
                'Grails-Version' : '2.1.1',
                'Implementation-Title' : 'Grails',
                'Implementation-Version' : '2.1.1',
                'Implementation-Vendor': 'grails.org'
            ].each { k,v ->
                mainAttributes.putValue(k, v)
            }
            
            new File("target/${warName}").withInputStream { InputStream is ->
                JarInputStream jis = new JarInputStream(is)
                JarOutputStream jos = new JarOutputStream(os, manifest)
                
                walkJar(jis, jos)
                addServices(services, jos)
                
                jos.close()
                jis.close()
            }
        }
    } catch(e) {
        StackTraceUtils.deepSanitize(e)
        grailsConsole.error e.message
        e.printStackTrace()
    }

}

setDefaultTarget('fatjar')

void addServices(Map services, JarOutputStream jos) {
    services.each { name, contents ->
        jos.putNextEntry(new JarEntry(name))
        if(name == "META-INF/grails-plugin.xml") {
            jos << '<plugin>' + contents.join("\n").replaceAll(/<\/?plugin[^>]*>/, "") + '</plugin>'
        } else {
            jos << contents.join("\n")
        }
        jos.closeEntry()
    }
}

void copyEntry(JarInputStream jis, JarOutputStream jos, JarEntry entry) {
    if(entry.name && !entries[entry.name]) {
        jos.putNextEntry(entry)
        if(!entry.isDirectory()) {
            int bytesRead = 0;
            while (-1 != (bytesRead = jis.read(BUFFER))) {
                jos.write(BUFFER, 0, bytesRead);
            }
        }
        entries[entry.name] = true
        jos.closeEntry()
    } else if(!entry.isDirectory()){
        grailsConsole.updateStatus "Duplicate dropped $entry.name"
    }
}

void moveEntry(JarInputStream jis, JarOutputStream jos, String name) {
    if(name && !entries[name]) {
        JarEntry entry = new JarEntry(name)
        jos.putNextEntry(entry)
        if(!entry.isDirectory()) {
            int bytesRead = 0;
            while (-1 != (bytesRead = jis.read(BUFFER))) {
                jos.write(BUFFER, 0, bytesRead);
            }
        }
        entries[entry.name] = true
        jos.closeEntry()
    } else if(!entry.isDirectory()){
        grailsConsole.updateStatus "Duplicate dropped $name"
    }
}

ByteArrayOutputStream getEntryAsOutputStream(JarInputStream jis, JarEntry entry) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(entry.size > 0 ? entry.size as int: 1024)
    int bytesRead = 0;
    while (-1 != (bytesRead = jis.read(BUFFER))) {
        baos.write(BUFFER, 0, bytesRead);
    }
    return baos
}

void walkJar(JarInputStream jis, JarOutputStream jos) {
    JarEntry entry
    while( (entry = jis.getNextJarEntry()) ) {
        String name = entry?.name
        
        if( entry.isDirectory()) {
            copyEntry(jis, jos, entry)
        } else if( name.endsWith(".jar") ) {
            flattenJarInEntry(jis, jos, entry)
        } else if ( name =~ /META-INF\/(.*(\.SF|\.RSA|\.DSA|\.INF)|MANIFEST.MF)/ ) {
            
            // Drop these
            grailsConsole.updateStatus "Dropped $name"
        
        } else if ( name == 'META-INF/grails-plugin.xml' ||
                    name.startsWith('META-INF/services/') || 
                    name =~ /META-INF\/spring\.(handlers|schemas)/
        ) {
            
            // Group these and add last
            String serviceName = entry.name
            String serviceItems = getEntryAsOutputStream(jis, entry)?.toString()?.trim()
            if(serviceItems) {
                if(!services[serviceName]) {
                    services[serviceName] = []
                }
                services[serviceName].add(serviceItems)
            }
        } else if ( name.startsWith("WEB-INF/classes/") ) {
        
            // Move these to root
            moveEntry(jis, jos, entry.name.replaceAll("WEB-INF/classes/", ""))
        
        } else {
            copyEntry(jis, jos, entry)
        }
    }
}

void flattenJarInEntry(JarInputStream jis, JarOutputStream jos, JarEntry entry) {
    ByteArrayInputStream bis
    if(entry.name.contains("grails-plugin-i18n") && i18nPatch) {
        println "Patching i18n plugin"
        // patch i18n
        bis = new ByteArrayInputStream(new File("scripts/i18n_patch/grails-plugin-i18n-2.1.1.jar").bytes)
    } else {
        bis = new ByteArrayInputStream(getEntryAsOutputStream(jis, entry).toByteArray())
    }
    JarInputStream innerJis = new JarInputStream(bis)
    walkJar(innerJis, jos)
}