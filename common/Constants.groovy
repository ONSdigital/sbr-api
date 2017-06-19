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

def test () {
    echo 'Hello Seattle'
}

def getSender () {
    return SENDER_ADDRESS;
}

def getEmailStatus () {
    return EMAIL_SERVICE_ENABLED
}




return this;