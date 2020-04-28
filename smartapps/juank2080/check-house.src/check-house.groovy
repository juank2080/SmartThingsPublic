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
	page(name: "pageOne", nextPage: "pageTwo", uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks-active.png",
                      title: "Locks",
                      required: true,
                      "Configuration routines and checks for house locks"
		}
        section("Side door check:") {
            input "sideDoorSensor", "capability.contactSensor", title: "Side door sensor?", required: true, multiple: false
            input "sideDoorLock", "capability.lock", title: "Side door lock?", required: true, multiple: false
            input "checkSideDoorInterval", "number", title: "Check door each: (min)", required: true, multiple: false
            input "closeSideDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: true, multiple: false
        }
    }
    page(name: "pageTwo", nextPage: "pageThree", uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png",
                      title: "Go Bed Routines",
                      required: true,
                      "Routines for go bed"
		}
        section("Lights:") {
            input "lightsIndoorRef", "capability.light", multiple: true, title: "Indoor Lights as Reference"
            input "lightsOutdoorRef", "capability.light", multiple: true, title: "Outside Lights to check"
            input "timeOfDayLights", "time", title: "Time?"
            input "checkInterval", "number", title: "Check in between (min)", required: true, multiple: false
            input "maxAttempts", "number", title: "Max Attempts", required: true, multiple: false
        }
        section("Locks:") {
            input "doorLocks", "capability.lock", title: "Locks to check?", required: true, multiple: true
            input "timeOfDayLocks", "time", title: "Time?"
        }
	}
    page(name: "pageThree", install: true, uninstall: true)
}

def pageThree() {
    dynamicPage(name: "pageThree", install: true) {
        section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                      title: "Runtime Information",
                      required: true,
                      "Shows some statistics of the app."
		}
        section {
        	paragraph "Outside lights on: ${lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }? "Yes" : "No"}"
			paragraph "Outside lights attempt: ${state?.lightAttempt != null? state.lightAttempt : "NA"}"
            paragraph "Outside lights shut at: ${state?.outsideLightsShutAt != null? formateTime(state.outsideLightsShutAt) : "NA"}"
            paragraph "Outside lights check status: ${state?.outsideLightsCheckStatus != null? state.outsideLightsCheckStatus : "NA"}"
            paragraph "Side door closed by: ${state?.sideDoorClosedBy != null? state.sideDoorClosedBy : "NA"}"
            paragraph "Side door closed by at: ${state?.sideDoorClosedByAt != null? formateTime(state.sideDoorClosedByAt) : "NA"}"
            paragraph "Side door check status: ${state?.sideDoorCheckStatus != null? state?.sideDoorCheckStatus : "NA"}"
		}
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
    subscribe(sideDoorLock, "lock.locked", sideDoorLockOnlock)
	subscribe(sideDoorLock, "lock.unlocked", sideDoorLockOnUnlocked)
	subscribe(sideDoorSensor, "contact.closed", contactSideDoorSensorOnClose)
    
    // Creating schedulers using timezone 
	def start = timeToday(timeOfDayLights, location?.timeZone)
	schedule(start, checkLights);
    
    start = timeToday(timeOfDayLocks, location?.timeZone)
	schedule(start, checkLocks);
    
    // Check if door is currently "unlocked"
    log.debug "Current values: Door (${sideDoorLock.currentLock}), Contact: (${sideDoorSensor.currentContact})"
    if (sideDoorLock.currentLock != "locked" && sideDoorSensor.currentContact == "closed") {
    	log.debug "Setting timer to check door again in ${checkSideDoorInterval} min"
        
        state.sideDoorClosedBy = null
    	state.sideDoorClosedByAt = null
    	state.sideDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Door is currently opened, checking sensor in ${checkSideDoorInterval} min"
        
    	runIn(60 * checkSideDoorInterval, checkSideDoorSensor);
    }
    
    // Init counter
    state.lightAttempt = 0
    state.sideDoorClosedBy = "-"
    state.sideDoorClosedByAt = null
    state.sideDoorCheckStatus = "-"
    state.outsideLightsShutAt = null
    state.outsideLightsCheckStatus = "-"
}

/**
* START EVENT HANDLERS
*/ 
def contactSideDoorSensorOnClose(evt){
	def method = "contactSideDoorSensorOnClose"
    
	log.debug "[${method}] checking in ${checkSideDoorInterval} min"
	log.debug "[${method}] Side door lock status in ${sideDoorLock.currentLock}"
    
	if (sideDoorLock.currentLock != "locked") {
    	log.debug "[${method}] Removing scheduler checkSideDoorSensor"
        log.debug "[${method}] Closing door in ${closeDoorDelayAfterSensorClosed} sec"
        
    	unschedule(checkSideDoorSensor)
        
        state.sideDoorClosedBy = null
    	state.sideDoorClosedByAt = null
        state.sideDoorCheckStatus = "Side door sensor closed, door will be closed in ${closeSideDoorDelayAfterSensorClosed} sec"
        sendEvent(name: "status", value: "updated")
        
    	runIn(closeSideDoorDelayAfterSensorClosed, closeSideDoor)
    }
}

