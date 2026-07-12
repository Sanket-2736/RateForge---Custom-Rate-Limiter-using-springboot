-- Atomic Sliding Window Rate Limiter using Redis Sorted Sets
-- 
-- This script implements a sliding window rate limiter using Redis sorted sets.
-- It performs the entire operation atomically on the Redis server:
-- 1. Remove entries outside the time window
-- 2. Count remaining entries
-- 3. Check if we can add another request
-- 4. If allowed, add the current timestamp
-- 5. Return allow/deny decision and remaining count
--
-- Keys:
--   KEYS[1]: Rate limit key (e.g., "user:123:requests")
--
-- Arguments:
--   ARGV[1]: Current timestamp in milliseconds
--   ARGV[2]: Window size in milliseconds
--   ARGV[3]: Maximum allowed requests in the window
--
-- Returns:
--   {allowed, remainingRequests}
--   allowed: 1 if request allowed, 0 if rejected
--   remainingRequests: number of requests left in the window (including this one if allowed)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowSizeMs = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])

-- Calculate the start of the sliding window
local windowStart = now - windowSizeMs

-- Step 1: Remove all entries outside the sliding window
-- ZREMRANGEBYSCORE removes all entries with score < windowStart
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- Step 2: Count remaining entries within the window
local currentCount = redis.call('ZCARD', key)

-- Step 3: Check if we can allow this request
local allowed = 0
local remainingRequests = currentCount

if currentCount < maxRequests then
  allowed = 1
  
  -- Step 4: Add the current timestamp to the sorted set
  -- Use millisecond timestamp as both the score and member (for uniqueness)
  -- Adding a counter to handle multiple requests at the exact same timestamp
  local member = now .. ':' .. currentCount
  redis.call('ZADD', key, now, member)
  
  remainingRequests = currentCount + 1
else
  -- Request rejected, don't add to the set
  remainingRequests = currentCount
end

-- Step 5: Set an expiration on the key to clean up old data
-- Expiry = window size + 1 second buffer to account for clock skew
redis.call('EXPIRE', key, math.ceil(windowSizeMs / 1000) + 1)

-- Return both allowed flag and remaining request count
return {allowed, remainingRequests}
