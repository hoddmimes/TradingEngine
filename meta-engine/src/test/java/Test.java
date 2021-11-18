import com.hoddmimes.jaux.txlogger.*;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.messages.generated.PriceLevel;
import com.hoddmimes.te.trades.TradeX;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Test extends Thread {
	private static SimpleDateFormat cSDF = new SimpleDateFormat("yyMMdd-HHmmssSSS");

	public static void main(String[] args) {
		Test t = new Test();
		t.test();
	}


	private void test() {
		TxLoggerReplayInterface txReplay = TxLoggerFactory.getReplayer( "./logs", "trades");
		TxLoggerReplayIterator tItr = txReplay.replaySync(TxLoggerReplayInterface.DIRECTION.Backward, new Date());



		TxLoggerWriterInterface txl = TxLoggerFactory.getWriter("./logs", "trades");
		for (int i = 0; i < 1000; i++) {
			String tMsg = "Test message " + String.valueOf( i );
			txl.write( tMsg.getBytes(StandardCharsets.UTF_8));
		}

	}
}
