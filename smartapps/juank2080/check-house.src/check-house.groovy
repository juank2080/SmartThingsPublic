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
        input "checkDoorInterval", "number", title: "Check door each: (min)", required: true, multiple: false
        input "closeDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: true, multiple: false
	}
    section("Go Bed Home Routine - Lights:") {
        input "lightsIndoorRef", "capability.light", multiple: true, title: "Indoor Lights as Refertence"
        input "lightsOutdoorRef", "capability.light", multiple: true, title: "Outside Lights to check"
        input "timeOfDayLights", "time", title: "Time?"
        input "checkInterval", "number", title: "Check in between (min)", required: true, multiple: false
        input "maxAttempts", "number", title: "Max Attempts", required: true, multiple: false
    }
    section("Go Bed Home Routine - Locks:") {
        input "doorLocks", "capability.lock", title: "Locks to check?", required: true, multiple: true
        input "timeOfDayLocks", "time", title: "Time?"
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
  
	// My App event subscriptions  
	subscribe(sideDoorLock, "lock.unlocked", doorLockOnUnlocked)
	subscribe(sideDoorSensor, "contact.closed", contactSensorOnClose)
    
    // Creating schedulers using timezone 
	def start = timeToday(timeOfDayLights, location?.timeZone)
	schedule(start, checkLights);
    
    start = timeToday(timeOfDayLocks, location?.timeZone)
	schedule(start, checkLocks);
    
    // Check if door is currently "unlocked"
    def currentValue = sideDoorLock.currentValue("lock")
    log.debug "Current values: Door (${sideDoorLock.currentLock}), Contact: (${sideDoorSensor.currentContact})"
    if (sideDoorLock.currentLock != "locked" && sideDoorSensor.currentContact == "closed") {
    	log.debug "Setting timer to check door again in ${minInterval} min"
    	runIn(60 * checkDoorInterval, checkSideDoorSensor);
    }
    
    // Init counter
    state.lightAttempt = 0
}

/**
* START EVENT HANDLERS
*/ 
def contactSensorOnClose(evt){
	log.debug "[contactSensorOnClose] checking in ${checkDoorInterval} min"
    
    log.debug "[contactSensorOnClose] Side door lock status in ${sideDoorLock.currentLock}"
	if (sideDoorLock.currentLock != "locked") {
    	log.debug "[contactSensorOnClose] Removing scheduler checkSideDoorSensor"
    	unschedule(checkSideDoorSensor)
        log.debug "[contactSensorOnClose] Closing door in ${closeDoorDelayAfterSensorClosed} sec"
    	runIn(closeDoorDelayAfterSensorClosed, closeSideDoor)
    }
}

def closeSideDoor(){
	sideDoorLock.lock()
}

def doorLockOnUnlocked(evt) {
	log.debug "[doorLockOnUnlocked] checking in ${checkDoorInterval} min"
	runIn(60 * checkDoorInterval, checkSideDoorSensor);
}
/*** END EVENT HANDLERS ***/

def checkSideDoorSensor() {
	log.debug "[checkSideDoorSensor] The current value of sideDoorSensor is ${sideDoorSensor.currentContact}"
    
	if (sideDoorSensor.currentContact == "closed") {
		log.debug "[checkSideDoorSensor] Locking side door"
		sideDoorLock.lock()
	} else {
    	log.debug "[checkSideDoorSensor] Executing checkSideDoorSensor in ${checkDoorInterval} min"
    	runIn(60 * checkDoorInterval, checkSideDoorSensor, [overwrite: false]);
  	}
}

def checkLocks(){
	log.debug "[checkLocks] Locking doors..."
    
	doorLocks.findAll { lock -> lock.currentLock != "locked" }?.each { lock -> lock.lock() }
}

def checkLights() {
	log.debug "[checkLights] Checking ..."
    
	if (lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }) {
    	log.debug "[checkLights] Found at least one outdoor light on"
        
        if (state.lightAttempt < maxAttempts && lightsIndoorRef?.any { light -> light.currentSwitch == "on" }) {
            state.lightAttempt = state.lightAttempt + 1
            log.debug "[checkLights] It is at least one light indoor on, check again in ${checkInterval}min, attempt: ${state.lightAttempt}"
            
			runIn(60*checkInterval, checkLights, [overwrite: false]);
      	} else {
        	state.lightAttempt = 0
            log.debug "[checkLights] Any indoor light is on, turning lights off"
            lightsOutdoorRef.findAll { light -> light.currentSwitch == "on" }?.each { light -> light.off() }
        }
        
    } else {
    	log.debug "[checkLights] No outside lights on - stop checking"
        state.lightAttempt = 0
    }
}