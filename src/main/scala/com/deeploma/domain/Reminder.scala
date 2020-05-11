package com.deeploma.domain

import java.util.{Date, UUID}

case class Reminder(id: UUID,
                    userId: UUID,
                    text: String,
                    time: Date,
                    wasSent: Boolean = false,
                    wasConfirmed: Boolean = true)
