package org.gvamosi.wrapping.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.gvamosi.wrapping.model.Wrapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("wrappingService")
public class WrappingService {

	@Autowired
	@Qualifier("cachedThreadPool")
	private ExecutorService executorService;

	private Map<String, Wrapping> results = new ConcurrentHashMap<String, Wrapping>();
	
	/*
	 * For JUnit Test reasons.
	 */
	public WrappingService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public Wrapping getWrapping(long workId, String sessionId) {
		return results.get(sessionId + workId);
	}
	
	public Wrapping wrapText(Wrapping wrapping, String sessionId) {
		return executeWorkerThread(wrapping, sessionId);
	}

	private Wrapping executeWorkerThread(Wrapping wrapping, String sessionId) {
		if (wrapping.getWorkId() == -1) {
			WorkerThread worker = new WorkerThread(wrapping, sessionId);
			
			Future<Void> task = (Future<Void>) executorService.submit(worker);
			
			if (task == null) {
				// all right :)
			}
			
			// wait for getting a real work ID
			while (worker.getWrapping().getWorkId() == -1) {}
			
			// sleep 1 sec, otherwise unit tests not working!
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return worker.getWrapping();
			// return wrapping;
		} else {
			return results.get(sessionId + wrapping.getWorkId());
		}
	}

	public class WorkerThread implements Runnable {

		private Wrapping wrapping;
		private String sessionId;

		public WorkerThread(Wrapping wrapping, String sessionId) {
			this.wrapping = wrapping;
			this.sessionId = sessionId;
		}

		public Wrapping getWrapping() {
			return wrapping;
		}

		@Override
		public void run() {
			long threadId = Thread.currentThread().getId();
			if (getWrapping().getWorkId() == -1) {
				
				// set workId
				getWrapping().setWorkId(threadId);
				getWrapping().setProcessed(false);
				results.put(sessionId + getWrapping().getWorkId(), getWrapping());
				
				// wrapping
				wrapTextGivenLength(getWrapping());
				results.put(sessionId + getWrapping().getWorkId(), getWrapping());
				
				// processed true
				getWrapping().setProcessed(true);
				results.put(sessionId + getWrapping().getWorkId(), getWrapping());
			}
		}
	}

	private void wrapTextGivenLength(Wrapping wrapping) {
		String splitted[] = wrapping.getTextToWrap().split("\\s+");
		for (int i = 0; i < splitted.length; i++) {
			StringBuilder sb = new StringBuilder();
			int j = 0;
			do {
				sb.append(splitted[i + j]);
				j++;
				if (i + j < splitted.length && sb.length() < wrapping.getWrapLength()) {
					sb.append(" ");
				}
			} while (i + j < splitted.length && sb.length() + splitted[i + j].length() <= wrapping.getWrapLength());
			i += j - 1;
			wrapping.getWrappedText().add(sb.toString());
		}
	}
}
