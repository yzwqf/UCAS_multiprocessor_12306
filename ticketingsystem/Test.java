package ticketingsystem;

import java.util.ArrayList;
import java.util.Random;

public class Test {
    final static int threadnum = 96; // concurrent thread number
    final static int routenum = 20; // route is designed from 1 to 3
    final static int coachnum = 15; // coach is arranged from 1 to 5
    final static int seatnum = 100; // seat is allocated from 1 to 20
    final static int stationnum = 10; // station is designed from 1 to 5

    final static int testnum = 500000;
    final static int retpc = 5; // return ticket operation is 5% percent
    final static int buypc = 20; // buy ticket operation is 15% percent
    final static int inqpc = 100; //inquiry ticket operation is 80% percent

    static String passengerName() {
        Random rand = new Random();
        long uid = rand.nextInt(testnum);
        return "passenger_" + WqfThreadID.get() + "_" + uid;
    }

    public static void main(String[] args) throws InterruptedException {

        Thread[] threads = new Thread[threadnum];
        final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);

	/*
        long[] buyTime = new long[threadnum];
        long[] refundTime = new long[threadnum];
        long[] inqueryTime = new long[threadnum];
        long[] buyTimes = new long[threadnum];
        long[] refundTimes = new long[threadnum];
        long[] inqueryTimes = new long[threadnum];
        for (int i = 0; i < threadnum; i++) {
            buyTime[i] = refundTime[i] = inqueryTime[i] = 0;
            buyTimes[i] = refundTimes[i] = inqueryTimes[i] = 0;
        }
	*/

        //long startTime =  System.currentTimeMillis();
        long startTime =  System.nanoTime();


        for (int i = 0; i< threadnum; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    long startTime, endTime;
                    Random rand = new Random();
                    Ticket ticket = new Ticket();
                    ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();

                    for (int i = 0; i < testnum; i++) {
                        int sel = rand.nextInt(inqpc);
                        if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket

                            //refundTimes[WqfThreadID.get()]++;
                            //startTime = System.nanoTime();

                            int select = rand.nextInt(soldTicket.size());
                            if ((ticket = soldTicket.remove(select)) != null) {
                                if (tds.refundTicket(ticket)) {
                                    //System.out.println("TicketRefund" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach  + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
                                    //System.out.flush();
                                } else {
                                    System.out.println("ErrOfRefund");
                                    //System.out.flush();
                                }
                            } else {
                                System.out.println("ErrOfRefund");
                                //System.out.flush();
                            }


                            //endTime = System.nanoTime();
                            //refundTime[WqfThreadID.get()] += (endTime-startTime);

                        } else if (retpc <= sel && sel < buypc) { // buy ticket


                            //buyTimes[WqfThreadID.get()]++;
                            //startTime = System.nanoTime();

                            String passenger = passengerName();
                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
                                soldTicket.add(ticket);
                                //System.out.println("TicketBought" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
                                //System.out.flush();
                            } else {
                                //System.out.println("TicketSoldOut" + " " + route+ " " + departure+ " " + arrival);
                                //System.out.flush();
                            }

                            //endTime = System.nanoTime();
                            //buyTime[WqfThreadID.get()] += (endTime-startTime);


                        } else if (buypc <= sel && sel < inqpc) { // inquiry ticket

                            //inqueryTimes[WqfThreadID.get()]++;
                            //startTime = System.nanoTime();

                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            int leftTicket = tds.inquiry(route, departure, arrival);

                            //System.out.println("RemainTicket" + " " + leftTicket + " " + route+ " " + departure+ " " + arrival);
                            //System.out.flush();

                            //endTime = System.nanoTime();
                            //inqueryTime[WqfThreadID.get()] += (endTime-startTime);
                        }
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i< threadnum; i++) {
            threads[i].join();
        }

        long endTime =  System.nanoTime();
        System.out.println("Used Time(s): "  + (double)(endTime-startTime)/1000000000L);

/*
        long totalBuyTimes = 0;
        long totalRefundTimes = 0;
        long totalInqueryTimes = 0;
        long totalBuyTime = 0;
        long totalRefundTime = 0;
        long totalInqueryTime = 0;
        for (int i = 0; i < threadnum; i++) {
            totalBuyTimes += buyTimes[i];
            totalRefundTimes += refundTimes[i];
            totalInqueryTimes += inqueryTimes[i];

            totalBuyTime += buyTime[i];
            totalRefundTime += refundTime[i];
            totalInqueryTime += inqueryTime[i];
        }

        System.out.println("Name    Total Time(ms)     Total Times    ns/per");
        System.out.println("Buy:    " + (double)totalBuyTime + "    " + totalBuyTimes + "    " + (double)totalBuyTime/totalBuyTimes);
        System.out.println("Refund:    " + (double)totalRefundTime + "    " + totalRefundTime + "    " + (double)totalRefundTime/totalRefundTimes);
        System.out.println("Inquery:    " + (double)totalInqueryTime + "    " + totalInqueryTimes + "    " + (double)totalInqueryTime/totalInqueryTimes);
*/

    }
}
