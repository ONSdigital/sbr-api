class Constants {
    static final SENDER_ADDRESS = envConf.getBoolean("alert.email.sender")
    static final REPLY_ADDRESS = envConf.getBoolean("alert.email.reply")
    static final RECIPIENT_ADDRESS = envConf.getBoolean("alert.email.recipient")
}


return this;