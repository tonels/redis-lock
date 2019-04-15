package redis_lock.as.redis1;

import java.util.Collections;
import java.util.UUID;

import redis.clients.jedis.Jedis;

public class Demo1 {

	public static void main(String[] args) {
			for (int i = 0; i < 5; i++) {
				new MyThread("线程" + i).start();
			}
	}
}

class MyThread extends Thread {

	private String name;
	
	private static final Long RELEASE_SUCCESS = 1L;

	public MyThread(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		Jedis jedis = new Jedis("127.0.0.1", 6379);
		jedis.connect();

		String lockKey = "lockKey";
		String requestId = UUID.randomUUID().toString();
		int expireTime = 5000;

		System.out.println(name + " 获取锁之前  a 等于 " + jedis.get("a"));

		boolean b = get(jedis, lockKey, requestId, expireTime);
		if (b) {
			System.out.println(name + "获取锁成功" + "请求ID是：" + jedis.get("lockKey"));
			if (Long.valueOf(jedis.get("a")) == 20) {
				System.out.println("手速慢了，所有商品亦被抢购完毕，");
				// 怎么退出
				Thread.currentThread().interrupted();
			} else {
				jedis.decr("a");
				System.out.println(name + " 获取锁成功，自减之后  a 等于" + jedis.get("a"));
			}

			try { // 释放锁
				releaseDistributedLock(jedis, lockKey, requestId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println(name + "获取锁失败" + "请求ID是：" + jedis.get("lockKey"));
		}
//		jedis.close();
	}

//	
	// 尝试获取锁
	/**
	 * @param jedis
	 * @param lockKey
	 * @param requestId
	 * @param expireTime
	 * @param NX,SET IF NOT EXIST
	 * @param PX,SET_WITH_EXPIRE_TIME
	 * @return
	 */
	private static boolean get(Jedis jedis, String lockKey, String requestId, int expireTime) {

		String result = jedis.set(lockKey, requestId, "NX", "PX", expireTime);

		if ("OK".equals(result)) {
			return true;
		}
		return false;
	}

	/**
	 * 释放分布式锁
	 * 
	 * @param jedis     Redis客户端
	 * @param lockKey   锁
	 * @param requestId 请求标识
	 * @return 是否释放成功
	 */
	public static boolean releaseDistributedLock(Jedis jedis, String lockKey, String requestId) {

		String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
		Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));

		if (RELEASE_SUCCESS.equals(result)) {
			return true;
		}
		return false;

	}
}
