package example

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

class JettyServer {

    static main(args) {
        Server server = new Server(8080)

        WebAppContext webapp = new WebAppContext()
        webapp.setContextPath("/")
        
        String base = "jar:${JettyServer.class.getProtectionDomain().getCodeSource().getLocation()}!/"
        println "Setting to $base"
        
        webapp.setWar(base) 
        webapp.setCopyWebDir(false)
        webapp.setCopyWebInf(false)
        webapp.setExtractWAR(false)
        
        server.setHandler(webapp)

        server.start()
        server.join()
    }

}