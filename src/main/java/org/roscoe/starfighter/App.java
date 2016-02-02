package org.roscoe.starfighter;

import rx.Observable;
import rx.Subscriber;
import uk.co.leemorris.starfighter.StarfighterConnection;
import uk.co.leemorris.starfighter.dto.NewOrderDetails;
import uk.co.leemorris.starfighter.dto.NewOrderResponse;
import uk.co.leemorris.starfighter.model.Direction;
import uk.co.leemorris.starfighter.model.OrderType;
import uk.co.leemorris.starfighter.model.StockQuote;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InterruptedException
    {
    	String stock = "KBC";
    	String venue = "EKMBEX";
    	String account = "TS55637980";
    	String apiToken = "b5483d287c636c7ebd802f465ebc478d6f38fd61";
    	
		StarfighterConnection connection = new StarfighterConnection(apiToken);
    	
    	int targetSellPrice = -1;
    	int targetBuyPrice = -1;
    	
    	for (;;) {
    		StockQuote quote = connection.getQuoteForStock(stock, venue).toBlocking().single();
    		System.out.println(quote.shortToString());
    		Thread.sleep(500);
    		
    		int bid = quote.getBid(); // 8130
    		if (bid <= 0) continue;
    		int ask = quote.getAsk(); // 8140
    		if (ask <= 0) continue;
    		
    		int distance = Math.abs(bid - ask); // 10
    		
//    		if (distance * 97 < ask) {
//    			continue;
//    		}
    		
    		if(true) {
    			Observable<NewOrderResponse> newOrder = connection.newOrder(new NewOrderDetails(account, venue, stock, (int) (bid + 0.1 * distance), 50, Direction.BUY, OrderType.LIMIT));
    			NewOrderResponse newOrderResponse = newOrder.toBlocking().single();
    			System.out.println("b " + newOrderResponse);
    			
    			
    			Observable<NewOrderResponse> newOrder2 = connection.newOrder(new NewOrderDetails(account, venue, stock, (int) (ask - 0.1 * distance), 50, Direction.SELL, OrderType.LIMIT));
    			NewOrderResponse newOrderResponse2 = newOrder2.toBlocking().single();
    			System.out.println("s " + newOrderResponse2);
    			
    		}
		}
    	
		
//		connection.subscribeToQuotes(venue, account, stock)
//        .subscribe(new Subscriber<StockQuote>() {
//            @Override
//            public void onCompleted() {
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//
//            }
//
//            @Override
//            public void onNext(StockQuote stockQuote) {
//              // new quotes will appear here
//            	System.out.println(stockQuote.shortToString());
//            }
//        });
    }
}
