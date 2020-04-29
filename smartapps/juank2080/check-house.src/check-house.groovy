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
	page(name: "pageOne", nextPage: "pageTwo", uninstall: true)
	page(name: "pageTwo", nextPage: "pageThree", uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks-active.png",
                      title: "Locks",
                      required: true,
                      "Configuration routines and checks for house locks"
		}
        section("Front door check:") {
            input "frontDoorSensor", "capability.contactSensor", title: "Front door sensor?", required: false, multiple: false
            input "frontDoorLock", "capability.lock", title: "Front door lock?", required: false, multiple: false
            input "checkFrontDoorInterval", "number", title: "Check door each: (min)", required: false, multiple: false
            input "closeFrontDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: false, multiple: false
        }
        section("Side door check:") {
            input "sideDoorSensor", "capability.contactSensor", title: "Side door sensor?", required: false, multiple: false
            input "sideDoorLock", "capability.lock", title: "Side door lock?", required: false, multiple: false
            input "checkSideDoorInterval", "number", title: "Check door each: (min)", required: false, multiple: false
            input "closeSideDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: false, multiple: false
        }
    }
    page(name: "pageThree", install: true, uninstall: true) {
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
}

def pageOne() {
    dynamicPage(name: "pageOne", nextPage: "pageTwo", uninstall: true) {
        section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                      title: "Runtime Information",
                      required: true,
                      "Shows some statistics of the app."
		}
        section("Outside lights:") {
        	paragraph "Outside lights on: ${lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }? "Yes" : "No"}"
			paragraph "Outside lights attempt: ${state?.lightAttempt != null? state.lightAttempt : "NA"}"
            paragraph "Outside lights shut at: ${state?.outsideLightsShutAt != null? formateTime(state.outsideLightsShutAt) : "NA"}"
            paragraph "Outside lights check status: ${state?.outsideLightsCheckStatus != null? state.outsideLightsCheckStatus : "NA"}"
        }
        section("Front door:") {
            paragraph "Front door closed by: ${state?.frontDoorClosedBy != null? state.frontDoorClosedBy : "NA"}"
            paragraph "Front door closed by at: ${state?.frontDoorClosedByAt != null? formateTime(state.frontDoorClosedByAt) : "NA"}"
            paragraph "Front door check status: ${state?.frontDoorCheckStatus != null? state?.frontDoorCheckStatus : "NA"}"
        }
        section("Side door:") {
            paragraph "Side door closed by: ${state?.sideDoorClosedBy != null? state.sideDoorClosedBy : "NA"}"
            paragraph "Side door closed by at: ${state?.sideDoorClosedByAt != null? formateTime(state.sideDoorClosedByAt) : "NA"}"
            paragraph "Side door check status: ${state?.sideDoorCheckStatus != null? state?.sideDoorCheckStatus : "NA"}"
		}
        section("General:") {
        	paragraph "Go bed locks routine: ${state?.goBedLocksStatus != null? state?.goBedLocksStatus : "NA"}"
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
    
    // Init counters
    state.lightAttempt = 0
    
    state.sideDoorClosedBy = "-"
    state.sideDoorClosedByAt = null
    state.sideDoorCheckStatus = "-"
    
    state.frontDoorClosedBy = "-"
    state.frontDoorClosedByAt = null
    state.frontDoorCheckStatus = "-"
    
    state.outsideLightsShutAt = null
    state.outsideLightsCheckStatus = "-"
    
    state.goBedLocksStatus = null
  
	// My App event subscriptions
    if (frontDoorLock != null) {
        subscribe(frontDoorLock, "lock.locked", frontDoorLockOnlock)
        subscribe(frontDoorLock, "lock.unlocked", frontDoorLockOnUnlocked)
        subscribe(frontDoorSensor, "contact.closed", contactFrontDoorSensorOnClose)
        
        // Check if door is currently "unlocked"
        log.debug "Current values: Door (${frontDoorLock.currentLock}), Contact: (${frontDoorSensor.currentContact})"
        if (frontDoorLock.currentLock != "locked" && frontDoorSensor.currentContact == "closed") {
            log.debug "Setting timer to check front door again in ${checkFrontDoorInterval} min"

            state.frontDoorClosedBy = null
            state.frontDoorClosedByAt = null
            state.frontDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Front door is currently opened, checking sensor in ${checkFrontDoorInterval} min"
            sendEvent(name: "status", value: "updated")

            runIn(60 * checkFrontDoorInterval, checkFrontDoorSensor);
        } 
  	}
    if (sideDoorLock != null) {
        subscribe(sideDoorLock, "lock.locked", sideDoorLockOnlock)
        subscribe(sideDoorLock, "lock.unlocked", sideDoorLockOnUnlocked)
        subscribe(sideDoorSensor, "contact.closed", contactSideDoorSensorOnClose)
        
        // Check if door is currently "unlocked"
        log.debug "Current values: Door (${sideDoorLock.currentLock}), Contact: (${sideDoorSensor.currentContact})"
        if (sideDoorLock.currentLock != "locked" && sideDoorSensor.currentContact == "closed") {
            log.debug "Setting timer to check side door again in ${checkSideDoorInterval} min"

            state.sideDoorClosedBy = null
            state.sideDoorClosedByAt = null
            state.sideDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Side door is currently opened, checking sensor in ${checkSideDoorInterval} min"
            sendEvent(name: "status", value: "updated")

            runIn(60 * checkSideDoorInterval, checkSideDoorSensor);
        } 
  	}
    
    // Creating schedulers using timezone 
	def start = timeToday(timeOfDayLights, location?.timeZone)
	schedule(start, checkLights);
    
    start = timeToday(timeOfDayLocks, location?.timeZone)
	schedule(start, checkLocks);
}

/**
* START EVENT HANDLERS
*/ 
// Side door
def contactSideDoorSensorOnClose(evt){
	def method = "contactSideDoorSensorOnClose"
    
	log.debug "[${method}] checking in ${checkSideDoorInterval} min"
	log.debug "[${method}] Side door lock status in ${sideDoorLock.currentLock}"
    
	if (sideDoorLock.currentLock != "locked") {
    	log.debug "[${method}] Removing scheduler checkSideDoorSensor"
        log.debug "[${method}] Closing door in ${closeSideDoorDelayAfterSensorClosed} sec"
        
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
	}
}

// Front door
def contactFrontDoorSensorOnClose(evt){
	def method = "contactFrontDoorSensorOnClose"
    
	log.debug "[${method}] checking in ${checkFrontDoorInterval} min"
	log.debug "[${method}] Front door lock status in ${frontDoorLock.currentLock}"
    
	if (frontDoorLock.currentLock != "locked") {
    	log.debug "[${method}] Removing scheduler checkFrontDoorSensor"
        log.debug "[${method}] Closing door in ${closeFrontDoorDelayAfterSensorClosed} sec"
        
    	unschedule(checkFrontDoorSensor)
        
        state.frontDoorClosedBy = null
    	state.frontDoorClosedByAt = null
        state.frontDoorCheckStatus = "Front door sensor closed, door will be closed in ${closeFrontDoorDelayAfterSensorClosed} sec"
        sendEvent(name: "status", value: "updated")
        
    	runIn(closeFrontDoorDelayAfterSensorClosed, closeFrontDoor)
    }
}

def frontDoorLockOnUnlocked(evt) {
	def method = "frontDoorLockOnUnlocked"
	log.debug "[${method}] checking in ${checkFrontDoorInterval} min"
    
    state.frontDoorClosedBy = null
    state.frontDoorClosedByAt = null
    state.frontDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Front door has been opened, checking sensor in ${checkFrontDoorInterval} min"
    sendEvent(name: "status", value: "updated")
    
	runIn(60 * checkFrontDoorInterval, checkFrontDoorSensor);
}

def frontDoorLockOnlock(evt){
	def method = "frontDoorLockOnlock"
	log.debug "[${method}] Front door sensor status ${frontDoorSensor.currentContact}"
    
	if (frontDoorSensor.currentContact == "closed") {
    	log.debug "[${method}] Front door is closed, removing scheduler checkFrontDoorSensor"
		unschedule(checkFrontDoorSensor)
	}
}
/*** END EVENT HANDLERS ***/

/**
* DOOR METHODS
*/
// Side door
def closeSideDoor(){
	state.sideDoorClosedBy = "[contactSideDoorSensorOnClose]"
    state.sideDoorClosedByAt = now()
    state.sideDoorCheckStatus = "Door is closed"
    sendEvent(name: "status", value: "updated")
    
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
        sendEvent(name: "status", value: "updated")
        
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
// Front door
def closeFrontDoor(){
	state.frontDoorClosedBy = "[contactFrontDoorSensorOnClose]"
    state.frontDoorClosedByAt = now()
    state.frontDoorCheckStatus = "Door is closed"
    sendEvent(name: "status", value: "updated")
    
	frontDoorLock.lock()
}

def checkFrontDoorSensor() {
	def method = "checkFrontDoorSensor"
	log.debug "[${method}] The current value of frontDoorSensor is ${frontDoorSensor.currentContact}"
    
	if (frontDoorSensor.currentContact == "closed") {
		log.debug "[${method}] Locking side door"
        
        state.frontDoorClosedBy = "[${method}]"
        state.frontDoorClosedByAt = now()
        state.frontDoorCheckStatus = "Door is closed"
        sendEvent(name: "status", value: "updated")
        
		frontDoorLock.lock()
	} else if (frontDoorLock.currentLock != "locked") {
    	log.debug "[${method}] Executing checkFrontDoorSensor in ${checkFrontDoorInterval} min"
        
        state.frontDoorClosedBy = null
        state.frontDoorClosedByAt = null
        state.frontDoorCheckStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Sensor is still opened, checking sensor in ${checkFrontDoorInterval} min"
        sendEvent(name: "status", value: "updated")
        
    	runIn(60 * checkFrontDoorInterval, checkFrontDoorSensor, [overwrite: false]);
  	} else {
    	state.frontDoorClosedBy = null
        state.frontDoorClosedByAt = now()
        state.frontDoorCheckStatus = "Door is closed"
        sendEvent(name: "status", value: "updated")
    }
}

// Go bed procedure - Locks
def checkLocks(){
	def method = "checkLocks"
	log.debug "[${method}] Locking doors..."
    
    if (doorLocks?.any { lock -> lock.currentLock != "locked" }) {
    	doorLocks.findAll { lock -> lock.currentLock != "locked" }?.each { lock -> lock.lock() }
    	state.goBedLocksStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] Doors were closed (${doorLocks.size()})"
    } else {
    	state.goBedLocksStatus = "[${new Date(now()).format("hh:mm a", location.timeZone)}] All doors were already closed (${doorLocks.size()})"
    }
    
    sendEvent(name: "status", value: "updated")
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