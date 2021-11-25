import com.hoddmimes.jaux.txlogger.*;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.messages.generated.PriceLevel;


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
		Integer x;

	public static void main(String[] args) {
		Test t = new Test();
		t.test();
	}


	private void test() {
		Integer v1 = null,v2 = 15;

		if (v1 == x) {
			System.out.println("Same 1");
		}


	}
}
