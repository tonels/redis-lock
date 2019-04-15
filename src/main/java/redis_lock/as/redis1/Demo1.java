package redis_lock.as.redis1;

import java.util.Collections;
import java.util.UUID;

import redis.clients.jedis.Jedis;

public class Demo1 {

	public static void main(String[] args) {
			for (int i = 0; i < 5; i++) {
				new MyThread("�߳�" + i).start();
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

		System.out.println(name + " ��ȡ��֮ǰ  a ���� " + jedis.get("a"));

		boolean b = get(jedis, lockKey, requestId, expireTime);
		if (b) {
			System.out.println(name + "��ȡ���ɹ�" + "����ID�ǣ�" + jedis.get("lockKey"));
			if (Long.valueOf(jedis.get("a")) == 20) {
				System.out.println("���sasDsaddsdDsd的���ˣ�������Ʒ�౻������ϣ�");
				// ��ô�˳�
				Thread.currentThread().interrupted();
			} else {
				jedis.decr("a");
				System.out.println(name + " ��ȡ���ɹ����Լ�֮��  a ����" + jedis.get("a"));
			}

			try { // �ͷ���
				releaseDistributedLock(jedis, lockKey, requestId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println(name + "��ȡ��ʧ��" + "����ID�ǣ�" + jedis.get("lockKey"));
		}
//		jedis.close();
	}

//	
	// ���Ի�ȡ��
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
	 * �ͷŷֲ�ʽ��
	 * 
	 * @param jedis     Redis�ͻ���
	 * @param lockKey   ��
	 * @param requestId 
	 * @return �Ƿ��ͷųɹ�
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
