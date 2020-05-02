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
        subscribe(frontDoorLock, "lock.locked", onLockLock)
        subscribe(frontDoorLock, "lock.unlocked", onLockUnlock)
        subscribe(frontDoorSensor, "contact.closed", onSensorClose)
        
        // Check if door is currently "unlocked"
        log.debug "Current values: Door (${frontDoorLock.currentLock}), Contact: (${frontDoorSensor.currentContact})"
        if (frontDoorLock.currentLock != "locked" && frontDoorSensor.currentContact == "closed") {
            log.debug "Setting timer to check front door again in ${checkFrontDoorInterval} min"

            state.frontDoorClosedBy = null
            state.frontDoorClosedByAt = null
            state.frontDoorCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Front door is currently opened, checking sensor in ${checkFrontDoorInterval} min"
            sendEvent(name: "status", value: "updated")

            runIn(60 * checkFrontDoorInterval, checkFrontDoorSensor, [overwrite: false, data: [deviceObj: frontDoorSensor]]);
        } 
  	}
    if (sideDoorLock != null) {
        subscribe(sideDoorLock, "lock.locked", onLockLock)
        subscribe(sideDoorLock, "lock.unlocked", onLockUnlock)
        subscribe(sideDoorSensor, "contact.closed", onSensorClose)
        
        // Check if door is currently "unlocked"
        log.debug "Current values: Door (${sideDoorLock.currentLock}), Contact: (${sideDoorSensor.currentContact})"
        if (sideDoorLock.currentLock != "locked" && sideDoorSensor.currentContact == "closed") {
            log.debug "Setting timer to check side door again in ${checkSideDoorInterval} min"

            state.sideDoorClosedBy = null
            state.sideDoorClosedByAt = null
            state.sideDoorCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Side door is currently opened, checking sensor in ${checkSideDoorInterval} min"
            sendEvent(name: "status", value: "updated")

            runIn(60 * checkSideDoorInterval, checkSideDoorSensor, [overwrite: false, data: [deviceObj: sideDoorSensor]]);
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
def onSensorClose(evt){
	def method = "onSensorClose"
    
    if (evt.device != null) {
    	def relatedElements = getDataRelatedByLockId(evt.device.id)
        
        log.debug "[${method}] checking in ${relatedElements.interval} min"
        log.debug "[${method}] Side door lock status in ${relatedElements.lock.currentLock}"

        if (relatedElements.lock.currentLock != "locked") {
            log.debug "[${method}] Removing scheduler ${relatedElements.callbackFunction}"
            log.debug "[${method}] Closing door in ${relatedElements.closeDelay} sec"

            unschedule(relatedElements.callbackFunction)

            writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                       "closedByAt":null,
                                                       "checkStatus":"Side door sensor closed, door will be closed in ${relatedElements.closeDelay} sec"])

            runIn(relatedElements.closeDelay, closeDoor, [overwrite: false, data: [deviceObj: relatedElements.lock]])
        }
    } else {
    	log.debug "[${method}] Device obj from Evnt is null"
    }
}

def onLockUnlock(evt) {
	def method = "onUnlock"
    
    if (evt.device != null) {
    	def relatedElements = getDataRelatedByLockId(evt.device.id)
    
    	if (relatedElements != null) {
            log.debug "[${method}] checking in ${relatedElements.interval} min"

			writeLogByLockId(evt.device.id, ["closedBy":null,
        									"closedByAt":null,
                                            "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${evt.device.label} has been opened, checking sensor in ${relatedElements.interval} min"])
   
            runIn(60 * relatedElements.interval, relatedElements.callbackFunction, [overwrite: false, data: [deviceObj: relatedElements.lock]]);
     	} else {
        	log.debug "[${method}] Couldn't get related elements from ${evt.device.label}"
        }
  	} else {
    	log.debug "[${method}] Device obj from Evnt is null"
    }
}

def onLockLock(evt){
	def method = "onLock"
    if (evt.device != null) {
    	def relatedElements = getDataRelatedByLockId(evt.device.id)
        
        if (relatedElements != null) {
            log.debug "[${method}] ${evt.device.label} sensor status ${relatedElements.sensor.currentContact}"

            if (relatedElements.sensor.currentContact == "closed") {
                log.debug "[${method}] ${evt.device.label} is closed, removing ${relatedElements.callbackFunction}"
                
                unschedule(relatedElements.callbackFunction)
            }
        } else {
        	log.debug "[${method}] Couldn't get related elements from ${evt.device.label}"
        }
    } else {
    	 log.debug "[${method}] Device obj from Evnt is null"
    }
}
/*** END EVENT HANDLERS ***/

/**
* DOOR METHODS
*/
// Side door
def closeDoor(data){
	def method = "closeDoor"
    
	if (data.deviceObj != null) {
    	def lockObj = findDeviceById(data.deviceObj.id)
        
        writeLogByLockId(data.deviceObj.id, ["closedBy":"[${data.closedBy}]",
        									"closedByAt":now(),
                                            "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Closing ${lockObj.label} now"])
        
        lockObj.lock()

        runIn(5, checkIfDoorClosed, [data: [tryCount: 1, deviceObj: lockObj]]);
  	} else {
    	log.debug "[${method}] Device obj from data is null"
    }
}

