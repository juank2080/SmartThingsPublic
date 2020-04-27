/**
 *  Check House
 *
 *  Copyright 2020 Juan Romero
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
 */
definition(
    name: "Check House",
    namespace: "juank2080",
    author: "Juan Romero",
    description: "Check house lights and doors to be closed",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Side door check:") {
    	input "sideDoorSensor", "capability.contactSensor", title: "Side door sensor?", required: true, multiple: false
        input "sideDoorLock", "capability.lock", title: "Side door lock?", required: true, multiple: false
        input "minInterval", "number", title: "Check in between (min)", required: true, multiple: false
	}
    section("Outdoor lights check:") {
        input "lightsIndoorRef", "capability.light", multiple: true, title: "Indoor Lights as Refertence"
        input "lightsOutdoorRef", "capability.light", multiple: true, title: "Outside Lights to check"
        input "timeOfDay", "time", title: "Time?"
        input "checkInterval", "number", title: "Check in between (min)", required: true, multiple: false
        input "maxAttempts", "number", title: "Max Attempts", required: true, multiple: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
    
	initialize()
}

def initialize() {
	log.debug "initialize"
    
    log.debug "minInterval ${minInterval}"
    
    subscribe(sideDoorLock, "lock.unlocked", startCheckingSideSensor)
    def start = timeToday(timeOfDay, location?.timeZone)
    log.debug "Local time ${start} or ${timeOfDay}"
    schedule(start, checkLights);
    
    def currentValue = sideDoorLock.currentValue("lock")
    log.debug "Current lock status ${currentValue}"
    if (currentValue != "locked") {
    	runIn(60*minInterval, checkSideDoorSensor);
    }
    
    state.lightAttempt = 0
}

def startCheckingSideSensor(evt) {
	log.debug "startCheckingSideSensor ${evt}"
	runIn(60*minInterval, checkSideDoorSensor);
}

def checkSideDoorSensor() {
	def currentValue = sideDoorSensor.currentValue("contact")
    log.debug "the current value of sideDoorSensor is ${currentValue}"
    
	if (currentValue == "closed") {
    	log.debug "Locking side door"
		sideDoorLock.lock()
	} else {
    	runIn(60*minInterval, checkSideDoorSensor, [overwrite: false]);
        log.debug "Executing in ${minInterval}min"
  	}
}

def checkLights() {
	log.debug "checkStatus"

	def outdoorLightsStatus = lightsOutdoorRef.currentSwitch
    
    log.debug "outdoorLightsStatus ${outdoorLightsStatus.size()}"
    
	if (outdoorLightsStatus?.any { switchVal -> switchVal == "on" ? true : false }) {
    	log.debug "Found at least one outdoor light on"
        
        def indoorLightsStatus = lightsIndoorRef.currentSwitch
        if (state.lightAttempt < maxAttempts && indoorLightsStatus?.any { switchVal -> switchVal == "on" }) {
            state.lightAttempt = state.lightAttempt + 1
            log.debug "It is at least one light indoor on, check again in ${checkInterval}min, attempt: ${state.lightAttempt}"            
			runIn(60*checkInterval, checkLights, [overwrite: false]);
      	} else {
        	state.lightAttempt = 0
            log.debug "Any indoor light is on, turning lights off"
            lightsOutdoorRef.findAll { light -> light.currentSwitch == "on" }?.each { light -> light.off() }
        }
        
    } else {
    	log.debug "Any light on - sleeping"
        state.lightAttempt = 0
    }
}