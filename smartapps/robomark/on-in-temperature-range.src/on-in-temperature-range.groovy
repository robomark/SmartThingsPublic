/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  On In Temperature Range
 * (modified from Its Too Cold)
 *
 *  Author: robomark
 */
definition(
    name: "On In Temperature Range",
    namespace: "robomark",
    author: "Mark Haseltine",
    description: "Monitor the temperature and when it moves in a range get a text and/or turn on a thing.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("Set min and max temperatures...") {
		input "mintemp", "number", title: "Min Temp?"
        input "maxtemp", "number", title: "Max Temp?"
	}
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
	section("Turn on a heater...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def localMax = settings.maxtemp
    def localMin = settings.mintemp
	def mySwitch = settings.switch1

	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if (evt.doubleValue <= localMax && evt.doubleValue >= localMin) {
		log.debug "Checking how long the temperature sensor has been reporting <= $localMax and >= $localMin"

		// right now, expect this to be triggered by weather station which only updates hourly, so don't need the throttling

		// Don't send a continuous stream of text messages
		//def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		//def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		//def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		//log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		//def alreadySentSms = recentEvents.count { it.doubleValue <= localMax && it.doubleValue >= localMin } > 1

		if (switch1.currentValue("switch") == "off") {
			log.debug "Temperature is between $localMax and $localMin:  sending SMS and activating $mySwitch"
			def tempScale = location.temperatureScale ?: "F"
			send("${temperatureSensor1.displayName} is in range, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
			switch1?.on()
		}
	}
    
    // turn off if outside range and switch is on
    if (evt.doubleValue > localMax || evt.doubleValue < localMin) {
		log.debug "Checking how long the temperature sensor has been reporting > $localMax or < $localMin"

		// Don't send a continuous stream of text messages
		//def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		//def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		//def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		//log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		//def alreadySentSms = recentEvents.count { it.doubleValue > localMax || it.doubleValue < localMin } > 1

		if (switch1.currentValue("switch") == "on") {
			log.debug "Temperature is outside $localMax and $localMin:  sending SMS and deactivating $mySwitch"
			def tempScale = location.temperatureScale ?: "F"
			send("${temperatureSensor1.displayName} is out of range, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
			switch1?.off()
		}
	}
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}