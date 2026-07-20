local queueKey = KEYS[1]
local lastLeakKey = KEYS[2]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local leakRatePerSec = tonumber(ARGV[3])

local queueSizeStr = redis.call('GET', queueKey)
local lastLeakStr = redis.call('GET', lastLeakKey)

local currentQueueSize
local lastLeakTime

if queueSizeStr == false then
  currentQueueSize = 0
  lastLeakTime = now
else
  currentQueueSize = tonumber(queueSizeStr)
  lastLeakTime = tonumber(lastLeakStr)
  
  local elapsedMs = now - lastLeakTime
  local elapsedSec = elapsedMs / 1000.0
  
  local leaked = elapsedSec * leakRatePerSec
  
  currentQueueSize = math.max(0, currentQueueSize - leaked)
end

local allowed = 0
local newQueueSize = currentQueueSize

if currentQueueSize < capacity then
  allowed = 1
  newQueueSize = currentQueueSize + 1
  
  redis.call('SET', queueKey, tostring(newQueueSize))
  redis.call('SET', lastLeakKey, tostring(now))
else
  newQueueSize = currentQueueSize
end

local expirySeconds = math.ceil((capacity / leakRatePerSec) + 10)
redis.call('EXPIRE', queueKey, expirySeconds)
redis.call('EXPIRE', lastLeakKey, expirySeconds)

local remainingCapacity = capacity - math.floor(newQueueSize)
return {allowed, remainingCapacity}
