-- Atomic Token Bucket Rate Limiter using Lua Script
-- 
-- This script performs the entire rate limiting check and token consumption
-- as a single atomic operation on the Redis server, eliminating race conditions.
--
-- Keys:
--   KEYS[1]: Token count key (e.g., "user:123:tokens")
--   KEYS[2]: Last refill timestamp key (e.g., "user:123:lastRefill")
--
-- Arguments:
--   ARGV[1]: Current time in milliseconds
--   ARGV[2]: Bucket capacity
--   ARGV[3]: Refill rate per second (as string for precision)
--
-- Returns:
--   {allowed, remainingTokens}
--   allowed: 1 if request allowed, 0 if rejected
--   remainingTokens: number of tokens remaining after operation

local tokenKey = KEYS[1]
local lastRefillKey = KEYS[2]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRatePerSec = tonumber(ARGV[3])

-- Get current state from Redis
local tokenCountStr = redis.call('GET', tokenKey)
local lastRefillStr = redis.call('GET', lastRefillKey)

local currentTokens
local lastRefillTime

if tokenCountStr == false then
  -- First request: initialize bucket to full capacity
  currentTokens = capacity
  lastRefillTime = now
else
  currentTokens = tonumber(tokenCountStr)
  lastRefillTime = tonumber(lastRefillStr)
  
  -- Calculate tokens to add based on elapsed time
  local elapsedMs = now - lastRefillTime
  local elapsedSec = elapsedMs / 1000.0
  local tokensToAdd = elapsedSec * refillRatePerSec
  
  -- Don't exceed capacity
  currentTokens = math.min(currentTokens + tokensToAdd, capacity)
end

-- Check if request can be allowed
local allowed = 0
local remainingTokens = math.floor(currentTokens)

if currentTokens >= 1.0 then
  allowed = 1
  currentTokens = currentTokens - 1.0
  remainingTokens = math.floor(currentTokens)
  
  -- Atomically update Redis state
  redis.call('SET', tokenKey, tostring(currentTokens))
  redis.call('SET', lastRefillKey, tostring(now))
else
  -- Request rejected, don't update state
  remainingTokens = math.floor(currentTokens)
end

-- Return both allowed flag and remaining tokens
return {allowed, remainingTokens}