def checkIfDoorClosed(data) {
	def method = "closeDoor"
    
    if (data.deviceObj != null) {
    	def lockObj = findDeviceById(data.deviceObj.id)
        
        if (data.tryCount <= 3) {
            if (lockObj.currentLock != "locked") {
            	writeLogByLockId(data.deviceObj.id, ["closedBy":"[checkIfDoorClosed]",
                                                    "closedByAt":now(),
                                                    "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} didn't close, try again in 5s (${data.tryCount})"])

                sideDoorLock.lock()

                runIn(5, checkIfDoorClosed, [data: [tryCount: data.tryCount + 1, deviceObj: lockObj]]);
            } else {
            	writeLogByLockId(data.deviceObj.id, ["closedBy":"[checkIfDoorClosed]",
                                                    "closedByAt":now(),
                                                    "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} has been closed (${data.tryCount})"])
            }
        } else {
        	def message = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} didn't lock (!)"
            
        	writeLogByLockId(data.deviceObj.id, ["closedBy":"[contactSideDoorSensorOnClose]",
                                                    "closedByAt":now(),
                                                    "checkStatus":message])
                                                    
            sendPush(message)
        }
  	} else {
    	log.debug "[${method}] Device obj from data is null"
    }
}

//Wrappers
def checkSideDoorSensor(data) { checkDoorSensor(data) }
def checkFrontDoorSensor(data) { checkDoorSensor(data) }

def checkDoorSensor(data) {
	if (data?.deviceObj != null) {
        def method = "checkDoorSensor"
        
        def relatedElements = getDataRelatedBySensorId(data.deviceObj.id)
        
        log.debug "[${method}] The current value of ${relatedElements.lock.label} is ${relatedElements.sensor.currentContact}"
 
        if (relatedElements.sensor.currentContact == "closed") {
            log.debug "[${method}] Locking ${relatedElements.lock.label}"

            data.closedBy = "[${method}]"
			data.deviceObj = relatedElements.lock

            closeDoor(data)
        } else if (relatedElements.lock.currentLock != "locked") {
            log.debug "[${method}] Executing checkSideDoorSensor in ${relatedElements.interval} min"

			writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                        "closedByAt":null,
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${relatedElements.sensor.label} is still opened, checking sensor in ${relatedElements.interval} min"])
       
            runIn(60 * relatedElements.interval, relatedElements.callbackFunction, [overwrite: false, data: [deviceObj: relatedElements.sensor]]);
        } else {
        	writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                        "closedByAt":now(),
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${relatedElements.lock} Door is closed"])
        }
 	} else {
    	log.debug "[${method}] deviceObj is null (?)"
    }
}

// Go bed procedure - Locks
def checkLocks(){
	def method = "checkLocks"
	log.debug "[${method}] Locking doors..."
    
    if (doorLocks?.any { lock -> lock.currentLock != "locked" }) {
    	doorLocks.findAll { lock -> lock.currentLock != "locked" }?.each { lock -> lock.lock() }
    	state.goBedLocksStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Doors were closed (${doorLocks.size()})"
    } else {
    	state.goBedLocksStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] All doors were already closed (${doorLocks.size()})"
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

                state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Indoor lights still on, cheking in ${checkInterval} min"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")

                runIn(60*checkInterval, checkLights, [overwrite: false]);
        	} else {
            	log.debug "[${method}] Max attempts reached, indoor lights still on, no more attempts will be done, outdoor lights remain on"
            
            	state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Indoor lights still on, no more attempts will be done, outdoor lights remain on"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")
            }
      	} else {
        	state.lightAttempt = 0
            log.debug "[${method}] Any indoor light is on, turning lights off"
            
    		state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Outdoor lights off"
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
	return new Date(time).format("MM/dd hh:mm a", location.timeZone);
}

def findDeviceById(id) {
	def myAppDevices = [];
	for (setting in settings) {
    	try {
        	log.debug setting.id
        	myAppDevices = myAppDevices + setting.value
        } catch (Exception e) {}
	}
    
    return myAppDevices.find{ it.id == id }
}

def getDataRelatedByLockId(lockId) {
	def relatedElements = null
    
	if (frontDoorLock.id == lockId) {
    	relatedElements = ["lock":frontDoorLock,,
        					"sensor": frontDoorSensor,
        					"interval": checkFrontDoorInterval,
                            "closeDelay": closeFrontDoorDelayAfterSensorClosed,
                            "callbackFunction": checkFrontDoorSensor];
    } else if (sideDoorLock.id == lockId) {
    	relatedElements = ["lock":sideDoorLock,
        					"sensor":sideDoorSensor,
        					"interval":checkSideDoorInterval,
                            "closeDelay":closeSideDoorDelayAfterSensorClosed,
                            "callbackFunction":checkSideDoorSensor];
    }
    
    return relatedElements
}

def getDataRelatedBySensorId(sensorId) {
	def relatedElements = null
    
	if (frontDoorSensor.id == sensorId) {
    	relatedElements = ["lock":frontDoorLock,
        					"sensor": frontDoorSensor,
        					"interval":checkFrontDoorInterval,
                            "closeDelay":closeFrontDoorDelayAfterSensorClosed,
                            "callbackFunction": checkFrontDoorSensor];
    } else if (sideDoorSensor.id == sensorId) {
    	relatedElements = ["lock":sideDoorLock,
        					"sensor":sideDoorSensor,
        					"interval":checkSideDoorInterval,
                            "closeDelay":closeSideDoorDelayAfterSensorClosed,
                            "callbackFunction":checkSideDoorSensor];
    }
    
    return relatedElements
}

def writeLogByLockId(lockId, logData) {
	if (frontDoorLock.id == lockId) {
    	state.frontDoorClosedBy = logData.closedBy
       	state.frontDoorClosedByAt = logData.closedByAt
      	state.frontDoorCheckStatus = logData.checkStatus
    } else if (sideDoorLock.id == lockId) {
    	state.sideDoorClosedBy = logData.closedBy
       	state.sideDoorClosedByAt = logData.closedByAt
      	state.sideDoorCheckStatus = logData.checkStatus
    }
    
    sendEvent(name: "status", value: "updated")
}