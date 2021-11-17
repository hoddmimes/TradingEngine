import com.hoddmimes.jaux.txlogger.*;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.messages.generated.PriceLevel;
import com.hoddmimes.te.trades.TradeX;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Test extends Thread
{
	double mTickSize = 0.01d;

	public static void main(String[] args) {
		Test t = new Test();
		t.test();
	}


	private boolean isTickSizeAligned( double pPrice ) {
		if (mTickSize == 0) {
			return true;
		}
		BigDecimal tPrice = new BigDecimal( Double.toString( pPrice));
		double  r = tPrice.subtract(tPrice.divideToIntegralValue(new BigDecimal("1.0"))).doubleValue();

		int x = (int) (r * 1000.0d);
		int y = (int) (mTickSize * 1000.0d);

		return ((x % y) == 0);
	}




	private void test() {
		System.out.println("valid : " + isTickSizeAligned( 99.8));
	}

}