def sideDoorLockOnUnlocked(evt) {
	def method = "sideDoorLockOnUnlocked"
	log.debug "[${method}] checking in ${checkSideDoorInterval} min"
    
    state.sideDoorClosedBy = null
    state.sideDoorClosedByAt = null
    state.sideDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Side door has been opened, checking sensor in ${checkSideDoorInterval} min"
    sendEvent(name: "status", value: "updated")
    
	runIn(60 * checkSideDoorInterval, checkSideDoorSensor);
}

def sideDoorLockOnlock(evt){
	def method = "sideDoorLockOnlock"
	log.debug "[${method}] Side door sensor status ${sideDoorSensor.currentContact}"
    
	if (sideDoorSensor.currentContact == "closed") {
    	log.debug "[${method}] Side door is closed, removing scheduler checkSideDoorSensor"
		unschedule(checkSideDoorSensor)
        
        state.sideDoorCheckStatus = "Door is closed"
        sendEvent(name: "status", value: "updated")
	}
}
/*** END EVENT HANDLERS ***/

/**
* DOOR METHODS
*/
def closeSideDoor(){
	state.sideDoorClosedBy = "[contactSideDoorSensorOnClose]"
    state.sideDoorClosedByAt = now()
    state.sideDoorCheckStatus = "Door is closed"
    
	sideDoorLock.lock()
}

def checkSideDoorSensor() {
	def method = "checkSideDoorSensor"
	log.debug "[${method}] The current value of sideDoorSensor is ${sideDoorSensor.currentContact}"
    
	if (sideDoorSensor.currentContact == "closed") {
		log.debug "[${method}] Locking side door"
        
        state.sideDoorClosedBy = "[${method}]"
        state.sideDoorClosedByAt = now()
        state.sideDoorCheckStatus = "Door is closed"
        
		sideDoorLock.lock()
	} else if (sideDoorLock.currentLock != "locked") {
    	log.debug "[${method}] Executing checkSideDoorSensor in ${checkSideDoorInterval} min"
        
        state.sideDoorClosedBy = null
        state.sideDoorClosedByAt = null
        state.sideDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Sensor is still opened, checking sensor in ${checkSideDoorInterval} min"
        sendEvent(name: "status", value: "updated")
        
    	runIn(60 * checkSideDoorInterval, checkSideDoorSensor, [overwrite: false]);
  	} else {
    	state.sideDoorClosedBy = null
        state.sideDoorClosedByAt = now()
        state.sideDoorCheckStatus = "Door is closed"
        sendEvent(name: "status", value: "updated")
    }
}

def checkLocks(){
	def method = "checkLocks"
	log.debug "[${method}] Locking doors..."
    
    state.sideDoorClosedBy = "[${method}]"
    state.sideDoorClosedByAt = now()
    state.sideDoorCheckStatus = "Doors closed"
    sendEvent(name: "status", value: "updated")
    
	doorLocks.findAll { lock -> lock.currentLock != "locked" }?.each { lock -> lock.lock() }
}
/**
* END DOOR METHODS
*/

/**
* LIGHTS METHODS
*/
def checkLights() {
	def method = "checkLights"
	log.debug "[${method}] Checking ..."
    
	if (lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }) {
    	log.debug "[${method}] Found at least one outdoor light on"
        if (state?.lightAttempt == null) {
        	state.lightAttempt = 0
      	}
            
        if (lightsIndoorRef?.any { light -> light.currentSwitch == "on" }) {
        	if (state.lightAttempt < maxAttempts) {
                state.lightAttempt = state.lightAttempt + 1
                log.debug "[${method}] It is at least one light indoor on, check again in ${checkInterval}min, attempt: ${state.lightAttempt}"

                state.outsideLightsCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Indoor lights still on, cheking in ${checkInterval} min"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")

                runIn(60*checkInterval, checkLights, [overwrite: false]);
        	} else {
            	log.debug "[${method}] Max attempts reached, indoor lights still on, no more attempts will be done, outdoor lights remain on"
            
            	state.outsideLightsCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Indoor lights still on, no more attempts will be done, outdoor lights remain on"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")
            }
      	} else {
        	state.lightAttempt = 0
            log.debug "[${method}] Any indoor light is on, turning lights off"
            
    		state.outsideLightsCheckStatus = "Outdoor lights off"
            state.outsideLightsShutAt = now()
            sendEvent(name: "status", value: "updated")
            
            lightsOutdoorRef.findAll { light -> light.currentSwitch == "on" }?.each { light -> light.off() }
        }
        
    } else {
    	log.debug "[${method}] No outside lights on - stop checking"
        state.lightAttempt = 0
        
        state.outsideLightsShut = "Outside lights are off"
    	state.outsideLightsShutAt = now()
        sendEvent(name: "status", value: "updated")
    }
}
/**
* END LIGHTS METHODS
*/

def formateTime(java.lang.Long time) {
	return new Date(time).format("hh:mm a", location.timeZone);
}