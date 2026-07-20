local tokenKey = KEYS[1]
local lastRefillKey = KEYS[2]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRatePerSec = tonumber(ARGV[3])

local tokenCountStr = redis.call('GET', tokenKey)
local lastRefillStr = redis.call('GET', lastRefillKey)

local currentTokens
local lastRefillTime

if tokenCountStr == false then
  currentTokens = capacity
  lastRefillTime = now
else
  currentTokens = tonumber(tokenCountStr)
  lastRefillTime = tonumber(lastRefillStr)
  
  local elapsedMs = now - lastRefillTime
  local elapsedSec = elapsedMs / 1000.0
  local tokensToAdd = elapsedSec * refillRatePerSec
  
  currentTokens = math.min(currentTokens + tokensToAdd, capacity)
end

local allowed = 0
local remainingTokens = math.floor(currentTokens)

if currentTokens >= 1.0 then
  allowed = 1
  currentTokens = currentTokens - 1.0
  remainingTokens = math.floor(currentTokens)
  
  redis.call('SET', tokenKey, tostring(currentTokens))
  redis.call('SET', lastRefillKey, tostring(now))
else
  remainingTokens = math.floor(currentTokens)
end

return {allowed, remainingTokens}
