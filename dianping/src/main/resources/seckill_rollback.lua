-- 参数：ARGV[1]=voucherId, ARGV[2]=userId
local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1. 判断用户是否在订单集合中（防止重复回滚）
if redis.call('sismember', orderKey, userId) == 0 then
    return 0
end

-- 2. 恢复库存
redis.call('incrby', stockKey, 1)
-- 3. 移除用户记录
redis.call('srem', orderKey, userId)

return 1
