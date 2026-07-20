local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowSizeMs = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])

local windowStart = now - windowSizeMs

redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

local currentCount = redis.call('ZCARD', key)

local allowed = 0
local remainingCapacity

if currentCount < maxRequests then
  allowed = 1
  local member = now .. ':' .. currentCount
  redis.call('ZADD', key, now, member)
  
  remainingCapacity = maxRequests - (currentCount + 1)
else
  remainingCapacity = 0
end

redis.call('EXPIRE', key, math.ceil(windowSizeMs / 1000) + 1)

return {allowed, remainingCapacity}
