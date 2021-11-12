import com.hoddmimes.te.messages.generated.PriceLevel;

import java.util.*;
import java.util.stream.Collectors;

public class Test
{
	public static void main(String[] args) {
		Test t = new Test();
		t.test();
	}

	private void test() {
		TreeMap<Double,List<Item>> tPriceMap = new TreeMap<>();
		for (int i = 9; i >= 1 ; i--) {
			double tPrice = (i * 10) + 1;
			ArrayList<Item> tList = new ArrayList<>();
			for (int j = 0; j < 20; j++) {
				tList.add(new Item(tPrice, i));
			}
			tPriceMap.put( tPrice, tList);
		}

		for (int i = 0; i < 10; i++) {
			//findLevel( mPriceMap.values(), 1000000 );
			testPriceLevel( tPriceMap, 1000000  );
		}
	}

	private void testPriceLevel( TreeMap<Double,List<Item>> mPriceMap, int pLoop ) {
		long pStartTime = System.nanoTime();
		for (int i = 0; i < pLoop; i++) {
			getPriceLevers(mPriceMap, 5);
		}
		long pExecTime = System.nanoTime() - pStartTime;

		long t = pExecTime / pLoop;
		System.out.println("nano / get-levels : " + t);
	}

	List<PriceLevel> getPriceLevers( TreeMap<Double,List<Item>> pPriceMap, int pLevels) {
		int i = 0;
		ArrayList<PriceLevel> tResultList = new ArrayList<>();
		Iterator<Map.Entry<Double,List<Item>>> tItrPrices = pPriceMap.entrySet().stream().iterator();
		while( (tItrPrices.hasNext()) && (i < pLevels)) {
			Map.Entry<Double,List<Item>> tPriceEntry = tItrPrices.next();
			List<Item> tList = tPriceEntry.getValue();
			int v = 0;
			for( Item itm : tList) {
				v += itm.mVolume;
			}
			PriceLevel pl = new PriceLevel();
			pl.setPrice( tPriceEntry.getKey());
			pl.setVolume(v);
			tResultList.add( pl );
		}
		return tResultList;
	}


	private void findLevel(Collection<List<Item>> pCollection, int pLoop) {
		List<List<Item>> tList = pCollection.stream().collect( Collectors.toList());

		long pStartTime = System.nanoTime();
		for (int i = 0; i < pLoop; i++) {
			double tPrice = tList.get((i%5)).get(0).mPrice;
			findIndex(tList, 5, tPrice);
		}
		long pExecTime = System.nanoTime() - pStartTime;

		long t =  pExecTime / pLoop;
		System.out.println("nano / search : " + t);


	}


	int findIndex( List<List<Item>> pList, int pMax, double pPrice ) {
		for (int j = 0; j < pMax; j++) {
			if (pList.get(j).get(0).mPrice == pPrice) {
				return j;
			}
		}
		return pMax + 1;
	}


	static class Item {
		double mPrice;
		int mVolume;

		Item( double pPrice, int pVolume ) {
			mPrice = pPrice;
			mVolume = pVolume;
		}
	}
}
