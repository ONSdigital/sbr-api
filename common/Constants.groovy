//class Constants {
//    static final SENDER_ADDRESS2 = "yolo hello"
////    static final SENDER_ADDRESS = envConf.getBoolean("alert.email.sender")
////    static final REPLY_ADDRESS = envConf.getBoolean("alert.email.reply")
////    static final RECIPIENT_ADDRESS = envConf.getBoolean("alert.email.recipient")
//
////    def test () {
////        return 'Hello Seattle'
////    }
//    def getSender2 () {
//        return SENDER_ADDRESS2;
//    }
//
//}

SENDER_ADDRESS = "<SENDER_ADDRESS>"
REPLY_ADDRESS = "<REPLY_ADDRESS>"
RECIPIENT_ADDRESS = "<RECIPIENT_ADDRESS>"
EMAIL_SERVICE_ENABLED = false

def getSender () {
    return SENDER_ADDRESS;
}

def getEmailStatus () {
    return EMAIL_SERVICE_ENABLED
}

def getRecipient () {
    return RECIPIENT_ADDRESS
}

def getReplyAddress () {
    return REPLY_ADDRESS
}



/*
* @method colourText(level,text)
*
* @description This method will wrap any input text inside
* ANSI colour codes.
*
* @param {String} level - The logging level (warn/info)
* @param {String} text - The text to wrap inside the colour
*
*/
def colourText(level,text){
    wrap([$class: 'AnsiColorBuildWrapper']) {
        // This method wraps input text in ANSI colour
        // Pass in a level, e.g. info or warning
        def code = getLevelCode(level)
        echo "${code[0]}${text}${code[1]}"
    }
}


/*
* @method getLevelCode(level)
*
* @description This method is called with a log level and
* will return a list with the start and end ANSI codes for
* the log level colour.
*
* @param {String} level - The logging level (warn/info)
*
* @return {List} colourCode - [start ANSI code, end ANSI code]
*
*/
def getLevelCode(level) {
    def colourCode
    switch (level) {
        case "info":
            // Blue
            colourCode = ['\u001B[33m','\u001B[0m']
            break
        case "warn":
            // Red
            colourCode = ['\u001B[31m','\u001B[0m']
            break
        default:
            colourCode = ['\u001B[34m','\u001B[0m']
            break
    }
    colourCode
}


return this;