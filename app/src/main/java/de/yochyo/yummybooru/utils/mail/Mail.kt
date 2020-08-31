/*
package de.yochyo.yummybooru.utils.mail

import java.util.*
import javax.activation.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


class Mail() : Authenticator() {
    private val _port = "465"
    private val _sport = "465"
    private val _host = "smtp.gmail.com"

    private val _auth = true
    private val _debuggable = false
    private val _multipart = MimeMultipart()


    fun send(subject: String, body: String): Boolean {
        val props = _setProperties()
        return if (subject != "" && body != "") {
            val session = Session.getInstance(props, this)
            val msg = MimeMessage(session)
            msg.setFrom(InternetAddress(MailData.from))
            msg.setRecipients(MimeMessage.RecipientType.TO, arrayOf(InternetAddress(MailData.to)))
            msg.subject = subject
            msg.sentDate = Date()

            val messageBodyPart: BodyPart = MimeBodyPart()
            messageBodyPart.setText(body)
            _multipart.addBodyPart(messageBodyPart)

            // Put parts in message
            msg.setContent(_multipart)

            // send email
            Transport.send(msg)
            true
        } else {
            false
        }
    }

    @Throws(Exception::class)
    fun addAttachment(filename: String?) {
        val messageBodyPart: BodyPart = MimeBodyPart()
        val source: DataSource = FileDataSource(filename)
        messageBodyPart.dataHandler = DataHandler(source)
        messageBodyPart.fileName = filename
        _multipart.addBodyPart(messageBodyPart)
    }

    public override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(MailData.from, MailData.password)
    }

    private fun _setProperties(): Properties {
        val props = Properties()
        props["mail.smtp.host"] = _host
        if (_debuggable) {
            props["mail.debug"] = "true"
        }
        if (_auth) {
            props["mail.smtp.auth"] = "true"
        }
        props["mail.smtp.port"] = _port
        props["mail.smtp.socketFactory.port"] = _sport
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.socketFactory.fallback"] = "false"
        return props
    }

    init {
        val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
        CommandMap.setDefaultCommandMap(mc)
    }
}
 */