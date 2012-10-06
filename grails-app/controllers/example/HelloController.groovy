package example

class HelloController {

    def index(HelloCommand command) { 
        command.validate()
        return [
            command: command
        ]    
    }
}

class HelloCommand {
    static constraints = {
        name(blank:false, nullable:false)
    }
    String name
    
}
