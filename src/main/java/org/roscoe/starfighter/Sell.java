package org.roscoe.starfighter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.FuncN;
import rx.observables.MathObservable;
import uk.co.leemorris.starfighter.StarfighterConnection;
import uk.co.leemorris.starfighter.dto.HeartbeatResult;
import uk.co.leemorris.starfighter.dto.NewOrderDetails;
import uk.co.leemorris.starfighter.dto.NewOrderResponse;
import uk.co.leemorris.starfighter.dto.VenueResult;
import uk.co.leemorris.starfighter.model.Direction;
import uk.co.leemorris.starfighter.model.OrderType;
import uk.co.leemorris.starfighter.model.StockQuote;

public class Sell {

	public static void main(String[] args) throws InterruptedException {
    	String stock = "YGEM";
    	String venue = "SOGBEX";
    	String account = "EJH28887520";
		String apiToken = "697a144bb2343794e7e613ded84f974955ec58a8";

		StarfighterConnection connection = new StarfighterConnection(apiToken);

		HeartbeatResult heartbeatResult = connection.heartbeat().timeout(3, TimeUnit.SECONDS)
				.toBlocking().single();
		if (!heartbeatResult.isOk()) {
			System.err.println("heartbeat failed");
			System.exit(-1);
		}

		VenueResult venueResult = connection.venueHeartbeat(venue).timeout(3, TimeUnit.SECONDS)
				.toBlocking().single();
		if (!venueResult.isOk()) {
			System.err.println("venue heartbeat failed");
			System.exit(-1);
		}

		Observable<StockQuote> quotes = connection.subscribeToQuotes(venue, account, stock).cache();
		Observable<Integer> avg10 = quotes.window(10).flatMap(
				stockQuoteObservable -> MathObservable.averageInteger(stockQuoteObservable
						.map(sq -> sq.getLast())));
//		Observable<Integer> avg100 = quotes.window(100).flatMap(
//				stockQuoteObservable -> MathObservable.averageInteger(stockQuoteObservable
//						.map(sq -> sq.getLast())).repeat(10));
//		
//		avg10.subscribe(avg -> System.out.println("average of 10 last: " + avg));
//		avg100.subscribe(avg -> System.out.println("average of 100 last: " + avg));
		
//		ConcurrentLinkedQueue
		List<StockQuote> list = Collections.synchronizedList(new LinkedList<StockQuote>());
		
		connection.subscribeToQuotes(venue, account, stock).subscribe(sq->list.add(sq));
		
		connection.subscribeToFills(venue, account, stock).subscribe(fill-> {
			System.out.println(fill);
		});
		
    	for (;;) {
    		Thread.sleep(500);
    		if (list.size() < 100) {
    			continue;
    		}
    		
    		Observable.from(list).takeLast(100)
    		.collect(()->new ArrayList<StockQuote>(), (x,y) -> x.add(y))
    				.subscribe(sq -> {
    					
    					
    					Observable<Integer> map = Observable.from(sq).map(s->s.getLast());
						
    					Observable.zip(MathObservable.averageInteger(map), MathObservable.max(map), MathObservable.min(map), new Func3<Integer, Integer, Integer, int[]>() {

							@Override
							public int[] call(Integer t1, Integer t2, Integer t3) {
								return new int[]{t1, t2, t3};
							}
						}).subscribe(i->{
							
							int max = i[1];
							int min = i[2];
							int distance = Math.abs(max - min);
							
							int buy = (int) (min + 0.2 * distance);
							Observable<NewOrderResponse> newOrder1 = connection.newOrder(new NewOrderDetails(account, venue, stock, buy, 10, Direction.BUY, OrderType.FOK));
							newOrder1.subscribe(no-> {
								no.getFills().forEach(fill->fill.getPrice());
							});
							int sell = (int) (max - 0.2 * distance);
							Observable<NewOrderResponse> newOrder2 = connection.newOrder(new NewOrderDetails(account, venue, stock, sell, 10, Direction.SELL, OrderType.FOK));
							System.out.println("buy = " + buy + " sell = " + sell);
						});
    					
    				}, e->{
    					System.err.println(e);
    				});

//			MathObservable.averageInteger(latest10).subscribe(avg -> System.out.println("avg " + avg));
//			MathObservable.max(latest10).subscribe(max -> System.out.println("max " + max));
//			MathObservable.min(latest10).subscribe(min -> System.out.println("min " + min));
			
//			Observable<NewOrderResponse> newOrder = connection.newOrder(new NewOrderDetails(account, venue, stock, (int) (bid + 0.1 * distance), 50, Direction.BUY, OrderType.LIMIT));
    	}

	}

}
