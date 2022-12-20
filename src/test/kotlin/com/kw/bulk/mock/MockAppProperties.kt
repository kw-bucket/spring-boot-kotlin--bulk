package com.kw.bulk.mock

import com.kw.bulk.config.properties.AppProperties
import com.kw.common.mail.config.properties.EmailProperties

object MockAppProperties {

    val emailConfig = AppProperties.EmailConfig(
        heartbeat = EmailProperties(subject = "", bodyTemplate = "heartbeat.html"),
        success = EmailProperties(subject = "", bodyTemplate = ""),
        failure = EmailProperties(subject = "", bodyTemplate = ""),
    )
}
