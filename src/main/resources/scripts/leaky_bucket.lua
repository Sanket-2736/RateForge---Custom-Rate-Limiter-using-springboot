-- Atomic Leaky Bucket Rate Limiter using Redis
-- 
-- This script implements a leaky bucket rate limiter. Requests are added to a queue
-- (represented as a counter) up to the bucket capacity. The bucket "leaks" at a
-- constant rate, freeing up capacity over time.
--
-- Key difference from token bucket:
-- - Token bucket: Accumulates tokens up to capacity, bursts allowed up to full capacity
-- - Leaky bucket: Queue fills up, drains at constant rate, smooths bursts into steady output
--
-- This ensures:
-- 1. Maximum burst size = capacity
-- 2. Average rate = leakRatePerSec (steady, predictable output)
-- 3. No bursty traffic patterns downstream
--
-- Keys:
--   KEYS[1]: Current queue size key (e.g., "user:123:queue")
--   KEYS[2]: Last leak timestamp key (e.g., "user:123:lastLeak")
--
-- Arguments:
--   ARGV[1]: Current timestamp in milliseconds
--   ARGV[2]: Bucket capacity (max queue size)
--   ARGV[3]: Leak rate per second
--
-- Returns:
--   {allowed, queueSize}
--   allowed: 1 if request accepted into queue, 0 if queue full
--   queueSize: number of requests currently in the queue

local queueKey = KEYS[1]
local lastLeakKey = KEYS[2]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local leakRatePerSec = tonumber(ARGV[3])

-- Get current queue state
local queueSizeStr = redis.call('GET', queueKey)
local lastLeakStr = redis.call('GET', lastLeakKey)

local currentQueueSize
local lastLeakTime

if queueSizeStr == false then
  -- First request: initialize queue
  currentQueueSize = 0
  lastLeakTime = now
else
  currentQueueSize = tonumber(queueSizeStr)
  lastLeakTime = tonumber(lastLeakStr)
  
  -- Calculate how much has leaked since last operation
  local elapsedMs = now - lastLeakTime
  local elapsedSec = elapsedMs / 1000.0
  
  -- Calculate requests that have leaked out
  local leaked = elapsedSec * leakRatePerSec
  
  -- Remove leaked requests from queue (cannot go below 0)
  currentQueueSize = math.max(0, currentQueueSize - leaked)
end

-- Check if we can accept this request
local allowed = 0
local newQueueSize = currentQueueSize

if currentQueueSize < capacity then
  -- Queue has space, accept the request
  allowed = 1
  newQueueSize = currentQueueSize + 1
  
  -- Update queue state atomically
  redis.call('SET', queueKey, tostring(newQueueSize))
  redis.call('SET', lastLeakKey, tostring(now))
else
  -- Queue is full, reject the request
  newQueueSize = currentQueueSize
end

-- Set expiration on keys to clean up old data
-- Expiry = time for capacity to leak at the leak rate + buffer
local expirySeconds = math.ceil((capacity / leakRatePerSec) + 10)
redis.call('EXPIRE', queueKey, expirySeconds)
redis.call('EXPIRE', lastLeakKey, expirySeconds)

-- Return both allowed flag and current queue size
return {allowed, math.floor(newQueueSize)}
